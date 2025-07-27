package rjv.mg.myidm.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

import rjv.mg.myidm.data.database.entity.DownloadSegmentEntity;
import rjv.mg.myidm.domain.model.SegmentStatus;

@Dao
public interface DownloadSegmentDao {
    
    // Insert operations
    @Insert
    long insert(DownloadSegmentEntity segment);
    
    @Insert
    List<Long> insertAll(List<DownloadSegmentEntity> segments);
    
    // Update operations
    @Update
    void update(DownloadSegmentEntity segment);
    
    @Update
    void updateAll(List<DownloadSegmentEntity> segments);
    
    // Delete operations
    @Delete
    void delete(DownloadSegmentEntity segment);
    
    @Delete
    void deleteAll(List<DownloadSegmentEntity> segments);
    
    @Query("DELETE FROM download_segments WHERE download_id = :downloadId")
    void deleteByDownloadId(long downloadId);
    
    // Query operations
    @Query("SELECT * FROM download_segments WHERE id = :segmentId")
    DownloadSegmentEntity getById(long segmentId);
    
    @Query("SELECT * FROM download_segments WHERE download_id = :downloadId ORDER BY segment_index ASC")
    List<DownloadSegmentEntity> getByDownloadId(long downloadId);
    
    @Query("SELECT * FROM download_segments WHERE download_id = :downloadId ORDER BY segment_index ASC")
    LiveData<List<DownloadSegmentEntity>> getByDownloadIdLive(long downloadId);
    
    @Query("SELECT * FROM download_segments WHERE download_id = :downloadId AND status = :status ORDER BY segment_index ASC")
    List<DownloadSegmentEntity> getByDownloadIdAndStatus(long downloadId, SegmentStatus status);
    
    @Query("SELECT * FROM download_segments WHERE status = :status ORDER BY last_activity ASC")
    List<DownloadSegmentEntity> getByStatus(SegmentStatus status);
    
    @Query("SELECT * FROM download_segments WHERE status = :status ORDER BY last_activity ASC")
    LiveData<List<DownloadSegmentEntity>> getByStatusLive(SegmentStatus status);
    
    @Query("SELECT * FROM download_segments WHERE download_id = :downloadId AND segment_index = :segmentIndex")
    DownloadSegmentEntity getByDownloadIdAndIndex(long downloadId, int segmentIndex);
    
    // Statistics queries
    @Query("SELECT COUNT(*) FROM download_segments WHERE download_id = :downloadId")
    int getCountByDownloadId(long downloadId);
    
    @Query("SELECT COUNT(*) FROM download_segments WHERE download_id = :downloadId AND status = :status")
    int getCountByDownloadIdAndStatus(long downloadId, SegmentStatus status);
    
    @Query("SELECT SUM(downloaded_bytes) FROM download_segments WHERE download_id = :downloadId")
    long getTotalDownloadedBytes(long downloadId);
    
    @Query("SELECT SUM(downloaded_bytes) FROM download_segments WHERE download_id = :downloadId AND status = :status")
    long getTotalDownloadedBytesByStatus(long downloadId, SegmentStatus status);
    
    // Progress queries
    @Query("SELECT AVG(download_speed) FROM download_segments WHERE download_id = :downloadId AND status = 'DOWNLOADING'")
    long getAverageSpeed(long downloadId);
    
    @Query("SELECT SUM(download_speed) FROM download_segments WHERE download_id = :downloadId AND status = 'DOWNLOADING'")
    long getTotalSpeed(long downloadId);
    
    // Stalled segments
    @Query("SELECT * FROM download_segments WHERE last_activity < :timestamp AND status = 'DOWNLOADING'")
    List<DownloadSegmentEntity> getStalledSegments(long timestamp);
    
    // Batch operations
    @Query("UPDATE download_segments SET status = :newStatus WHERE download_id = :downloadId")
    void updateStatusByDownloadId(long downloadId, SegmentStatus newStatus);
    
    @Query("UPDATE download_segments SET status = :newStatus WHERE id IN (:segmentIds)")
    void updateStatusByIds(List<Long> segmentIds, SegmentStatus newStatus);
    
    @Query("UPDATE download_segments SET downloaded_bytes = :downloadedBytes, download_speed = :speed, last_activity = :timestamp WHERE id = :segmentId")
    void updateProgress(long segmentId, long downloadedBytes, long speed, long timestamp);
    
    @Query("UPDATE download_segments SET retry_count = retry_count + 1 WHERE id = :segmentId")
    void incrementRetryCount(long segmentId);
    
    @Query("UPDATE download_segments SET error_message = :errorMessage WHERE id = :segmentId")
    void updateErrorMessage(long segmentId, String errorMessage);
    
    // Cleanup
    @Query("DELETE FROM download_segments WHERE download_id = :downloadId AND status = 'COMPLETED'")
    void deleteCompletedSegments(long downloadId);
} 