package rjv.mg.myidm.example;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import rjv.mg.myidm.data.database.entity.DownloadEntity;
import rjv.mg.myidm.data.database.entity.DownloadSegmentEntity;
import rjv.mg.myidm.domain.downloader.MultiThreadDownloader;
import rjv.mg.myidm.domain.model.DownloadType;

import java.util.List;

import java.io.File;

/**
 * Exemple d'utilisation du Downloader multi-threads
 * 
 * Cet exemple montre comment :
 * - Créer un téléchargement avec segments multiples
 * - Gérer les callbacks de progression
 * - Gérer les erreurs et les retry
 * - Pause/reprise des téléchargements
 */
public class MultiThreadDownloadExample {
    
    private static final String TAG = "MultiThreadDownloadExample";
    
    private Context context;
    private MultiThreadDownloader downloader;
    
    public MultiThreadDownloadExample(Context context) {
        this.context = context;
    }
    
    /**
     * Exemple de téléchargement d'un fichier vidéo avec 16 segments
     */
    public void downloadVideoExample() {
        // URL du fichier à télécharger
        String videoUrl = "https://example.com/video.mp4";
        String filename = "video_example.mp4";
        
        // Créer l'entité de téléchargement
        DownloadEntity download = new DownloadEntity(videoUrl, filename, DownloadType.HTTP_HTTPS);
        
        // Configuration avancée
        download.setFileSize(100 * 1024 * 1024); // 100MB (sera détecté automatiquement)
        download.setMaxConcurrentSegments(16); // 16 segments simultanés
        download.setSegmentCount(16);
        download.setPriority(1); // Priorité élevée
        download.setUserAgent("MyIDM/1.0");
        
        // Définir le chemin de destination
        File downloadDir = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS), "MyIDM");
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        download.setFilePath(new File(downloadDir, filename).getAbsolutePath());
        
        // Créer le downloader
        downloader = new MultiThreadDownloader(context, download);
        
        // Configurer les callbacks
        setupCallbacks();
        
        // Démarrer le téléchargement
        downloader.start();
        
        Log.i(TAG, "Démarrage du téléchargement: " + videoUrl);
    }
    
    /**
     * Exemple de téléchargement avec gestion des erreurs et retry
     */
    public void downloadWithRetryExample() {
        String fileUrl = "https://example.com/large_file.zip";
        String filename = "large_file.zip";
        
        DownloadEntity download = new DownloadEntity(fileUrl, filename, DownloadType.HTTP_HTTPS);
        download.setMaxConcurrentSegments(8);
        download.setMaxRetries(5); // 5 tentatives par segment
        download.setPriority(0);
        
        File downloadDir = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS), "MyIDM");
        download.setFilePath(new File(downloadDir, filename).getAbsolutePath());
        
        downloader = new MultiThreadDownloader(context, download);
        setupCallbacks();
        downloader.start();
    }
    
    /**
     * Configuration des callbacks pour le monitoring
     */
    private void setupCallbacks() {
        // Callback de progression
        downloader.setProgressCallback(new MultiThreadDownloader.DownloadProgressCallback() {
            @Override
            public void onProgress(long downloadedBytes, long totalBytes, int progress, long speed) {
                Log.d(TAG, String.format("Progression: %d%% - %s/s", 
                    progress, formatSpeed(speed)));
                
                // Mettre à jour l'UI ici
                updateProgressUI(progress, speed);
            }
            
            @Override
            public void onSegmentProgress(int segmentIndex, long downloadedBytes, 
                                        long totalBytes, int progress) {
                Log.v(TAG, String.format("Segment %d: %d%%", segmentIndex, progress));
            }
        });
        
        // Callback de completion
        downloader.setCompletionCallback(new MultiThreadDownloader.DownloadCompletionCallback() {
            @Override
            public void onCompleted(String filePath, String checksum) {
                Log.i(TAG, "Téléchargement terminé: " + filePath);
                Log.i(TAG, "Checksum MD5: " + checksum);
                
                // Notifier l'utilisateur
                showCompletionNotification(filePath);
            }
            
            @Override
            public void onMerged(String filePath) {
                Log.i(TAG, "Fichiers fusionnés: " + filePath);
            }
        });
        
        // Callback d'erreur
        downloader.setErrorCallback(new MultiThreadDownloader.DownloadErrorCallback() {
            @Override
            public void onError(String error, int segmentIndex) {
                Log.e(TAG, "Erreur de téléchargement: " + error);
                if (segmentIndex >= 0) {
                    Log.e(TAG, "Segment affecté: " + segmentIndex);
                }
                
                // Gérer l'erreur (retry automatique, notification, etc.)
                handleDownloadError(error, segmentIndex);
            }
            
            @Override
            public void onSegmentFailed(int segmentIndex, String error) {
                Log.w(TAG, String.format("Segment %d échoué: %s", segmentIndex, error));
                
                // Le segment sera automatiquement retry
                // Vous pouvez aussi implémenter une logique personnalisée ici
            }
        });
    }
    
    /**
     * Exemple de gestion des contrôles de téléchargement
     */
    public void controlDownloadExample() {
        if (downloader == null) return;
        
        // Pause du téléchargement
        downloader.pause();
        Log.i(TAG, "Téléchargement mis en pause");
        
        // Attendre 5 secondes
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Reprise du téléchargement
        downloader.resume();
        Log.i(TAG, "Téléchargement repris");
        
        // Attendre encore 10 secondes
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Annulation du téléchargement
        downloader.cancel();
        Log.i(TAG, "Téléchargement annulé");
    }
    
    /**
     * Exemple de monitoring des segments
     */
    public void monitorSegmentsExample() {
        if (downloader == null) return;
        
        // Obtenir la liste des segments
        List<DownloadSegmentEntity> segments = downloader.getSegments();
        
        Log.i(TAG, "Nombre de segments: " + segments.size());
        
        for (DownloadSegmentEntity segment : segments) {
            Log.d(TAG, String.format("Segment %d: %s - %d%%", 
                segment.getSegmentIndex(),
                segment.getStatus(),
                segment.getProgress()));
        }
        
        // Obtenir les statistiques
        long downloadedBytes = downloader.getDownloadedBytes();
        int progress = downloader.getProgress();
        
        Log.i(TAG, String.format("Total téléchargé: %s - Progression: %d%%", 
            formatBytes(downloadedBytes), progress));
    }
    
    /**
     * Exemple de téléchargement avec configuration personnalisée
     */
    public void customConfigurationExample() {
        String url = "https://example.com/file.iso";
        String filename = "file.iso";
        
        DownloadEntity download = new DownloadEntity(url, filename, DownloadType.HTTP_HTTPS);
        
        // Configuration pour un fichier très volumineux
        download.setMaxConcurrentSegments(32); // Maximum de segments
        download.setSegmentCount(32);
        download.setPriority(2); // Priorité très élevée
        download.setMaxRetries(10); // Plus de tentatives
        
        // Headers personnalisés
        download.setUserAgent("MyIDM/1.0 (Advanced Downloader)");
        download.setReferer("https://example.com");
        download.setHeaders("{\"Authorization\": \"Bearer token123\"}");
        
        File downloadDir = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS), "MyIDM");
        download.setFilePath(new File(downloadDir, filename).getAbsolutePath());
        
        downloader = new MultiThreadDownloader(context, download);
        setupCallbacks();
        downloader.start();
    }
    
    // Méthodes utilitaires
    private String formatSpeed(long bytesPerSecond) {
        if (bytesPerSecond < 1024) {
            return bytesPerSecond + " B/s";
        } else if (bytesPerSecond < 1024 * 1024) {
            return String.format("%.1f KB/s", bytesPerSecond / 1024.0);
        } else {
            return String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0));
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    private void updateProgressUI(int progress, long speed) {
        // Implémenter la mise à jour de l'interface utilisateur
        // Par exemple, mettre à jour une ProgressBar et un TextView
    }
    
    private void showCompletionNotification(String filePath) {
        // Implémenter la notification de completion
        // Utiliser NotificationManager pour afficher une notification
    }
    
    private void handleDownloadError(String error, int segmentIndex) {
        // Implémenter la gestion d'erreur personnalisée
        // Par exemple, afficher un dialog, retry manuel, etc.
    }
} 