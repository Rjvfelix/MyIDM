package rjv.mg.myidm.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.ColumnInfo;

import rjv.mg.myidm.domain.model.SegmentStatus;

@Entity(
    tableName = "download_segments",
    foreignKeys = @ForeignKey(
        entity = DownloadEntity.class,
        parentColumns = "id",
        childColumns = "download_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = {"download_id"}),
        @Index(value = {"status"}),
        @Index(value = {"segment_index"})
    }
)
public class DownloadSegmentEntity {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @ColumnInfo(name = "download_id")
    private long downloadId;
    
    @ColumnInfo(name = "segment_index")
    private int segmentIndex;
    
    @ColumnInfo(name = "start_byte")
    private long startByte;
    
    @ColumnInfo(name = "end_byte")
    private long endByte;
    
    @ColumnInfo(name = "downloaded_bytes")
    private long downloadedBytes;
    
    @ColumnInfo(name = "status")
    private SegmentStatus status;
    
    @ColumnInfo(name = "temp_file_path")
    private String tempFilePath;
    
    @ColumnInfo(name = "checksum")
    private String checksum;
    
    @ColumnInfo(name = "retry_count")
    private int retryCount;
    
    @ColumnInfo(name = "max_retries")
    private int maxRetries;
    
    @ColumnInfo(name = "download_speed")
    private long downloadSpeed;
    
    @ColumnInfo(name = "last_activity")
    private long lastActivity;
    
    @ColumnInfo(name = "error_message")
    private String errorMessage;
    
    // Constructors
    public DownloadSegmentEntity() {}
    
    @androidx.room.Ignore
    public DownloadSegmentEntity(long downloadId, int segmentIndex, long startByte, long endByte) {
        this.downloadId = downloadId;
        this.segmentIndex = segmentIndex;
        this.startByte = startByte;
        this.endByte = endByte;
        this.downloadedBytes = 0;
        this.status = SegmentStatus.PENDING;
        this.retryCount = 0;
        this.maxRetries = 3;
        this.lastActivity = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getDownloadId() { return downloadId; }
    public void setDownloadId(long downloadId) { this.downloadId = downloadId; }
    
    public int getSegmentIndex() { return segmentIndex; }
    public void setSegmentIndex(int segmentIndex) { this.segmentIndex = segmentIndex; }
    
    public long getStartByte() { return startByte; }
    public void setStartByte(long startByte) { this.startByte = startByte; }
    
    public long getEndByte() { return endByte; }
    public void setEndByte(long endByte) { this.endByte = endByte; }
    
    public long getDownloadedBytes() { return downloadedBytes; }
    public void setDownloadedBytes(long downloadedBytes) { this.downloadedBytes = downloadedBytes; }
    
    public SegmentStatus getStatus() { return status; }
    public void setStatus(SegmentStatus status) { this.status = status; }
    
    public String getTempFilePath() { return tempFilePath; }
    public void setTempFilePath(String tempFilePath) { this.tempFilePath = tempFilePath; }
    
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    
    public long getDownloadSpeed() { return downloadSpeed; }
    public void setDownloadSpeed(long downloadSpeed) { this.downloadSpeed = downloadSpeed; }
    
    public long getLastActivity() { return lastActivity; }
    public void setLastActivity(long lastActivity) { this.lastActivity = lastActivity; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    // Helper methods
    public long getSegmentSize() {
        return endByte - startByte + 1;
    }
    
    public int getProgress() {
        long segmentSize = getSegmentSize();
        if (segmentSize <= 0) return 0;
        return (int) ((downloadedBytes * 100) / segmentSize);
    }
    
    public boolean isCompleted() {
        return status == SegmentStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return status == SegmentStatus.FAILED;
    }
    
    public boolean canRetry() {
        return retryCount < maxRetries;
    }
    
    public boolean isStalled() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastActivity) > 30000; // 30 seconds
    }
    
    public long getRemainingBytes() {
        return getSegmentSize() - downloadedBytes;
    }
} 