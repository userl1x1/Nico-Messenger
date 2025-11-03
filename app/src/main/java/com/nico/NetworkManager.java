package com.nico;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkManager {
    private static final int PORT = 8888;
    private static final int DISCOVERY_PORT = 8889;
    private static final String DISCOVERY_MESSAGE = "NICO_DISCOVERY";
    private static final String DISCOVERY_RESPONSE = "NICO_RESPONSE";
    
    private ServerSocket serverSocket;
    private DatagramSocket discoverySocket;
    private boolean isRunning = false;
    private Context context;
    private SharedPreferences prefs;
    private Map<String, String> discoveredDevices = new ConcurrentHashMap<>();
    
    // Интерфейс для callback'ов
    public interface NetworkListener {
        void onMessageReceived(String chatName, String sender, String message);
        void onDeviceDiscovered(String ip, String deviceName);
        void onConnectionStatusChanged(boolean connected);
    }
    
    private NetworkListener listener;
    
    public NetworkManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("nico_prefs", Context.MODE_PRIVATE);
    }
    
    public void setListener(NetworkListener listener) {
        this.listener = listener;
    }
    
    // Запускаем сервер для приёма сообщений
    public void startServer() {
        if (isRunning) return;
        
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                isRunning = true;
                
                System.out.println("🔄 Nico: Server started on port " + PORT);
                System.out.println("📡 Nico: Your IP - " + getLocalIpAddress());
                
                // Запускаем discovery сервер
                startDiscoveryServer();
                
                while (isRunning) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("🔗 Nico: New connection from " + 
                            clientSocket.getInetAddress().getHostAddress());
                        
                        // Обрабатываем сообщения от клиента
                        handleClient(clientSocket);
                        
                    } catch (SocketException e) {
                        System.out.println("🛑 Nico: Server socket closed");
                    }
                }
                
            } catch (IOException e) {
                System.out.println("❌ Nico: Server error - " + e.getMessage());
            }
        }).start();
    }
    
    // Запускаем discovery сервер для обнаружения устройств
    private void startDiscoveryServer() {
        new Thread(() -> {
            try {
                discoverySocket = new DatagramSocket(DISCOVERY_PORT);
                discoverySocket.setBroadcast(true);
                
                byte[] buffer = new byte[1024];
                
                while (isRunning) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    discoverySocket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength()).trim();
                    String senderIp = packet.getAddress().getHostAddress();
                    
                    if (DISCOVERY_MESSAGE.equals(message)) {
                        System.out.println("🔍 Nico: Discovery request from " + senderIp);
                        
                        // Отправляем ответ
                        sendDiscoveryResponse(senderIp);
                        
                    } else if (message.startsWith(DISCOVERY_RESPONSE)) {
                        // Получили ответ на наш discovery запрос
                        String deviceName = message.substring(DISCOVERY_RESPONSE.length() + 1);
                        discoveredDevices.put(senderIp, deviceName);
                        
                        System.out.println("✅ Nico: Discovered device - " + deviceName + " at " + senderIp);
                        
                        if (listener != null) {
                            listener.onDeviceDiscovered(senderIp, deviceName);
                        }
                    }
                }
                
            } catch (IOException e) {
                if (isRunning) {
                    System.out.println("❌ Nico: Discovery server error - " + e.getMessage());
                }
            }
        }).start();
    }
    
    // Отправляем ответ на discovery запрос
    private void sendDiscoveryResponse(String targetIp) {
        try {
            String deviceName = "Nico-" + getLocalIpAddress().replace(".", "");
            String response = DISCOVERY_RESPONSE + "|" + deviceName;
            
            DatagramSocket socket = new DatagramSocket();
            byte[] data = response.getBytes();
            
            DatagramPacket packet = new DatagramPacket(
                data, data.length, 
                InetAddress.getByName(targetIp), DISCOVERY_PORT
            );
            
            socket.send(packet);
            socket.close();
            
        } catch (IOException e) {
            System.out.println("❌ Nico: Failed to send discovery response");
        }
    }
    
    // Останавливаем сервер
    public void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (discoverySocket != null) {
                discoverySocket.close();
            }
            System.out.println("🛑 Nico: Server stopped");
        } catch (IOException e) {
            System.out.println("❌ Nico: Error stopping server");
        }
    }
    
    // Обработка сообщений от клиента
    private void handleClient(Socket clientSocket) {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream())
                );
                
                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println("📨 Nico: Received raw message - " + message);
                    
                    // Парсим сообщение
                    processReceivedMessage(message, clientSocket.getInetAddress().getHostAddress());
                }
                
            } catch (IOException e) {
                System.out.println("❌ Nico: Client disconnected");
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // Игнорируем ошибку закрытия
                }
            }
        }).start();
    }
    
    // Отправка сообщения другому устройству
    public void sendMessage(String targetIp, String chatName, String sender, String message) {
        new Thread(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(targetIp, PORT), 3000);
                
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                
                // Формат: CHAT_NAME|SENDER|MESSAGE|TIMESTAMP
                String formattedMessage = String.format("%s|%s|%s|%d", 
                    chatName, sender, message, System.currentTimeMillis());
                
                writer.println(formattedMessage);
                writer.close();
                socket.close();
                
                System.out.println("✈️ Nico: Message sent to " + targetIp + " - " + message);
                
                // Сохраняем в базу как исходящее сообщение
                DatabaseHelper dbHelper = new DatabaseHelper(context);
                dbHelper.addMessage(chatName, sender, message, 
                    formatTimestamp(System.currentTimeMillis()), true);
                
            } catch (IOException e) {
                System.out.println("❌ Nico: Failed to send to " + targetIp + " - " + e.getMessage());
                
                if (listener != null) {
                    listener.onConnectionStatusChanged(false);
                }
            }
        }).start();
    }
    
    // Обработка полученного сообщения
    private void processReceivedMessage(String message, String senderIp) {
        try {
            String[] parts = message.split("\\|", 4);
            if (parts.length == 4) {
                String chatName = parts[0];
                String sender = parts[1];
                String text = parts[2];
                long timestamp = Long.parseLong(parts[3]);
                
                System.out.println("💬 Nico: Parsed message - " + sender + ": " + text);
                
                // Сохраняем в базу данных
                DatabaseHelper dbHelper = new DatabaseHelper(context);
                dbHelper.addMessage(chatName, sender, text, formatTimestamp(timestamp), false);
                
                // Показываем уведомление
                NotificationHelper.showMessageNotification(context, sender, text);
                
                // Уведомляем слушателя
                if (listener != null) {
                    listener.onMessageReceived(chatName, sender, text);
                }
                
            } else {
                System.out.println("⚠️ Nico: Invalid message format from " + senderIp);
            }
        } catch (Exception e) {
            System.out.println("❌ Nico: Error processing message from " + senderIp + " - " + e.getMessage());
        }
    }
    
    // Сканируем сеть на наличие других устройств Nico
    public void discoverDevices() {
        new Thread(() -> {
            try {
                String localIp = getLocalIpAddress();
                String baseIp = localIp.substring(0, localIp.lastIndexOf(".") + 1);
                
                System.out.println("🔍 Nico: Starting network discovery...");
                
                // Очищаем список устройств
                discoveredDevices.clear();
                
                // Отправляем broadcast запрос
                sendBroadcastDiscovery();
                
                // Также проверяем конкретные IP в подсети
                for (int i = 1; i <= 255; i++) {
                    String testIp = baseIp + i;
                    if (!testIp.equals(localIp)) {
                        sendDirectDiscovery(testIp);
                    }
                }
                
            } catch (Exception e) {
                System.out.println("❌ Nico: Discovery error - " + e.getMessage());
            }
        }).start();
    }
    
    // Отправляем broadcast discovery запрос
    private void sendBroadcastDiscovery() {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);
            
            byte[] data = DISCOVERY_MESSAGE.getBytes();
            DatagramPacket packet = new DatagramPacket(
                data, data.length, 
                InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT
            );
            
            socket.send(packet);
            socket.close();
            
        } catch (IOException e) {
            System.out.println("❌ Nico: Broadcast discovery failed");
        }
    }
    
    // Отправляем direct discovery запрос
    private void sendDirectDiscovery(String ip) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(1000);
            
            byte[] data = DISCOVERY_MESSAGE.getBytes();
            DatagramPacket packet = new DatagramPacket(
                data, data.length, 
                InetAddress.getByName(ip), DISCOVERY_PORT
            );
            
            socket.send(packet);
            
            // Ждём ответ
            byte[] buffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);
            
            String message = new String(response.getData(), 0, response.getLength()).trim();
            if (message.startsWith(DISCOVERY_RESPONSE)) {
                String deviceName = message.substring(DISCOVERY_RESPONSE.length() + 1);
                discoveredDevices.put(ip, deviceName);
                
                System.out.println("✅ Nico: Direct discovered - " + deviceName + " at " + ip);
                
                if (listener != null) {
                    listener.onDeviceDiscovered(ip, deviceName);
                }
            }
            
            socket.close();
            
        } catch (SocketTimeoutException e) {
            // Таймаут - устройство не ответило
        } catch (IOException e) {
            // Ошибка соединения
        }
    }
    
    // Получаем список обнаруженных устройств
    public Map<String, String> getDiscoveredDevices() {
        return new HashMap<>(discoveredDevices);
    }
    
    // Получаем локальный IP адрес
    public String getLocalIpAddress() {
        try {
            WifiManager wm = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
            return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        } catch (Exception e) {
            return "192.168.1.100"; // Fallback IP
        }
    }
    
    // Сохраняем IP устройства для быстрого доступа
    public void saveDeviceIp(String ip, String name) {
        prefs.edit().putString("saved_device_" + name, ip).apply();
    }
    
    // Загружаем сохранённый IP устройства
    public String getSavedDeviceIp(String name) {
        return prefs.getString("saved_device_" + name, null);
    }
    
    // Форматируем timestamp
    private String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        return new java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(date);
    }
}
