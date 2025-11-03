package com.nico;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Map;

public class ConnectActivity extends AppCompatActivity 
    implements NetworkManager.NetworkListener {
    
    private EditText ipInput;
    private TextView myIpText, deviceNameText, statusText, discoveredTitle;
    private LinearLayout devicesLayout;
    private NetworkManager networkManager;
    private String selectedIp = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        
        // Инициализируем сетевой менеджер
        networkManager = new NetworkManager(this);
        networkManager.setListener(this);
        networkManager.startServer();
        
        setupiOSStyle();
        setupViews();
        setupClickListeners();
        
        // Показываем информацию об устройстве
        showDeviceInfo();
        
        System.out.println("🔗 Nico: Connect Activity started");
    }
    
    private void setupiOSStyle() {
        getWindow().setStatusBarColor(0xFFF2F2F7);
        getWindow().setNavigationBarColor(0xFFF2F2F7);
    }
    
    private void setupViews() {
        ipInput = findViewById(R.id.ipInput);
        myIpText = findViewById(R.id.myIpText);
        deviceNameText = findViewById(R.id.deviceNameText);
        statusText = findViewById(R.id.statusText);
        discoveredTitle = findViewById(R.id.discoveredTitle);
        devicesLayout = findViewById(R.id.devicesLayout);
    }
    
    private void setupClickListeners() {
        // Кнопка ручного подключения
        Button connectButton = findViewById(R.id.connectButton);
        connectButton.setOnClickListener(v -> connectToDevice());
        
        // Кнопка сканирования сети
        Button scanButton = findViewById(R.id.scanButton);
        scanButton.setOnClickListener(v -> scanNetwork());
    }
    
    private void showDeviceInfo() {
        String ip = networkManager.getLocalIpAddress();
        String deviceName = "Nico-" + ip.replace(".", "");
        
        myIpText.setText("IP: " + ip);
        deviceNameText.setText("Name: " + deviceName);
        
        System.out.println("📱 Nico: Device info - IP: " + ip + ", Name: " + deviceName);
    }
    
    private void connectToDevice() {
        String ip = ipInput.getText().toString().trim();
        if (!ip.isEmpty()) {
            selectedIp = ip;
            networkManager.saveDeviceIp(ip, "Manual_Device");
            
            updateStatus("Connecting to " + ip + "...");
            
            // Тестируем подключение отправкой тестового сообщения
            testConnection(ip);
            
        } else {
            Toast.makeText(this, "Please enter IP address", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void testConnection(String ip) {
        new Thread(() -> {
            try {
                // Отправляем тестовое сообщение
                networkManager.sendMessage(ip, "Test", "System", "Connection test");
                
                runOnUiThread(() -> {
                    updateStatus("✅ Connected to " + ip);
                    Toast.makeText(this, "Successfully connected!", Toast.LENGTH_SHORT).show();
                    
                    // Сохраняем IP для использования в чатах
                    saveConnectedIp(ip);
                });
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    updateStatus("❌ Failed to connect to " + ip);
                    Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void scanNetwork() {
        updateStatus("Scanning network...");
        devicesLayout.removeAllViews();
        discoveredTitle.setVisibility(View.GONE);
        
        // Запускаем обнаружение устройств
        networkManager.discoverDevices();
        
        // Показываем прогресс
        showProgressIndicator();
    }
    
    private void showProgressIndicator() {
        TextView progressText = new TextView(this);
        progressText.setText("Scanning...");
        progressText.setTextColor(0xFF8E8E93);
        progressText.setPadding(50, 30, 50, 30);
        progressText.setTextSize(14);
        progressText.setGravity(View.TEXT_ALIGNMENT_CENTER);
        
        devicesLayout.addView(progressText);
    }
    
    private void saveConnectedIp(String ip) {
        // Сохраняем IP в SharedPreferences для использования в чатах
        getSharedPreferences("nico_prefs", MODE_PRIVATE)
            .edit()
            .putString("connected_ip", ip)
            .apply();
        
        System.out.println("💾 Nico: Saved connected IP: " + ip);
        
        // Закрываем экран подключения через 2 секунды
        new android.os.Handler().postDelayed(() -> {
            finish();
        }, 2000);
    }
    
    private void updateStatus(String message) {
        runOnUiThread(() -> {
            statusText.setText(message);
            System.out.println("📡 Nico Status: " + message);
        });
    }
    
    // ===== NetworkListener Implementation =====
    
    @Override
    public void onMessageReceived(String chatName, String sender, String message) {
        // Сообщения получаем в ChatActivity
    }
    
    @Override
    public void onDeviceDiscovered(String ip, String deviceName) {
        runOnUiThread(() -> {
            updateStatus("Found: " + deviceName + " at " + ip);
            discoveredTitle.setVisibility(View.VISIBLE);
            
            // Убираем индикатор прогресса
            if (devicesLayout.getChildCount() > 0 && 
                devicesLayout.getChildAt(0) instanceof TextView) {
                TextView firstChild = (TextView) devicesLayout.getChildAt(0);
                if ("Scanning...".equals(firstChild.getText().toString())) {
                    devicesLayout.removeViewAt(0);
                }
            }
            
            // Добавляем устройство в список
            addDiscoveredDevice(ip, deviceName);
        });
    }
    
    @Override
    public void onConnectionStatusChanged(boolean connected) {
        runOnUiThread(() -> {
            if (connected) {
                updateStatus("✅ Connection established");
            } else {
                updateStatus("❌ Connection lost");
            }
        });
    }
    
    private void addDiscoveredDevice(String ip, String deviceName) {
        Button deviceButton = new Button(this);
        deviceButton.setText(deviceName + "\n" + ip);
        deviceButton.setBackgroundColor(0xFFFFFFFF);
        deviceButton.setTextColor(0xFF000000);
        deviceButton.setPadding(40, 20, 40, 20);
        deviceButton.setTextSize(12);
        
        deviceButton.setOnClickListener(v -> {
            selectedIp = ip;
            ipInput.setText(ip);
            updateStatus("Selected: " + deviceName);
            
            // Автоматически подключаемся к выбранному устройству
            connectToDevice();
        });
        
        devicesLayout.addView(deviceButton);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkManager != null) {
            networkManager.stopServer();
        }
    }
}
