package rjv.mg.myidm.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

import rjv.mg.myidm.data.database.entity.DownloadEntity;
import rjv.mg.myidm.domain.model.DownloadStatus;
import rjv.mg.myidm.domain.model.DownloadType;

@Dao
public interface DownloadDao {
    
    // Insert operations
    @Insert
    long insert(DownloadEntity download);
    
    @Insert
    List<Long> insertAll(List<DownloadEntity> downloads);
    
    // Update operations
    @Update
    void update(DownloadEntity download);
    
    @Update
    void updateAll(List<DownloadEntity> downloads);
    
    // Delete operations
    @Delete
    void delete(DownloadEntity download);
    
    @Delete
    void deleteAll(List<DownloadEntity> downloads);
    
    @Query("DELETE FROM downloads WHERE id = :downloadId")
    void deleteById(long downloadId);
    
    @Query("DELETE FROM downloads WHERE status = :status")
    void deleteByStatus(DownloadStatus status);
    
    // Query operations
    @Query("SELECT * FROM downloads WHERE id = :downloadId")
    DownloadEntity getById(long downloadId);
    
    @Query("SELECT * FROM downloads WHERE id = :downloadId")
    LiveData<DownloadEntity> getByIdLive(long downloadId);
    
    @Query("SELECT * FROM downloads WHERE url = :url")
    DownloadEntity getByUrl(String url);
    
    @Query("SELECT * FROM downloads ORDER BY created_at DESC")
    List<DownloadEntity> getAll();
    
    @Query("SELECT * FROM downloads ORDER BY created_at DESC")
    LiveData<List<DownloadEntity>> getAllLive();
    
    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY created_at DESC")
    List<DownloadEntity> getByStatus(DownloadStatus status);
    
    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY created_at DESC")
    LiveData<List<DownloadEntity>> getByStatusLive(DownloadStatus status);
    
    @Query("SELECT * FROM downloads WHERE type = :type ORDER BY created_at DESC")
    List<DownloadEntity> getByType(DownloadType type);
    
    @Query("SELECT * FROM downloads WHERE type = :type ORDER BY created_at DESC")
    LiveData<List<DownloadEntity>> getByTypeLive(DownloadType type);
    
    @Query("SELECT * FROM downloads WHERE status IN (:statuses) ORDER BY priority DESC, created_at ASC")
    List<DownloadEntity> getByStatuses(List<DownloadStatus> statuses);
    
    @Query("SELECT * FROM downloads WHERE status IN (:statuses) ORDER BY priority DESC, created_at ASC")
    LiveData<List<DownloadEntity>> getByStatusesLive(List<DownloadStatus> statuses);
    
    // Statistics queries
    @Query("SELECT COUNT(*) FROM downloads WHERE status = :status")
    int getCountByStatus(DownloadStatus status);
    
    @Query("SELECT COUNT(*) FROM downloads WHERE type = :type")
    int getCountByType(DownloadType type);
    
    @Query("SELECT SUM(file_size) FROM downloads WHERE status = :status")
    long getTotalSizeByStatus(DownloadStatus status);
    
    @Query("SELECT SUM(downloaded_size) FROM downloads WHERE status = :status")
    long getTotalDownloadedSizeByStatus(DownloadStatus status);
    
    // Search queries
    @Query("SELECT * FROM downloads WHERE filename LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY created_at DESC")
    List<DownloadEntity> search(String query);
    
    @Query("SELECT * FROM downloads WHERE filename LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY created_at DESC")
    LiveData<List<DownloadEntity>> searchLive(String query);
    
    // Premium queries
    @Query("SELECT * FROM downloads WHERE is_premium = :isPremium ORDER BY created_at DESC")
    List<DownloadEntity> getByPremiumStatus(boolean isPremium);
    
    @Query("SELECT * FROM downloads WHERE is_premium = :isPremium ORDER BY created_at DESC")
    LiveData<List<DownloadEntity>> getByPremiumStatusLive(boolean isPremium);
    
    // Queue management
    @Query("SELECT * FROM downloads WHERE status IN ('PENDING', 'QUEUED') ORDER BY priority DESC, created_at ASC LIMIT :limit")
    List<DownloadEntity> getQueuedDownloads(int limit);
    
    @Query("SELECT COUNT(*) FROM downloads WHERE status IN ('DOWNLOADING', 'RESUMING')")
    int getActiveDownloadCount();
    
    @Query("SELECT COUNT(*) FROM downloads WHERE status IN ('DOWNLOADING', 'RESUMING')")
    LiveData<Integer> getActiveDownloadsCountLive();
    
    // Cleanup queries
    @Query("DELETE FROM downloads WHERE status = 'COMPLETED' AND completed_at < :beforeDate")
    void deleteCompletedBefore(java.util.Date beforeDate);
    
    @Query("DELETE FROM downloads WHERE status = 'FAILED' AND created_at < :beforeDate")
    void deleteFailedBefore(java.util.Date beforeDate);
    
    // Batch operations
    @Query("UPDATE downloads SET status = :newStatus WHERE id IN (:downloadIds)")
    void updateStatusByIds(List<Long> downloadIds, DownloadStatus newStatus);
    
    @Query("UPDATE downloads SET priority = :priority WHERE id = :downloadId")
    void updatePriority(long downloadId, int priority);
    
    @Query("UPDATE downloads SET downloaded_size = :downloadedSize, download_speed = :speed WHERE id = :downloadId")
    void updateProgress(long downloadId, long downloadedSize, long speed);
} 