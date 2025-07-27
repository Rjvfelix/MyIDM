package rjv.mg.myidm.domain.downloader;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import rjv.mg.myidm.data.database.dao.DownloadDao;
import rjv.mg.myidm.data.database.dao.DownloadSegmentDao;
import rjv.mg.myidm.data.database.entity.DownloadEntity;
import rjv.mg.myidm.data.database.entity.DownloadSegmentEntity;
import rjv.mg.myidm.domain.browser.VideoDetector;
import rjv.mg.myidm.domain.model.DownloadStatus;
import rjv.mg.myidm.domain.model.DownloadType;
import rjv.mg.myidm.domain.model.SegmentStatus;

@Singleton
public class DownloadManager {
    
    private static final String TAG = "DownloadManager";
    
    // Configuration
    private static final int MAX_CONCURRENT_DOWNLOADS = 30; // Premium: 30, Free: 5
    private static final int MAX_CONCURRENT_SEGMENTS = 32;
    private static final int QUEUE_CAPACITY = 100;
    
    private final Context context;
    private final DownloadDao downloadDao;
    private final DownloadSegmentDao segmentDao;
    private final VideoDetector videoDetector;
    
    // Download management
    private final Map<Long, MultiThreadDownloader> activeDownloaders;
    // private final Map<Long, TorrentDownloader> activeTorrents; // Désactivé car gestion torrent désactivée
    private final PriorityBlockingQueue<DownloadEntity> downloadQueue;
    private final ExecutorService downloadExecutor;
    private final ExecutorService queueExecutor;
    
    // State
    private boolean isInitialized = false;
    private int currentConcurrentDownloads = 0;
    
