package com.example.weatherroute;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class NavigationService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        // Build the persistent notification for the system tray
        Notification notification = new Notification.Builder(this, "ROUTE_CHANNEL")
                .setContentTitle("Weather Route Active")
                .setContentText("Monitoring live weather conditions...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();

        // Start the service in the foreground
        startForeground(1, notification);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not using bound service
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "ROUTE_CHANNEL",
                    "Active Route Monitoring",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }
}