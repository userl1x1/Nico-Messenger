package com.nico;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        setupiOSStyle();
        System.out.println("⚙️ Nico: Settings Activity started");
    }
    
    private void setupiOSStyle() {
        getWindow().setStatusBarColor(0xFFF2F2F7);
        getWindow().setNavigationBarColor(0xFFF2F2F7);
    }
}
