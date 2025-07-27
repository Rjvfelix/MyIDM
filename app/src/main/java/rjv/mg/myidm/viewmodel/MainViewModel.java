package rjv.mg.myidm.viewmodel;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;
import rjv.mg.myidm.data.database.DownloadDatabase;
import rjv.mg.myidm.data.database.dao.DownloadDao;
import rjv.mg.myidm.data.database.entity.DownloadEntity;
import rjv.mg.myidm.domain.downloader.DownloadManager;
import rjv.mg.myidm.domain.model.DownloadStatus;
import rjv.mg.myidm.service.DownloadService;

@HiltViewModel
public class MainViewModel extends AndroidViewModel {
    
    private static final String TAG = "MainViewModel";
    
    private final DownloadDao downloadDao;
    private final DownloadManager downloadManager;
    private final ExecutorService executorService;
    
    // LiveData
    private final MutableLiveData<Integer> activeDownloadsCount = new MutableLiveData<>(0);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> currentUrl = new MutableLiveData<>();
    
    @Inject
    public MainViewModel(@ApplicationContext Context context, DownloadDao downloadDao, DownloadManager downloadManager) {
        super((Application) context);
        this.downloadDao = downloadDao;
        this.downloadManager = downloadManager;
        this.executorService = Executors.newFixedThreadPool(4);
        
        initializeObservers();
    }
    
    private void initializeObservers() {
        // Observe active downloads count
        downloadDao.getByStatusesLive(List.of(DownloadStatus.DOWNLOADING, DownloadStatus.RESUMING))
            .observeForever(downloads -> {
                activeDownloadsCount.postValue(downloads.size());
            });
    }
    
    public void initializeServices() {
        executorService.execute(() -> {
            try {
                // Initialize download manager
                downloadManager.initialize();
                
                // Start download service if needed
                if (downloadManager.hasActiveDownloads()) {
                    DownloadService.startService(getApplication());
                }
                
                Log.d(TAG, "Services initialized successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error initializing services", e);
                error.postValue("Failed to initialize services: " + e.getMessage());
            }
        });
    }
    
    public void handleUrlIntent(String url) {
        currentUrl.postValue(url);
        
        // Check if it's a direct download link
        if (downloadManager.isDownloadableUrl(url)) {
            addDownloadFromUrl(url);
        }
    }
    
