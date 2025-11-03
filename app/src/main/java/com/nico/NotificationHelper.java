package com.nico;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public class NotificationHelper {
    private static final String CHANNEL_ID = "nico_messages";
    private static final String CHANNEL_NAME = "Nico Messages";
    private static final String CHANNEL_DESCRIPTION = "Notifications for new messages in Nico Messenger";
    
    public static void showMessageNotification(Context context, String sender, String message) {
        try {
            System.out.println("🔔 Nico: Attempting to show notification - " + sender + ": " + message);
            
            // В Termux уведомления не работают, но логируем для тестирования
            System.out.println("📱 [SIMULATED NOTIFICATION] " + sender + ": " + message);
            
            // Здесь будет реальный код уведомлений когда соберём APK
            // Для Termux просто логируем
            
        } catch (Exception e) {
            System.out.println("❌ Nico: Notification error - " + e.getMessage());
        }
    }
    
    public static void showConnectionNotification(Context context, String deviceName, boolean connected) {
        try {
            String status = connected ? "Connected to " : "Disconnected from ";
            System.out.println("🔔 Nico: " + status + deviceName);
            
        } catch (Exception e) {
            System.out.println("❌ Nico: Connection notification error");
        }
    }
    
    public static void showDeviceDiscoveryNotification(Context context, String deviceName, String ip) {
        try {
            System.out.println("🔔 Nico: Discovered device - " + deviceName + " at " + ip);
            
        } catch (Exception e) {
            System.out.println("❌ Nico: Discovery notification error");
        }
    }
    
    public static void showTestNotification(Context context) {
        showMessageNotification(context, "Nico System", "Welcome to Nico Messenger! 🚀");
    }
    
    // Создаем канал уведомлений (для реального Android)
    private static void createNotificationChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription(CHANNEL_DESCRIPTION);
                manager.createNotificationChannel(channel);
                System.out.println("📢 Nico: Notification channel created");
            } catch (Exception e) {
                System.out.println("❌ Nico: Channel creation error");
            }
        }
    }
}
