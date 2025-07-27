package rjv.mg.myidm.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import rjv.mg.myidm.MyIDMApplication;
import rjv.mg.myidm.R;
import rjv.mg.myidm.data.database.dao.DownloadDao;
import rjv.mg.myidm.data.database.entity.DownloadEntity;
import rjv.mg.myidm.domain.downloader.DownloadManager;
import rjv.mg.myidm.domain.model.DownloadStatus;
import rjv.mg.myidm.ui.MainActivity;

@AndroidEntryPoint
public class DownloadService extends Service {
    
    private static final String TAG = "DownloadService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = MyIDMApplication.DOWNLOAD_CHANNEL_ID;
    
    @Inject
    DownloadManager downloadManager;
    
    @Inject
    DownloadDao downloadDao;
    
    private ScheduledExecutorService updateExecutor;
    private boolean isServiceRunning = false;
    
    public static void startService(Context context) {
        Intent intent = new Intent(context, DownloadService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
    
    public static void stopService(Context context) {
        Intent intent = new Intent(context, DownloadService.class);
        context.stopService(intent);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "DownloadService created");
        
        createNotificationChannel();
        updateExecutor = Executors.newSingleThreadScheduledExecutor();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "DownloadService started");
        
        if (!isServiceRunning) {
            startForeground(NOTIFICATION_ID, createNotification());
            startUpdateScheduler();
            isServiceRunning = true;
        }
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DownloadService destroyed");
        
        if (updateExecutor != null) {
            updateExecutor.shutdown();
        }
        
        isServiceRunning = false;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Download progress notifications");
            channel.setShowBadge(false);
            channel.setSound(null, null);
            channel.enableVibration(false);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Create action buttons
        PendingIntent pauseIntent = PendingIntent.getService(
            this, 1, new Intent(this, DownloadService.class).setAction("PAUSE_ALL"),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        PendingIntent resumeIntent = PendingIntent.getService(
            this, 2, new Intent(this, DownloadService.class).setAction("RESUME_ALL"),
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MyIDM Download Manager")
            .setContentText("Managing downloads...")
            .setSmallIcon(R.drawable.ic_download)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_pause, "Pause All", pauseIntent)
            .addAction(R.drawable.ic_play, "Resume All", resumeIntent)
            .setOngoing(true)
            .setSilent(true)
            .build();
    }
    
    private void updateNotification(String title, String content, int progress) {
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_download)
                .setProgress(100, progress, false)
                .setOngoing(true)
                .setSilent(true);
            
            // Add action buttons
            Intent pauseIntent = new Intent(this, DownloadService.class).setAction("PAUSE_ALL");
            Intent resumeIntent = new Intent(this, DownloadService.class).setAction("RESUME_ALL");
            
            PendingIntent pausePendingIntent = PendingIntent.getService(
                this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            PendingIntent resumePendingIntent = PendingIntent.getService(
                this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            builder.addAction(R.drawable.ic_pause, "Pause All", pausePendingIntent)
                   .addAction(R.drawable.ic_play, "Resume All", resumePendingIntent);
            
            // Add main activity intent
            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            
            PendingIntent mainPendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            builder.setContentIntent(mainPendingIntent);
            
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }
    
    private void startUpdateScheduler() {
        updateExecutor.scheduleAtFixedRate(() -> {
            try {
                updateNotificationContent();
            } catch (Exception e) {
                Log.e(TAG, "Error updating notification", e);
            }
        }, 0, 2, TimeUnit.SECONDS);
    }
    
    private void updateNotificationContent() {
        try {
            List<DownloadEntity> activeDownloads = downloadDao.getByStatuses(
                List.of(DownloadStatus.DOWNLOADING, DownloadStatus.RESUMING)
            );
            
            if (activeDownloads.isEmpty()) {
                // No active downloads, stop service
                stopSelf();
                return;
            }
            
            // Calculate overall progress
            long totalSize = 0;
            long totalDownloaded = 0;
            long totalSpeed = 0;
            
            for (DownloadEntity download : activeDownloads) {
                totalSize += download.getFileSize();
                totalDownloaded += download.getDownloadedSize();
                totalSpeed += download.getDownloadSpeed();
            }
            
            int overallProgress = totalSize > 0 ? (int) (totalDownloaded * 100 / totalSize) : 0;
            
            String title = "Downloading " + activeDownloads.size() + " files";
            String content = formatProgress(totalDownloaded, totalSize) + 
                           " • " + formatSpeed(totalSpeed);
            
            updateNotification(title, content, overallProgress);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification content", e);
        }
    }
    
    private String formatProgress(long downloaded, long total) {
        if (total <= 0) return "0%";
        
        int progress = (int) (downloaded * 100 / total);
        return progress + "%";
    }
    
    private String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return bytesPerSecond + " B/s";
        } else if (bytesPerSecond < 1024 * 1024) {
            return String.format("%.1f KB/s", bytesPerSecond / 1024.0);
        } else {
            return String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0));
        }
    }
    
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "Task removed, but service continues running");
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "Low memory warning received");
    }
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.d(TAG, "Memory trim level: " + level);
    }
} 