    @Inject
    public DownloadManager(@ApplicationContext Context context, 
                          DownloadDao downloadDao, 
                          DownloadSegmentDao segmentDao,
                          VideoDetector videoDetector) {
        this.context = context;
        this.downloadDao = downloadDao;
        this.segmentDao = segmentDao;
        this.videoDetector = videoDetector;
        
        this.activeDownloaders = new ConcurrentHashMap<>();
        // this.activeTorrents = new ConcurrentHashMap<>(); // Désactivé car gestion torrent désactivée
        this.downloadQueue = new PriorityBlockingQueue<>(QUEUE_CAPACITY, 
            (d1, d2) -> Integer.compare(d2.getPriority(), d1.getPriority()));
        
        this.downloadExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_DOWNLOADS);
        this.queueExecutor = Executors.newSingleThreadExecutor();
    }
    
    public void initialize() {
        if (isInitialized) return;
        
        Log.d(TAG, "Initializing DownloadManager...");
        
        // Restore active downloads from database
        restoreActiveDownloads();
        
        // Start queue processor
        startQueueProcessor();
        
        isInitialized = true;
        Log.d(TAG, "DownloadManager initialized successfully");
    }
    
    private void restoreActiveDownloads() {
        try {
            List<DownloadEntity> activeDownloads = downloadDao.getByStatuses(
                List.of(DownloadStatus.DOWNLOADING, DownloadStatus.RESUMING, DownloadStatus.PAUSED)
            );
            
            for (DownloadEntity download : activeDownloads) {
                if (download.getStatus() == DownloadStatus.DOWNLOADING || 
                    download.getStatus() == DownloadStatus.RESUMING) {
                    // Resume download
                    startDownload(download);
                } else if (download.getStatus() == DownloadStatus.PAUSED) {
                    // Add to queue for later resumption
                    downloadQueue.offer(download);
                }
            }
            
            Log.d(TAG, "Restored " + activeDownloads.size() + " active downloads");
        } catch (Exception e) {
            Log.e(TAG, "Error restoring active downloads", e);
        }
    }
    
    private void startQueueProcessor() {
        queueExecutor.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Wait for available slot
                    while (currentConcurrentDownloads >= MAX_CONCURRENT_DOWNLOADS) {
                        Thread.sleep(1000);
                    }
                    
                    // Take next download from queue
                    DownloadEntity download = downloadQueue.take();
                    
                    // Start download
                    startDownloadInternal(download);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    Log.e(TAG, "Error in queue processor", e);
                }
            }
        });
    }
    
    public DownloadEntity createDownloadFromUrl(String url) {
        try {
            // Detect download type
            DownloadType type = VideoDetector.detectDownloadType(url);
            
            // Get file info
            FileInfo fileInfo = getFileInfo(url);
            
            // Create download entity
            DownloadEntity download = new DownloadEntity(url, fileInfo.filename, type);
            download.setFileSize(fileInfo.fileSize);
            download.setContentType(fileInfo.contentType);
            download.setMaxConcurrentSegments(calculateOptimalSegments(fileInfo.fileSize));
            
            // Set download path
            String downloadPath = getDownloadPath(fileInfo.filename, type);
            download.setFilePath(downloadPath);
            
            return download;
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating download from URL: " + url, e);
            return null;
        }
    }
    
    private FileInfo getFileInfo(String url) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL urlObj = new URL(url);
            connection = (HttpURLConnection) urlObj.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP error: " + responseCode);
            }
            
            String filename = getFilenameFromUrl(url, connection);
            long fileSize = connection.getContentLengthLong();
            String contentType = connection.getContentType();
            
            return new FileInfo(filename, fileSize, contentType);
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    private String getFilenameFromUrl(String url, HttpURLConnection connection) {
        // Try to get filename from Content-Disposition header
        String contentDisposition = connection.getHeaderField("Content-Disposition");
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            int start = contentDisposition.indexOf("filename=") + 9;
            int end = contentDisposition.indexOf(";", start);
            if (end == -1) end = contentDisposition.length();
            String filename = contentDisposition.substring(start, end).replace("\"", "");
            if (!filename.isEmpty()) {
                return filename;
            }
        }
        
        // Extract from URL
        String filename = url.substring(url.lastIndexOf('/') + 1);
        if (filename.contains("?")) {
            filename = filename.substring(0, filename.indexOf('?'));
        }
        
        // Generate default filename if needed
        if (filename.isEmpty() || filename.equals("/")) {
            filename = "download_" + System.currentTimeMillis();
        }
        
        return filename;
    }
    
    private int calculateOptimalSegments(long fileSize) {
        if (fileSize <= 0) return 8;
        
        // Calculate based on file size
        int segments = (int) Math.min(
            Math.max(fileSize / (1024 * 1024), 8), // At least 8 segments
            MAX_CONCURRENT_SEGMENTS
        );
        
        return segments;
    }
    
    private String getDownloadPath(String filename, DownloadType type) {
        File downloadDir;
        
        switch (type) {
            case TORRENT:
                downloadDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "MyIDM/Torrents");
                break;
            case M3U8:
            case DASH:
                downloadDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "MyIDM/Streaming");
                break;
            case YOUTUBE:
            case FACEBOOK:
            case INSTAGRAM:
            case TIKTOK:
                downloadDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "MyIDM/Social");
                break;
            default:
                downloadDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "MyIDM");
                break;
        }
        
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        
        return new File(downloadDir, filename).getAbsolutePath();
    }
    
    public void startDownload(DownloadEntity download) {
        try {
            // Update status
            download.setStatus(DownloadStatus.QUEUED);
            downloadDao.update(download);
            
            // Add to queue
            downloadQueue.offer(download);
            
            Log.d(TAG, "Download queued: " + download.getUrl());
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting download", e);
            download.setStatus(DownloadStatus.FAILED);
            downloadDao.update(download);
        }
    }
    
    private void startDownloadInternal(DownloadEntity download) {
        try {
            currentConcurrentDownloads++;
            
            // Update status
            download.setStatus(DownloadStatus.DOWNLOADING);
            download.setStartedAt(new java.util.Date());
            downloadDao.update(download);
            
            switch (download.getType()) {
                case TORRENT:
                    startTorrentDownload(download);
                    break;
                case M3U8:
                case DASH:
                    startStreamingDownload(download);
                    break;
                default:
                    startHttpDownload(download);
                    break;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting download internal", e);
            download.setStatus(DownloadStatus.FAILED);
            downloadDao.update(download);
            currentConcurrentDownloads--;
        }
    }
    
    private void startHttpDownload(DownloadEntity download) {
        downloadExecutor.execute(() -> {
            try {
                MultiThreadDownloader downloader = new MultiThreadDownloader(context, download);
                activeDownloaders.put(download.getId(), downloader);
                
                // Setup callbacks
                setupDownloaderCallbacks(downloader, download);
                
                // Start download
                downloader.start();
                
            } catch (Exception e) {
                Log.e(TAG, "Error in HTTP download", e);
                handleDownloadError(download, e.getMessage());
            }
        });
    }
    
    private void startTorrentDownload(DownloadEntity download) {
        downloadExecutor.execute(() -> {
            try {
                // Fonctionnalité torrent désactivée temporairement
                Log.w(TAG, "Torrent downloading is currently disabled (libtorrent non intégré)");
                download.setStatus(DownloadStatus.FAILED);
                downloadDao.update(download);
                currentConcurrentDownloads--;
            } catch (Exception e) {
                Log.e(TAG, "Error in torrent download", e);
                handleDownloadError(download, e.getMessage());
            }
        });
    }
    
    private void startStreamingDownload(DownloadEntity download) {
        // For streaming downloads, we'll use a specialized downloader
        // This will be implemented to handle M3U8/DASH conversion
        downloadExecutor.execute(() -> {
            try {
                // TODO: Implement streaming downloader
                Log.d(TAG, "Streaming download not yet implemented");
                download.setStatus(DownloadStatus.FAILED);
                downloadDao.update(download);
                currentConcurrentDownloads--;
                
            } catch (Exception e) {
                Log.e(TAG, "Error in streaming download", e);
                handleDownloadError(download, e.getMessage());
            }
        });
    }
    
    private void setupDownloaderCallbacks(MultiThreadDownloader downloader, DownloadEntity download) {
        downloader.setProgressCallback(new MultiThreadDownloader.DownloadProgressCallback() {
            @Override
            public void onProgress(long downloadedBytes, long totalBytes, int progress, long speed) {
                updateDownloadProgress(download.getId(), downloadedBytes, totalBytes, progress, speed);
            }
            
            @Override
            public void onSegmentProgress(int segmentIndex, long downloadedBytes, long totalBytes, int progress) {
                updateSegmentProgress(download.getId(), segmentIndex, downloadedBytes, totalBytes, progress);
            }
        });
        
        downloader.setCompletionCallback(new MultiThreadDownloader.DownloadCompletionCallback() {
            @Override
            public void onCompleted(String filePath, String checksum) {
                handleDownloadCompleted(download.getId(), filePath, checksum);
            }
            
            @Override
            public void onMerged(String filePath) {
                Log.d(TAG, "Download merged: " + filePath);
            }
        });
        
        downloader.setErrorCallback(new MultiThreadDownloader.DownloadErrorCallback() {
            @Override
            public void onError(String error, int segmentIndex) {
                handleDownloadError(download, error);
            }
            
            @Override
            public void onSegmentFailed(int segmentIndex, String error) {
                Log.w(TAG, "Segment " + segmentIndex + " failed: " + error);
            }
        });
    }
    
    // Suppression/neutralisation de setupTorrentCallbacks car TorrentDownloader est désactivé
    // private void setupTorrentCallbacks(TorrentDownloader torrentDownloader, DownloadEntity download) {
    //     // Désactivé
    // }
    
    private void updateDownloadProgress(long downloadId, long downloadedBytes, long totalBytes, int progress, long speed) {
        try {
            downloadDao.updateProgress(downloadId, downloadedBytes, speed);
        } catch (Exception e) {
            Log.e(TAG, "Error updating download progress", e);
        }
    }
    
    private void updateSegmentProgress(long downloadId, int segmentIndex, long downloadedBytes, long totalBytes, int progress) {
        try {
            DownloadSegmentEntity segment = segmentDao.getByDownloadIdAndIndex(downloadId, segmentIndex);
            if (segment != null) {
                segment.setDownloadedBytes(downloadedBytes);
                segment.setLastActivity(System.currentTimeMillis());
                segmentDao.update(segment);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating segment progress", e);
        }
    }
    
    private void handleDownloadCompleted(long downloadId, String filePath, String checksum) {
        try {
            DownloadEntity download = downloadDao.getById(downloadId);
            if (download != null) {
                download.setStatus(DownloadStatus.COMPLETED);
                download.setCompletedAt(new java.util.Date());
                download.setChecksum(checksum);
                downloadDao.update(download);
                
                // Clean up
                activeDownloaders.remove(downloadId);
                currentConcurrentDownloads--;
                
                Log.d(TAG, "Download completed: " + download.getFilename());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling download completion", e);
        }
    }
    
    private void handleDownloadError(DownloadEntity download, String error) {
        try {
            download.setStatus(DownloadStatus.FAILED);
            downloadDao.update(download);
            
            // Clean up
            activeDownloaders.remove(download.getId());
            currentConcurrentDownloads--;
            
            Log.e(TAG, "Download failed: " + download.getFilename() + " - " + error);
        } catch (Exception e) {
            Log.e(TAG, "Error handling download error", e);
        }
    }
    
    public void pauseDownload(long downloadId) {
        try {
            MultiThreadDownloader downloader = activeDownloaders.get(downloadId);
            if (downloader != null) {
                downloader.pause();
            }
            
            // Retirer les cleanups sur activeTorrents
            // activeTorrents.remove(downloadId);
            
            DownloadEntity download = downloadDao.getById(downloadId);
            if (download != null) {
                download.setStatus(DownloadStatus.PAUSED);
                downloadDao.update(download);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error pausing download", e);
        }
    }
    
    public void resumeDownload(long downloadId) {
        try {
            DownloadEntity download = downloadDao.getById(downloadId);
            if (download != null) {
                download.setStatus(DownloadStatus.RESUMING);
                downloadDao.update(download);
                
                MultiThreadDownloader downloader = activeDownloaders.get(downloadId);
                if (downloader != null) {
                    downloader.resume();
                }
                
                // Retirer les cleanups sur activeTorrents
                // activeTorrents.remove(downloadId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resuming download", e);
        }
    }
    
    public void cancelDownload(long downloadId) {
        try {
            MultiThreadDownloader downloader = activeDownloaders.get(downloadId);
            if (downloader != null) {
                downloader.cancel();
                activeDownloaders.remove(downloadId);
            }
            
            // Retirer les cleanups sur activeTorrents
            // activeTorrents.remove(downloadId);
            
            DownloadEntity download = downloadDao.getById(downloadId);
            if (download != null) {
                download.setStatus(DownloadStatus.CANCELLED);
                downloadDao.update(download);
            }
            
            currentConcurrentDownloads--;
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling download", e);
        }
    }
    
    public void pauseAllDownloads() {
        for (MultiThreadDownloader downloader : activeDownloaders.values()) {
            downloader.pause();
        }
        
        // Retirer les cleanups sur activeTorrents
        // for (TorrentDownloader torrentDownloader : activeTorrents.values()) {
        //     torrentDownloader.pause();
        // }
    }
    
    public void resumeAllDownloads() {
        for (MultiThreadDownloader downloader : activeDownloaders.values()) {
            downloader.resume();
        }
        
        // Retirer les cleanups sur activeTorrents
        // for (TorrentDownloader torrentDownloader : activeTorrents.values()) {
        //     torrentDownloader.resume();
        // }
    }
    
    public boolean hasActiveDownloads() {
        return !activeDownloaders.isEmpty();
    }
    
    public boolean isDownloadableUrl(String url) {
        return VideoDetector.detectDownloadType(url) != DownloadType.HTTP_HTTPS || 
               url.matches(".*\\.(mp4|avi|mkv|mov|wmv|flv|webm|m4v|3gp|zip|rar|pdf|doc|docx|xls|xlsx|ppt|pptx)$");
    }
    
    public void shutdown() {
        // Cancel all active downloads
        for (MultiThreadDownloader downloader : activeDownloaders.values()) {
            downloader.cancel();
        }
        
        // Retirer les cleanups sur activeTorrents
        // for (TorrentDownloader torrentDownloader : activeTorrents.values()) {
        //     torrentDownloader.cancel();
        // }
        
        // Shutdown executors
        downloadExecutor.shutdown();
        queueExecutor.shutdown();
        
        Log.d(TAG, "DownloadManager shutdown completed");
    }
    
    private static class FileInfo {
        final String filename;
        final long fileSize;
        final String contentType;
        
        FileInfo(String filename, long fileSize, String contentType) {
            this.filename = filename;
            this.fileSize = fileSize;
            this.contentType = contentType;
        }
    }
} 