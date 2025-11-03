package com.nico;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity 
    implements NetworkManager.NetworkListener {
    
    private EditText messageInput;
    private Button sendButton;
    private TextView chatTitle;
    private LinearLayout messagesLayout;
    private DatabaseHelper dbHelper;
    private NetworkManager networkManager;
    private String currentChatName;
    private String connectedIp;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        // Получаем имя чата из Intent
        currentChatName = getIntent().getStringExtra("CHAT_NAME");
        if (currentChatName == null) {
            currentChatName = "General Chat";
        }
        
        // Получаем сохранённый IP адрес
        SharedPreferences prefs = getSharedPreferences("nico_prefs", MODE_PRIVATE);
        connectedIp = prefs.getString("connected_ip", "");
        
        // Инициализируем базу данных
        dbHelper = new DatabaseHelper(this);
        
        // Инициализируем сетевой менеджер
        networkManager = new NetworkManager(this);
        networkManager.setListener(this);
        networkManager.startServer();
        
        setupiOSStyle();
        setupViews();
        loadMessagesFromDatabase();
        setupClickListeners();
        
        System.out.println("💬 Nico Chat Activity started for: " + currentChatName);
        System.out.println("📡 Nico: Connected to IP: " + (connectedIp.isEmpty() ? "None" : connectedIp));
        
        // Показываем статус подключения
        updateConnectionStatus();
    }
    
    private void setupiOSStyle() {
        getWindow().setStatusBarColor(0xFFF2F2F7);
        getWindow().setNavigationBarColor(0xFFF2F2F7);
    }
    
    private void setupViews() {
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        chatTitle = findViewById(R.id.chatTitle);
        messagesLayout = findViewById(R.id.messagesLayout);
        
        // Устанавливаем заголовок чата
        if (chatTitle != null) {
            chatTitle.setText(currentChatName);
            
            // Добавляем индикатор подключения
            updateConnectionStatus();
        }
    }
    
    private void updateConnectionStatus() {
        if (chatTitle != null) {
            String status = connectedIp.isEmpty() ? " (Offline)" : " (Connected)";
            chatTitle.setText(currentChatName + status);
        }
    }
    
    private void loadMessagesFromDatabase() {
        // Загружаем сообщения из базы данных
        List<DatabaseHelper.Message> messages = dbHelper.getMessagesForChat(currentChatName);
        
        // Очищаем layout сообщений
        if (messagesLayout != null) {
            messagesLayout.removeAllViews();
        }
        
        // Отображаем сообщения
        for (DatabaseHelper.Message message : messages) {
            addMessageToLayout(message);
        }
        
        System.out.println("📨 Nico: Displayed " + messages.size() + " messages");
        
        // Прокручиваем к последнему сообщению
        scrollToBottom();
    }
    
    private void setupClickListeners() {
        // Кнопка отправки
        sendButton.setOnClickListener(v -> sendMessage());
        
        // Кнопка назад
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        
        // Отправка по Enter
        messageInput.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == 66) { // Enter key
                sendMessage();
                return true;
            }
            return false;
        });
    }
    
    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (!text.isEmpty()) {
            if (connectedIp.isEmpty()) {
                // Режим офлайн - сохраняем только локально
                saveMessageLocally(text);
                Toast.makeText(this, "Message saved (offline mode)", Toast.LENGTH_SHORT).show();
            } else {
                // Режим онлайн - отправляем по сети
                sendMessageOverNetwork(text);
            }
            
            // Очищаем поле ввода
            messageInput.setText("");
            
            // Обновляем сообщения на экране
            loadMessagesFromDatabase();
        }
    }
    
    private void saveMessageLocally(String text) {
        String timestamp = getCurrentTime();
        dbHelper.addMessage(currentChatName, "You", text, timestamp, true);
        System.out.println("💾 Nico: Message saved locally - " + text);
    }
    
    private void sendMessageOverNetwork(String text) {
        String timestamp = getCurrentTime();
        
        // Сохраняем локально
        dbHelper.addMessage(currentChatName, "You", text, timestamp, true);
        
        // Отправляем по сети
        networkManager.sendMessage(connectedIp, currentChatName, "You", text);
        
        Toast.makeText(this, "Message sent to " + connectedIp, Toast.LENGTH_SHORT).show();
        System.out.println("✈️ Nico: Message sent via network - " + text);
    }
    
    private void addMessageToLayout(DatabaseHelper.Message message) {
        if (messagesLayout == null) return;
        
        // Создаем контейнер для сообщения
        LinearLayout messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setPadding(20, 10, 20, 10);
        
        // Заголовок сообщения (отправитель и время)
        TextView headerView = new TextView(this);
        String headerText = message.sender + " • " + message.time;
        headerView.setText(headerText);
        headerView.setTextSize(12);
        headerView.setTextColor(0xFF8E8E93);
        headerView.setPadding(0, 0, 0, 5);
        
        // Текст сообщения
        TextView messageView = new TextView(this);
        messageView.setText(message.text);
        messageView.setTextSize(16);
        messageView.setPadding(20, 15, 20, 15);
        messageView.setMaxWidth(800); // Ограничиваем ширину для красоты
        
        // Стили для сообщений
        if (message.isOutgoing) {
            // Исходящие сообщения - iOS Blue
            messageView.setBackgroundColor(0xFF007AFF);
            messageView.setTextColor(0xFFFFFFFF);
            messageContainer.setGravity(android.view.Gravity.END);
            headerView.setGravity(android.view.Gravity.END);
        } else {
            // Входящие сообщения - iOS Light Gray
            messageView.setBackgroundColor(0xFFF2F2F7);
            messageView.setTextColor(0xFF000000);
            messageContainer.setGravity(android.view.Gravity.START);
            headerView.setGravity(android.view.Gravity.START);
        }
        
        // Закругляем углы (через background drawable)
        messageView.setBackgroundResource(R.drawable.message_bubble);
        
        // Добавляем в контейнер
        messageContainer.addView(headerView);
        messageContainer.addView(messageView);
        
        // Добавляем в основной layout
        messagesLayout.addView(messageContainer);
    }
    
    private void scrollToBottom() {
        if (messagesLayout != null) {
            messagesLayout.post(() -> {
                messagesLayout.fullScroll(android.view.View.FOCUS_DOWN);
            });
        }
    }
    
    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }
    
    // ===== NetworkListener Implementation =====
    
    @Override
    public void onMessageReceived(String chatName, String sender, String message) {
        runOnUiThread(() -> {
            System.out.println("📨 Nico: Received message in chat - " + sender + ": " + message);
            
            // Показываем уведомление
            Toast.makeText(this, "New message from " + sender, Toast.LENGTH_SHORT).show();
            
            // Обновляем сообщения
            loadMessagesFromDatabase();
            
            // Прокручиваем к новому сообщению
            scrollToBottom();
        });
    }
    
    @Override
    public void onDeviceDiscovered(String ip, String deviceName) {
        // Не используется в чате
    }
    
    @Override
    public void onConnectionStatusChanged(boolean connected) {
        runOnUiThread(() -> {
            if (!connected) {
                connectedIp = "";
                updateConnectionStatus();
                Toast.makeText(this, "Connection lost", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkManager != null) {
            networkManager.stopServer();
        }
    }
}
