package com.nico;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    private LinearLayout mainLayout;
    private DatabaseHelper dbHelper;
    private NetworkManager networkManager;
    private TextView connectionStatus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Находим основной layout
        mainLayout = findViewById(R.id.main_layout);
        
        // Инициализируем базу данных
        dbHelper = new DatabaseHelper(this);
        
        // Инициализируем сетевой менеджер
        networkManager = new NetworkManager(this);
        networkManager.startServer();
        
        setupiOSStyle();
        setupConnectionStatus();
        loadChatsFromDatabase();
        setupClickListeners();
        
        System.out.println("🚀 Nico Messenger started!");
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем статус подключения при возвращении на экран
        updateConnectionStatus();
    }
    
    private void setupiOSStyle() {
        // iOS цвета для статус бара и навигации
        getWindow().setStatusBarColor(0xFFF2F2F7);
        getWindow().setNavigationBarColor(0xFFF2F2F7);
        
        System.out.println("🎨 Nico iOS style applied!");
    }
    
    private void setupConnectionStatus() {
        // Создаем TextView для статуса подключения
        connectionStatus = new TextView(this);
        connectionStatus.setTextSize(12);
        connectionStatus.setPadding(20, 10, 20, 10);
        connectionStatus.setGravity(View.TEXT_ALIGNMENT_CENTER);
        
        // Добавляем в layout (после заголовка)
        mainLayout.addView(connectionStatus, 1); // Добавляем после первого элемента
    }
    
    private void updateConnectionStatus() {
        SharedPreferences prefs = getSharedPreferences("nico_prefs", MODE_PRIVATE);
        String connectedIp = prefs.getString("connected_ip", "");
        
        if (connectedIp.isEmpty()) {
            connectionStatus.setText("🔴 Offline - Tap 'Connect' to start chatting");
            connectionStatus.setTextColor(0xFFFF3B30); // iOS Red
        } else {
            connectionStatus.setText("🟢 Connected to " + connectedIp);
            connectionStatus.setTextColor(0xFF34C759); // iOS Green
        }
    }
    
    private void loadChatsFromDatabase() {
        // Загружаем чаты из базы данных
        List<DatabaseHelper.Chat> chats = dbHelper.getRecentChats();
        
        // Показываем чаты в виде кнопок
        showChatsAsButtons(chats);
    }
    
    private void showChatsAsButtons(List<DatabaseHelper.Chat> chats) {
        // Очищаем существующие чаты (кроме первых трёх элементов - хедер, статус и текст)
        for (int i = 3; i < mainLayout.getChildCount(); i++) {
            if (mainLayout.getChildAt(i) instanceof Button) {
                mainLayout.removeViewAt(i);
                i--; // Уменьшаем счетчик т.к. удалили элемент
            }
        }
    
        // Добавляем кнопку подключения
        addConnectButton();
        
        // Добавляем разделитель
        addSeparator("Your Chats:");
        
        // Добавляем чаты из базы данных
        for (DatabaseHelper.Chat chat : chats) {
            Button chatButton = new Button(this);
            chatButton.setText(chat.name + "\n" + chat.lastMessage + "\n" + chat.time);
            chatButton.setBackgroundColor(0xFFFFFFFF);
            chatButton.setTextColor(0xFF000000);
            chatButton.setOnClickListener(v -> {
                openChatActivity(chat.name);
            });
            
            chatButton.setPadding(50, 30, 50, 30);
            chatButton.setTextSize(14);
            
            mainLayout.addView(chatButton);
        }
        
        // Если чатов нет, показываем сообщение
        if (chats.isEmpty()) {
            addNoChatsMessage();
        }
        
        System.out.println("💬 Nico: Displayed " + chats.size() + " chats from database");
    }
    
    // Добавляем кнопку подключения к сети
    private void addConnectButton() {
        Button connectButton = new Button(this);
        connectButton.setText("🔗 Connect to Device\nSetup network connection");
        connectButton.setBackgroundColor(0xFF007AFF); // iOS Blue
        connectButton.setTextColor(0xFFFFFFFF);
        connectButton.setOnClickListener(v -> {
            openConnectActivity();
        });
        
        connectButton.setPadding(50, 30, 50, 30);
        connectButton.setTextSize(14);
        
        mainLayout.addView(connectButton);
    }
    
    // Добавляем разделитель с текстом
    private void addSeparator(String text) {
        TextView separator = new TextView(this);
        separator.setText(text);
        separator.setTextColor(0xFF8E8E93);
        separator.setTextSize(14);
        separator.setPadding(20, 20, 20, 10);
        
        mainLayout.addView(separator);
    }
    
    // Сообщение когда чатов нет
    private void addNoChatsMessage() {
        TextView noChatsText = new TextView(this);
        noChatsText.setText("No chats yet\nConnect to a device and start messaging!");
        noChatsText.setTextColor(0xFF8E8E93);
        noChatsText.setTextSize(14);
        noChatsText.setGravity(View.TEXT_ALIGNMENT_CENTER);
        noChatsText.setPadding(50, 40, 50, 40);
        
        mainLayout.addView(noChatsText);
    }
    
    private void setupClickListeners() {
        // Кнопка редактирования (если есть в layout)
        Button editButton = findViewById(R.id.editButton);
        if (editButton != null) {
            editButton.setOnClickListener(v -> {
                System.out.println("✏️ Nico: Refresh chats");
                loadChatsFromDatabase(); // Обновляем список чатов
                updateConnectionStatus(); // Обновляем статус подключения
            });
        }
        
        System.out.println("🖱️ Nico: Click listeners ready");
    }
    
    // Метод для открытия экрана чата
    private void openChatActivity(String chatName) {
        try {
            System.out.println("➡️ Nico: Opening chat with " + chatName);
            
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("CHAT_NAME", chatName);
            startActivity(intent);
            
        } catch (Exception e) {
            System.out.println("❌ Error opening chat: " + e.getMessage());
        }
    }
    
    // Метод для открытия экрана подключения
    private void openConnectActivity() {
        try {
            Intent intent = new Intent(MainActivity.this, ConnectActivity.class);
            startActivity(intent);
            System.out.println("🔗 Nico: Opening connection screen");
        } catch (Exception e) {
            System.out.println("❌ Error opening connection: " + e.getMessage());
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkManager != null) {
            networkManager.stopServer();
        }
    }
    
    // Модель чата для Nico
    class Chat {
        String name;
        String lastMessage; 
        String time;
        
        Chat(String name, String lastMessage, String time) {
            this.name = name;
            this.lastMessage = lastMessage;
            this.time = time;
        }
    }
}
