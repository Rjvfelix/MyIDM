package rjv.mg.myidm.domain.downloader;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

import rjv.mg.myidm.data.database.entity.DownloadEntity;
import rjv.mg.myidm.data.database.entity.DownloadSegmentEntity;
import rjv.mg.myidm.domain.model.DownloadStatus;
import rjv.mg.myidm.domain.model.SegmentStatus;

public class MultiThreadDownloader {
    private static final String TAG = "MultiThreadDownloader";
    
    // Configuration
    private static final int DEFAULT_SEGMENT_COUNT = 8;
    private static final int MAX_SEGMENT_COUNT = 32;
    private static final int MIN_SEGMENT_SIZE = 1024 * 1024; // 1MB
    private static final int BUFFER_SIZE = 8192;
    private static final int CONNECTION_TIMEOUT = 30000;
    private static final int READ_TIMEOUT = 60000;
    private static final int MAX_RETRIES = 3;
    
    // Thread pool for download segments
    private final ExecutorService segmentExecutor;
    private final ExecutorService mergeExecutor;
    
    // Download state
    private final DownloadEntity download;
    private final List<DownloadSegmentEntity> segments;
    private final AtomicLong totalDownloadedBytes;
    private final AtomicInteger completedSegments;
    private final AtomicInteger failedSegments;
    
    // Callbacks
    private DownloadProgressCallback progressCallback;
    private DownloadCompletionCallback completionCallback;
    private DownloadErrorCallback errorCallback;
    
    // Control flags
    private volatile boolean isPaused;
    private volatile boolean isCancelled;
    private volatile boolean isCompleted;
    
    // Performance tracking
    private final Map<Integer, Long> segmentSpeeds;
    private final long startTime;
    
    public interface DownloadProgressCallback {
        void onProgress(long downloadedBytes, long totalBytes, int progress, long speed);
        void onSegmentProgress(int segmentIndex, long downloadedBytes, long totalBytes, int progress);
    }
    
    public interface DownloadCompletionCallback {
        void onCompleted(String filePath, String checksum);
        void onMerged(String filePath);
    }
    
    public interface DownloadErrorCallback {
        void onError(String error, int segmentIndex);
        void onSegmentFailed(int segmentIndex, String error);
    }
    
