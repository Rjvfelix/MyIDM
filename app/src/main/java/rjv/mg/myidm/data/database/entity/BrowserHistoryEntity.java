package rjv.mg.myidm.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Index;
import androidx.room.ColumnInfo;
import java.util.Date;

@Entity(
    tableName = "browser_history",
    indices = {
        @Index(value = {"url"}),
        @Index(value = {"title"}),
        @Index(value = {"visited_at"})
    }
)
public class BrowserHistoryEntity {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @ColumnInfo(name = "url")
    private String url;
    
    @ColumnInfo(name = "title")
    private String title;
    
    @ColumnInfo(name = "favicon_url")
    private String faviconUrl;
    
    @ColumnInfo(name = "visited_at")
    private Date visitedAt;
    
    @ColumnInfo(name = "visit_count")
    private int visitCount;
    
    @ColumnInfo(name = "last_visit")
    private Date lastVisit;
    
    @ColumnInfo(name = "is_bookmarked")
    private boolean isBookmarked;
    
    @ColumnInfo(name = "metadata")
    private String metadata; // JSON string for additional data
    
    // Constructors
    public BrowserHistoryEntity() {}
    
    @androidx.room.Ignore
    public BrowserHistoryEntity(String url, String title) {
        this.url = url;
        this.title = title;
        this.visitedAt = new Date();
        this.lastVisit = new Date();
        this.visitCount = 1;
        this.isBookmarked = false;
    }
    
    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getFaviconUrl() { return faviconUrl; }
    public void setFaviconUrl(String faviconUrl) { this.faviconUrl = faviconUrl; }
    
    public Date getVisitedAt() { return visitedAt; }
    public void setVisitedAt(Date visitedAt) { this.visitedAt = visitedAt; }
    
    public int getVisitCount() { return visitCount; }
    public void setVisitCount(int visitCount) { this.visitCount = visitCount; }
    
    public Date getLastVisit() { return lastVisit; }
    public void setLastVisit(Date lastVisit) { this.lastVisit = lastVisit; }
    
    public boolean isBookmarked() { return isBookmarked; }
    public void setBookmarked(boolean bookmarked) { isBookmarked = bookmarked; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    // Helper methods
    public void incrementVisitCount() {
        this.visitCount++;
        this.lastVisit = new Date();
    }
    
    public String getDomain() {
        if (url == null || url.isEmpty()) return "";
        try {
            java.net.URI uri = new java.net.URI(url);
            return uri.getHost();
        } catch (Exception e) {
            return "";
        }
    }
} 