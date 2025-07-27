package rjv.mg.myidm.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;
import androidx.room.ColumnInfo;
import java.util.Date;

import rjv.mg.myidm.domain.model.DownloadStatus;
import rjv.mg.myidm.domain.model.DownloadType;

@Entity(
    tableName = "downloads",
    indices = {
        @Index(value = {"url"}),
        @Index(value = {"status"}),
        @Index(value = {"type"}),
        @Index(value = {"created_at"})
    }
)
public class DownloadEntity {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @ColumnInfo(name = "url")
    private String url;
    
    @ColumnInfo(name = "filename")
    private String filename;
    
    @ColumnInfo(name = "file_path")
    private String filePath;
    
    @ColumnInfo(name = "file_size")
    private long fileSize;
    
    @ColumnInfo(name = "downloaded_size")
    private long downloadedSize;
    
    @ColumnInfo(name = "status")
    private DownloadStatus status;
    
    @ColumnInfo(name = "type")
    private DownloadType type;
    
    @ColumnInfo(name = "content_type")
    private String contentType;
    
    @ColumnInfo(name = "checksum")
    private String checksum;
    
    @ColumnInfo(name = "checksum_algorithm")
    private String checksumAlgorithm;
    
    @ColumnInfo(name = "segment_count")
    private int segmentCount;
    
    @ColumnInfo(name = "max_concurrent_segments")
    private int maxConcurrentSegments;
    
    @ColumnInfo(name = "download_speed")
    private long downloadSpeed; // bytes per second
    
    @ColumnInfo(name = "average_speed")
    private long averageSpeed;
    
    @ColumnInfo(name = "priority")
    private int priority;
    
    @ColumnInfo(name = "is_premium")
    private boolean isPremium;
    
    @ColumnInfo(name = "is_encrypted")
    private boolean isEncrypted;
    
    @ColumnInfo(name = "encryption_key")
    private String encryptionKey;
    
    @ColumnInfo(name = "retry_count")
    private int retryCount;
    
    @ColumnInfo(name = "max_retries")
    private int maxRetries;
    
    @ColumnInfo(name = "created_at")
    private Date createdAt;
    
    @ColumnInfo(name = "started_at")
    private Date startedAt;
    
    @ColumnInfo(name = "completed_at")
    private Date completedAt;
    
    @ColumnInfo(name = "user_agent")
    private String userAgent;
    
    @ColumnInfo(name = "referer")
    private String referer;
    
    @ColumnInfo(name = "cookies")
    private String cookies;
    
    @ColumnInfo(name = "headers")
    private String headers;
    
    @ColumnInfo(name = "metadata")
    private String metadata; // JSON string for additional data
    
    // Constructors
    public DownloadEntity() {}
    
    @androidx.room.Ignore
    public DownloadEntity(String url, String filename, DownloadType type) {
        this.url = url;
        this.filename = filename;
        this.type = type;
        this.status = DownloadStatus.PENDING;
        this.createdAt = new Date();
        this.priority = 0;
        this.segmentCount = 1;
        this.maxConcurrentSegments = 1;
        this.maxRetries = 3;
        this.retryCount = 0;
    }
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    
    public long getDownloadedSize() { return downloadedSize; }
    public void setDownloadedSize(long downloadedSize) { this.downloadedSize = downloadedSize; }
    
    public DownloadStatus getStatus() { return status; }
    public void setStatus(DownloadStatus status) { this.status = status; }
    
    public DownloadType getType() { return type; }
    public void setType(DownloadType type) { this.type = type; }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    
    public String getChecksumAlgorithm() { return checksumAlgorithm; }
    public void setChecksumAlgorithm(String checksumAlgorithm) { this.checksumAlgorithm = checksumAlgorithm; }
    
    public int getSegmentCount() { return segmentCount; }
    public void setSegmentCount(int segmentCount) { this.segmentCount = segmentCount; }
    
    public int getMaxConcurrentSegments() { return maxConcurrentSegments; }
    public void setMaxConcurrentSegments(int maxConcurrentSegments) { this.maxConcurrentSegments = maxConcurrentSegments; }
    
    public long getDownloadSpeed() { return downloadSpeed; }
    public void setDownloadSpeed(long downloadSpeed) { this.downloadSpeed = downloadSpeed; }
    
    public long getAverageSpeed() { return averageSpeed; }
    public void setAverageSpeed(long averageSpeed) { this.averageSpeed = averageSpeed; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    
    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }
    
    public boolean isEncrypted() { return isEncrypted; }
    public void setEncrypted(boolean encrypted) { isEncrypted = encrypted; }
    
    public String getEncryptionKey() { return encryptionKey; }
    public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public Date getStartedAt() { return startedAt; }
    public void setStartedAt(Date startedAt) { this.startedAt = startedAt; }
    
    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getReferer() { return referer; }
    public void setReferer(String referer) { this.referer = referer; }
    
    public String getCookies() { return cookies; }
    public void setCookies(String cookies) { this.cookies = cookies; }
    
    public String getHeaders() { return headers; }
    public void setHeaders(String headers) { this.headers = headers; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    // Helper methods
    public int getProgress() {
        if (fileSize <= 0) return 0;
        return (int) ((downloadedSize * 100) / fileSize);
    }
    
    public boolean isCompleted() {
        return status == DownloadStatus.COMPLETED;
    }
    
    public boolean isFailed() {
        return status == DownloadStatus.FAILED;
    }
    
    public boolean canRetry() {
        return retryCount < maxRetries;
    }
} 