    public void addDownloadFromUrl(String url) {
        isLoading.postValue(true);
        
        executorService.execute(() -> {
            try {
                DownloadEntity download = downloadManager.createDownloadFromUrl(url);
                if (download != null) {
                    long downloadId = downloadDao.insert(download);
                    download.setId(downloadId);
                    
                    // Start download
                    downloadManager.startDownload(download);
                    
                    Log.d(TAG, "Download added successfully: " + url);
                } else {
                    error.postValue("Unable to create download from URL");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding download", e);
                error.postValue("Failed to add download: " + e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }
    
    public void retryFailedDownloads() {
        executorService.execute(() -> {
            try {
                List<DownloadEntity> failedDownloads = downloadDao.getByStatus(DownloadStatus.FAILED);
                for (DownloadEntity download : failedDownloads) {
                    if (download.canRetry()) {
                        download.setStatus(DownloadStatus.PENDING);
                        download.setRetryCount(0);
                        downloadDao.update(download);
                        downloadManager.startDownload(download);
                    }
                }
                Log.d(TAG, "Retried " + failedDownloads.size() + " failed downloads");
            } catch (Exception e) {
                Log.e(TAG, "Error retrying downloads", e);
                error.postValue("Failed to retry downloads: " + e.getMessage());
            }
        });
    }
    
    public void clearCompletedDownloads() {
        executorService.execute(() -> {
            try {
                List<DownloadEntity> completedDownloads = downloadDao.getByStatus(DownloadStatus.COMPLETED);
                for (DownloadEntity download : completedDownloads) {
                    downloadDao.delete(download);
                }
                Log.d(TAG, "Cleared " + completedDownloads.size() + " completed downloads");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing completed downloads", e);
                error.postValue("Failed to clear completed downloads: " + e.getMessage());
            }
        });
    }
    
    public void exportDownloads() {
        executorService.execute(() -> {
            try {
                List<DownloadEntity> downloads = downloadDao.getAll();
                // TODO: Implement export functionality
                Log.d(TAG, "Exporting " + downloads.size() + " downloads");
            } catch (Exception e) {
                Log.e(TAG, "Error exporting downloads", e);
                error.postValue("Failed to export downloads: " + e.getMessage());
            }
        });
    }
    
    public void importDownloads() {
        executorService.execute(() -> {
            try {
                // TODO: Implement import functionality
                Log.d(TAG, "Importing downloads");
            } catch (Exception e) {
                Log.e(TAG, "Error importing downloads", e);
                error.postValue("Failed to import downloads: " + e.getMessage());
            }
        });
    }
    
    public void pauseAllDownloads() {
        executorService.execute(() -> {
            try {
                downloadManager.pauseAllDownloads();
                Log.d(TAG, "All downloads paused");
            } catch (Exception e) {
                Log.e(TAG, "Error pausing downloads", e);
                error.postValue("Failed to pause downloads: " + e.getMessage());
            }
        });
    }
    
    public void resumeAllDownloads() {
        executorService.execute(() -> {
            try {
                downloadManager.resumeAllDownloads();
                Log.d(TAG, "All downloads resumed");
            } catch (Exception e) {
                Log.e(TAG, "Error resuming downloads", e);
                error.postValue("Failed to resume downloads: " + e.getMessage());
            }
        });
    }
    
    public void cancelDownload(long downloadId) {
        executorService.execute(() -> {
            try {
                downloadManager.cancelDownload(downloadId);
                Log.d(TAG, "Download cancelled: " + downloadId);
            } catch (Exception e) {
                Log.e(TAG, "Error cancelling download", e);
                error.postValue("Failed to cancel download: " + e.getMessage());
            }
        });
    }
    
    public void deleteDownload(long downloadId) {
        executorService.execute(() -> {
            try {
                DownloadEntity download = downloadDao.getById(downloadId);
                if (download != null) {
                    // Cancel if active
                    if (download.getStatus() == DownloadStatus.DOWNLOADING || 
                        download.getStatus() == DownloadStatus.RESUMING) {
                        downloadManager.cancelDownload(downloadId);
                    }
                    
                    // Delete from database
                    downloadDao.deleteById(downloadId);
                    Log.d(TAG, "Download deleted: " + downloadId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting download", e);
                error.postValue("Failed to delete download: " + e.getMessage());
            }
        });
    }
    
    public void updateDownloadPriority(long downloadId, int priority) {
        executorService.execute(() -> {
            try {
                downloadDao.updatePriority(downloadId, priority);
                Log.d(TAG, "Download priority updated: " + downloadId + " -> " + priority);
            } catch (Exception e) {
                Log.e(TAG, "Error updating download priority", e);
                error.postValue("Failed to update priority: " + e.getMessage());
            }
        });
    }
    
    public void clearError() {
        error.postValue(null);
    }
    
    public void cleanup() {
        executorService.shutdown();
    }
    
    // Getters for LiveData
    public LiveData<Integer> getActiveDownloadsCount() {
        return activeDownloadsCount;
    }
    
    public LiveData<String> getError() {
        return error;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getCurrentUrl() {
        return currentUrl;
    }
    
    public LiveData<List<DownloadEntity>> getAllDownloads() {
        return downloadDao.getAllLive();
    }
    
    public LiveData<List<DownloadEntity>> getDownloadsByStatus(DownloadStatus status) {
        return downloadDao.getByStatusLive(status);
    }
    
    public LiveData<List<DownloadEntity>> getDownloadsByStatuses(List<DownloadStatus> statuses) {
        return downloadDao.getByStatusesLive(statuses);
    }
} 