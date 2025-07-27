package rjv.mg.myidm.ui.download;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import rjv.mg.myidm.data.database.dao.DownloadDao;
import rjv.mg.myidm.data.database.entity.DownloadEntity;
import rjv.mg.myidm.domain.downloader.DownloadManager;
import rjv.mg.myidm.domain.model.DownloadStatus;

@HiltViewModel
public class DownloadsViewModel extends AndroidViewModel {
    
    private static final String TAG = "DownloadsViewModel";
    
    private final DownloadDao downloadDao;
    private final DownloadManager downloadManager;
    
    private final MutableLiveData<DownloadStatus> currentFilter = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    
    @Inject
    public DownloadsViewModel(Application application, DownloadDao downloadDao, DownloadManager downloadManager) {
        super(application);
        this.downloadDao = downloadDao;
        this.downloadManager = downloadManager;
    }
    
    public LiveData<List<DownloadEntity>> getDownloads() {
        return downloadDao.getAllLive();
    }
    
    public LiveData<List<DownloadEntity>> getDownloadsByStatus(DownloadStatus status) {
        if (status == null) {
            return downloadDao.getAllLive();
        }
        return downloadDao.getByStatusLive(status);
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getError() {
        return error;
    }
    
    public LiveData<Integer> getActiveDownloadsCount() {
        return downloadDao.getActiveDownloadsCountLive();
    }
    
    public void setFilter(DownloadStatus status) {
        currentFilter.setValue(status);
        Log.d(TAG, "Filter set to: " + (status != null ? status.name() : "ALL"));
    }
    
    public void refreshDownloads() {
        isLoading.setValue(true);
        // The LiveData will automatically update when the database changes
        isLoading.setValue(false);
    }
    
    public void pauseDownload(int position) {
        try {
            List<DownloadEntity> downloads = getCurrentDownloadsList();
            if (position >= 0 && position < downloads.size()) {
                DownloadEntity download = downloads.get(position);
                downloadManager.pauseDownload(download.getId());
                Log.d(TAG, "Paused download: " + download.getFilename());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error pausing download", e);
            error.setValue("Erreur lors de la mise en pause: " + e.getMessage());
        }
    }
    
    public void resumeDownload(int position) {
        try {
            List<DownloadEntity> downloads = getCurrentDownloadsList();
            if (position >= 0 && position < downloads.size()) {
                DownloadEntity download = downloads.get(position);
                downloadManager.resumeDownload(download.getId());
                Log.d(TAG, "Resumed download: " + download.getFilename());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resuming download", e);
            error.setValue("Erreur lors de la reprise: " + e.getMessage());
        }
    }
    
    public void cancelDownload(int position) {
        try {
            List<DownloadEntity> downloads = getCurrentDownloadsList();
            if (position >= 0 && position < downloads.size()) {
                DownloadEntity download = downloads.get(position);
                downloadManager.cancelDownload(download.getId());
                Log.d(TAG, "Cancelled download: " + download.getFilename());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling download", e);
            error.setValue("Erreur lors de l'annulation: " + e.getMessage());
        }
    }
    
    public void addDownload(DownloadEntity download) {
        try {
            long id = downloadDao.insert(download);
            download.setId(id);
            downloadManager.startDownload(download);
            Log.d(TAG, "Added and started download: " + download.getFilename());
        } catch (Exception e) {
            Log.e(TAG, "Error adding download", e);
            error.setValue("Erreur lors de l'ajout: " + e.getMessage());
        }
    }
    
    public void deleteDownload(int position) {
        try {
            List<DownloadEntity> downloads = getCurrentDownloadsList();
            if (position >= 0 && position < downloads.size()) {
                DownloadEntity download = downloads.get(position);
                downloadDao.delete(download);
                Log.d(TAG, "Deleted download: " + download.getFilename());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting download", e);
            error.setValue("Erreur lors de la suppression: " + e.getMessage());
        }
    }
    
    public void retryFailedDownload(int position) {
        try {
            List<DownloadEntity> downloads = getCurrentDownloadsList();
            if (position >= 0 && position < downloads.size()) {
                DownloadEntity download = downloads.get(position);
                if (download.getStatus() == DownloadStatus.FAILED && download.canRetry()) {
                    download.setStatus(DownloadStatus.PENDING);
                    download.setRetryCount(0);
                    downloadDao.update(download);
                    downloadManager.startDownload(download);
                    Log.d(TAG, "Retried download: " + download.getFilename());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrying download", e);
            error.setValue("Erreur lors de la nouvelle tentative: " + e.getMessage());
        }
    }
    
    public void clearCompletedDownloads() {
        try {
            List<DownloadEntity> completedDownloads = downloadDao.getByStatus(DownloadStatus.COMPLETED);
            for (DownloadEntity download : completedDownloads) {
                downloadDao.delete(download);
            }
            Log.d(TAG, "Cleared " + completedDownloads.size() + " completed downloads");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing completed downloads", e);
            error.setValue("Erreur lors de l'effacement: " + e.getMessage());
        }
    }
    
    public void pauseAllDownloads() {
        try {
            downloadManager.pauseAllDownloads();
            Log.d(TAG, "Paused all downloads");
        } catch (Exception e) {
            Log.e(TAG, "Error pausing all downloads", e);
            error.setValue("Erreur lors de la mise en pause: " + e.getMessage());
        }
    }
    
    public void resumeAllDownloads() {
        try {
            downloadManager.resumeAllDownloads();
            Log.d(TAG, "Resumed all downloads");
        } catch (Exception e) {
            Log.e(TAG, "Error resuming all downloads", e);
            error.setValue("Erreur lors de la reprise: " + e.getMessage());
        }
    }
    
    public void clearError() {
        error.setValue(null);
    }
    
    private List<DownloadEntity> getCurrentDownloadsList() {
        // This is a simplified approach. In a real app, you might want to maintain
        // a separate list based on the current filter
        return downloadDao.getAll();
    }
    
    public DownloadEntity getDownloadAtPosition(int position) {
        List<DownloadEntity> downloads = getCurrentDownloadsList();
        if (position >= 0 && position < downloads.size()) {
            return downloads.get(position);
        }
        return null;
    }
    
    public boolean hasActiveDownloads() {
        return downloadManager.hasActiveDownloads();
    }
    
    public void exportDownloads() {
        try {
            List<DownloadEntity> downloads = downloadDao.getAll();
            // TODO: Implement export functionality
            Log.d(TAG, "Exporting " + downloads.size() + " downloads");
        } catch (Exception e) {
            Log.e(TAG, "Error exporting downloads", e);
            error.setValue("Erreur lors de l'export: " + e.getMessage());
        }
    }
    
    public void importDownloads() {
        try {
            // TODO: Implement import functionality
            Log.d(TAG, "Importing downloads");
        } catch (Exception e) {
            Log.e(TAG, "Error importing downloads", e);
            error.setValue("Erreur lors de l'import: " + e.getMessage());
        }
    }
} 