    public MultiThreadDownloader(Context context, DownloadEntity download) {
        this.download = download;
        this.segments = new ArrayList<>();
        this.totalDownloadedBytes = new AtomicLong(0);
        this.completedSegments = new AtomicInteger(0);
        this.failedSegments = new AtomicInteger(0);
        this.segmentSpeeds = new ConcurrentHashMap<>();
        this.startTime = System.currentTimeMillis();
        
        // Create thread pools
        int maxConcurrentSegments = Math.min(download.getMaxConcurrentSegments(), MAX_SEGMENT_COUNT);
        this.segmentExecutor = Executors.newFixedThreadPool(maxConcurrentSegments, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "DownloadSegment-" + counter.getAndIncrement());
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            }
        });
        
        this.mergeExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "DownloadMerger");
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            }
        });
        
        initializeSegments();
    }
    
    private void initializeSegments() {
        long fileSize = download.getFileSize();
        int segmentCount = calculateOptimalSegmentCount(fileSize);
        
        long segmentSize = fileSize / segmentCount;
        long remainingBytes = fileSize % segmentCount;
        
        for (int i = 0; i < segmentCount; i++) {
            long startByte = i * segmentSize;
            long endByte = startByte + segmentSize - 1;
            
            // Add remaining bytes to the last segment
            if (i == segmentCount - 1) {
                endByte += remainingBytes;
            }
            
            DownloadSegmentEntity segment = new DownloadSegmentEntity(
                download.getId(), i, startByte, endByte
            );
            segments.add(segment);
        }
        
        download.setSegmentCount(segmentCount);
    }
    
    private int calculateOptimalSegmentCount(long fileSize) {
        if (fileSize <= 0) return DEFAULT_SEGMENT_COUNT;
        
        // Calculate optimal segment count based on file size
        int optimalCount = (int) Math.min(
            Math.max(fileSize / MIN_SEGMENT_SIZE, DEFAULT_SEGMENT_COUNT),
            MAX_SEGMENT_COUNT
        );
        
        // Ensure it doesn't exceed max concurrent segments
        return Math.min(optimalCount, download.getMaxConcurrentSegments());
    }
    
    public void start() {
        if (isCompleted || isCancelled) return;
        
        Log.d(TAG, "Starting download: " + download.getUrl() + " with " + segments.size() + " segments");
        
        // Submit all segments for download
        for (DownloadSegmentEntity segment : segments) {
            segmentExecutor.submit(() -> downloadSegment(segment));
        }
    }
    
    private void downloadSegment(DownloadSegmentEntity segment) {
        if (isCancelled || isPaused) return;
        
        int retryCount = 0;
        boolean success = false;
        
        while (retryCount < MAX_RETRIES && !success && !isCancelled) {
            try {
                success = downloadSegmentInternal(segment);
                if (success) {
                    segment.setStatus(SegmentStatus.COMPLETED);
                    completedSegments.incrementAndGet();
                    checkCompletion();
                } else {
                    retryCount++;
                    segment.setRetryCount(retryCount);
                    if (retryCount < MAX_RETRIES) {
                        Thread.sleep(1000 * retryCount); // Exponential backoff
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error downloading segment " + segment.getSegmentIndex(), e);
                retryCount++;
                segment.setRetryCount(retryCount);
                segment.setErrorMessage(e.getMessage());
                
                if (retryCount >= MAX_RETRIES) {
                    segment.setStatus(SegmentStatus.FAILED);
                    failedSegments.incrementAndGet();
                    if (errorCallback != null) {
                        errorCallback.onSegmentFailed(segment.getSegmentIndex(), e.getMessage());
                    }
                }
            }
        }
    }
    
    private boolean downloadSegmentInternal(DownloadSegmentEntity segment) throws IOException {
        URL url = new URL(download.getUrl());
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECTION_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            
            // Set range header for partial download
            String rangeHeader = "bytes=" + segment.getStartByte() + "-" + segment.getEndByte();
            connection.setRequestProperty("Range", rangeHeader);
            
            // Set additional headers
            if (download.getUserAgent() != null) {
                connection.setRequestProperty("User-Agent", download.getUserAgent());
            }
            if (download.getReferer() != null) {
                connection.setRequestProperty("Referer", download.getReferer());
            }
            if (download.getCookies() != null) {
                connection.setRequestProperty("Cookie", download.getCookies());
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode);
            }
            
            // Create temporary file for segment
            File tempFile = createTempFile(segment);
            segment.setTempFilePath(tempFile.getAbsolutePath());
            
            inputStream = connection.getInputStream();
            outputStream = new FileOutputStream(tempFile);
            
            byte[] buffer = new byte[BUFFER_SIZE];
            long downloadedBytes = 0;
            long lastSpeedUpdate = System.currentTimeMillis();
            long lastSpeedBytes = 0;
            
            while (downloadedBytes < segment.getSegmentSize() && !isCancelled && !isPaused) {
                int bytesRead = inputStream.read(buffer);
                if (bytesRead == -1) break;
                
                outputStream.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;
                
                // Update segment progress
                segment.setDownloadedBytes(downloadedBytes);
                segment.setLastActivity(System.currentTimeMillis());
                
                // Calculate speed
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastSpeedUpdate >= 1000) { // Update speed every second
                    long speed = (downloadedBytes - lastSpeedBytes) * 1000 / (currentTime - lastSpeedUpdate);
                    segment.setDownloadSpeed(speed);
                    segmentSpeeds.put(segment.getSegmentIndex(), speed);
                    lastSpeedUpdate = currentTime;
                    lastSpeedBytes = downloadedBytes;
                }
                
                // Update total progress
                long totalBytes = totalDownloadedBytes.addAndGet(bytesRead);
                if (progressCallback != null) {
                    progressCallback.onProgress(totalBytes, download.getFileSize(), 
                        (int) (totalBytes * 100 / download.getFileSize()), calculateTotalSpeed());
                    progressCallback.onSegmentProgress(segment.getSegmentIndex(), 
                        downloadedBytes, segment.getSegmentSize(), segment.getProgress());
                }
            }
            
            outputStream.flush();
            
            // Verify segment integrity
            if (downloadedBytes == segment.getSegmentSize()) {
                String checksum = calculateChecksum(tempFile);
                segment.setChecksum(checksum);
                return true;
            } else {
                return false;
            }
            
        } finally {
            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException e) { }
            }
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException e) { }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    private File createTempFile(DownloadSegmentEntity segment) throws IOException {
        File tempDir = new File(download.getFilePath()).getParentFile();
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        
        String tempFileName = download.getFilename() + ".part" + segment.getSegmentIndex();
        return new File(tempDir, tempFileName);
    }
    
    private String calculateChecksum(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5 algorithm not available", e);
        }
    }
    
    private long calculateTotalSpeed() {
        long totalSpeed = 0;
        for (Long speed : segmentSpeeds.values()) {
            totalSpeed += speed;
        }
        return totalSpeed;
    }
    
    private void checkCompletion() {
        int totalSegments = segments.size();
        int completed = completedSegments.get();
        int failed = failedSegments.get();
        
        if (completed + failed == totalSegments) {
            if (failed == 0) {
                // All segments completed successfully, merge files
                mergeExecutor.submit(this::mergeSegments);
            } else {
                // Some segments failed
                if (errorCallback != null) {
                    errorCallback.onError("Some segments failed to download", -1);
                }
            }
        }
    }
    
    private void mergeSegments() {
        try {
            File outputFile = new File(download.getFilePath());
            File parentDir = outputFile.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                for (DownloadSegmentEntity segment : segments) {
                    if (segment.getStatus() == SegmentStatus.COMPLETED) {
                        File tempFile = new File(segment.getTempFilePath());
                        if (tempFile.exists()) {
                            try (FileInputStream inputStream = new FileInputStream(tempFile)) {
                                byte[] buffer = new byte[BUFFER_SIZE];
                                int bytesRead;
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, bytesRead);
                                }
                            }
                        }
                    }
                }
            }
            
            // Calculate final checksum
            String finalChecksum = calculateChecksum(outputFile);
            
            // Clean up temporary files
            cleanupTempFiles();
            
            isCompleted = true;
            
            if (completionCallback != null) {
                completionCallback.onCompleted(outputFile.getAbsolutePath(), finalChecksum);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error merging segments", e);
            if (errorCallback != null) {
                errorCallback.onError("Failed to merge segments: " + e.getMessage(), -1);
            }
        }
    }
    
    private void cleanupTempFiles() {
        for (DownloadSegmentEntity segment : segments) {
            if (segment.getTempFilePath() != null) {
                File tempFile = new File(segment.getTempFilePath());
                if (tempFile.exists()) {
                    tempFile.delete();
                }
            }
        }
    }
    
    public void pause() {
        isPaused = true;
        Log.d(TAG, "Download paused");
    }
    
    public void resume() {
        isPaused = false;
        Log.d(TAG, "Download resumed");
    }
    
    public void cancel() {
        isCancelled = true;
        isPaused = false;
        Log.d(TAG, "Download cancelled");
        
        // Shutdown executors
        segmentExecutor.shutdownNow();
        mergeExecutor.shutdownNow();
        
        // Clean up temporary files
        cleanupTempFiles();
    }
    
    public void setProgressCallback(DownloadProgressCallback callback) {
        this.progressCallback = callback;
    }
    
    public void setCompletionCallback(DownloadCompletionCallback callback) {
        this.completionCallback = callback;
    }
    
    public void setErrorCallback(DownloadErrorCallback callback) {
        this.errorCallback = callback;
    }
    
    public boolean isCompleted() {
        return isCompleted;
    }
    
    public boolean isCancelled() {
        return isCancelled;
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public List<DownloadSegmentEntity> getSegments() {
        return new ArrayList<>(segments);
    }
    
    public long getDownloadedBytes() {
        return totalDownloadedBytes.get();
    }
    
    public int getProgress() {
        if (download.getFileSize() <= 0) return 0;
        return (int) (totalDownloadedBytes.get() * 100 / download.getFileSize());
    }
} 