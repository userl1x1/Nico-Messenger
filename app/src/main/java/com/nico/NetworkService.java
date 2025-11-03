package com.nico;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class NetworkService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("🔧 Nico: Network Service started");
        return START_STICKY;
    }
}
