package rjv.mg.myidm;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import dagger.hilt.android.HiltAndroidApp;
import rjv.mg.myidm.data.database.DownloadDatabase;
import rjv.mg.myidm.domain.downloader.TorrentDownloader;

@HiltAndroidApp
public class MyIDMApplication extends Application implements Configuration.Provider {
    
    private static final String TAG = "MyIDMApplication";
    
    // Notification channels
    public static final String DOWNLOAD_CHANNEL_ID = "download_channel";
    public static final String BROWSER_CHANNEL_ID = "browser_channel";
    public static final String TORRENT_CHANNEL_ID = "torrent_channel";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "MyIDM Application starting...");
        
        // Initialize database
        initializeDatabase();
        
        // Create notification channels
        createNotificationChannels();
        
        // Initialize WorkManager
        // WorkManager.initialize(this, getWorkManagerConfiguration());
        
        Log.d(TAG, "MyIDM Application initialized successfully");
    }
    
    private void initializeDatabase() {
        try {
            // Initialize Room database
            DownloadDatabase.getInstance(this);
            Log.d(TAG, "Database initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize database", e);
        }
    }
    
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager != null) {
                // Download channel
                NotificationChannel downloadChannel = new NotificationChannel(
                    DOWNLOAD_CHANNEL_ID,
                    "Downloads",
                    NotificationManager.IMPORTANCE_LOW
                );
                downloadChannel.setDescription("Download progress notifications");
                downloadChannel.setShowBadge(false);
                notificationManager.createNotificationChannel(downloadChannel);
                
                // Browser channel
                NotificationChannel browserChannel = new NotificationChannel(
                    BROWSER_CHANNEL_ID,
                    "Browser",
                    NotificationManager.IMPORTANCE_DEFAULT
                );
                browserChannel.setDescription("Browser notifications");
                browserChannel.setShowBadge(false);
                notificationManager.createNotificationChannel(browserChannel);
                
                // Torrent channel
                NotificationChannel torrentChannel = new NotificationChannel(
                    TORRENT_CHANNEL_ID,
                    "Torrents",
                    NotificationManager.IMPORTANCE_LOW
                );
                torrentChannel.setDescription("Torrent download notifications");
                torrentChannel.setShowBadge(false);
                notificationManager.createNotificationChannel(torrentChannel);
                
                Log.d(TAG, "Notification channels created successfully");
            }
        }
    }
    
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setDefaultProcessName("rjv.mg.myidm")
            .build();
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        
        Log.d(TAG, "MyIDM Application terminating...");
        
        // TorrentDownloader.shutdownSession(); // Neutralisé car gestion torrent désactivée
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "Low memory warning received");
        
        // Clear caches and non-essential data
        System.gc();
    }
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        
        switch (level) {
            case TRIM_MEMORY_UI_HIDDEN:
                Log.d(TAG, "UI hidden - clearing UI caches");
                break;
            case TRIM_MEMORY_RUNNING_MODERATE:
                Log.d(TAG, "Moderate memory pressure");
                break;
            case TRIM_MEMORY_RUNNING_LOW:
                Log.d(TAG, "Low memory pressure");
                break;
            case TRIM_MEMORY_RUNNING_CRITICAL:
                Log.w(TAG, "Critical memory pressure");
                System.gc();
                break;
        }
    }
} 