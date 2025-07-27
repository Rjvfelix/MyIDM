package rjv.mg.myidm.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;
import java.util.Date;

import rjv.mg.myidm.data.database.entity.BrowserHistoryEntity;

@Dao
public interface BrowserHistoryDao {
    
    // Insert operations
    @Insert
    long insert(BrowserHistoryEntity history);
    
    @Insert
    List<Long> insertAll(List<BrowserHistoryEntity> histories);
    
    // Update operations
    @Update
    void update(BrowserHistoryEntity history);
    
    @Update
    void updateAll(List<BrowserHistoryEntity> histories);
    
    // Delete operations
    @Delete
    void delete(BrowserHistoryEntity history);
    
    @Delete
    void deleteAll(List<BrowserHistoryEntity> histories);
    
    @Query("DELETE FROM browser_history WHERE id = :historyId")
    void deleteById(long historyId);
    
    @Query("DELETE FROM browser_history WHERE url = :url")
    void deleteByUrl(String url);
    
    // Query operations
    @Query("SELECT * FROM browser_history WHERE id = :historyId")
    BrowserHistoryEntity getById(long historyId);
    
    @Query("SELECT * FROM browser_history WHERE url = :url")
    BrowserHistoryEntity getByUrl(String url);
    
    @Query("SELECT * FROM browser_history ORDER BY visited_at DESC")
    List<BrowserHistoryEntity> getAll();
    
    @Query("SELECT * FROM browser_history ORDER BY visited_at DESC")
    LiveData<List<BrowserHistoryEntity>> getAllLive();
    
    @Query("SELECT * FROM browser_history WHERE is_bookmarked = 1 ORDER BY visited_at DESC")
    List<BrowserHistoryEntity> getBookmarks();
    
    @Query("SELECT * FROM browser_history WHERE is_bookmarked = 1 ORDER BY visited_at DESC")
    LiveData<List<BrowserHistoryEntity>> getBookmarksLive();
    
    @Query("SELECT * FROM browser_history WHERE visited_at >= :sinceDate ORDER BY visited_at DESC")
    List<BrowserHistoryEntity> getSince(Date sinceDate);
    
    @Query("SELECT * FROM browser_history WHERE visited_at >= :sinceDate ORDER BY visited_at DESC")
    LiveData<List<BrowserHistoryEntity>> getSinceLive(Date sinceDate);
    
    // Search operations
    @Query("SELECT * FROM browser_history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visited_at DESC")
    List<BrowserHistoryEntity> search(String query);
    
    @Query("SELECT * FROM browser_history WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY visited_at DESC")
    LiveData<List<BrowserHistoryEntity>> searchLive(String query);
    
    @Query("SELECT * FROM browser_history WHERE url LIKE '%' || :domain || '%' ORDER BY visited_at DESC")
    List<BrowserHistoryEntity> getByDomain(String domain);
    
    // Statistics
    @Query("SELECT COUNT(*) FROM browser_history")
    int getTotalCount();
    
    @Query("SELECT COUNT(*) FROM browser_history WHERE is_bookmarked = 1")
    int getBookmarkCount();
    
    @Query("SELECT COUNT(*) FROM browser_history WHERE visited_at >= :sinceDate")
    int getCountSince(Date sinceDate);
    
    // Most visited
    @Query("SELECT * FROM browser_history ORDER BY visit_count DESC, last_visit DESC LIMIT :limit")
    List<BrowserHistoryEntity> getMostVisited(int limit);
    
    @Query("SELECT * FROM browser_history ORDER BY visit_count DESC, last_visit DESC LIMIT :limit")
    LiveData<List<BrowserHistoryEntity>> getMostVisitedLive(int limit);
    
    // Recent visits
    @Query("SELECT * FROM browser_history ORDER BY last_visit DESC LIMIT :limit")
    List<BrowserHistoryEntity> getRecentVisits(int limit);
    
    @Query("SELECT * FROM browser_history ORDER BY last_visit DESC LIMIT :limit")
    LiveData<List<BrowserHistoryEntity>> getRecentVisitsLive(int limit);
    
    // Update operations
    @Query("UPDATE browser_history SET visit_count = visit_count + 1, last_visit = :lastVisit WHERE url = :url")
    void incrementVisitCount(String url, Date lastVisit);
    
    @Query("UPDATE browser_history SET is_bookmarked = :isBookmarked WHERE id = :historyId")
    void updateBookmarkStatus(long historyId, boolean isBookmarked);
    
    @Query("UPDATE browser_history SET title = :title, favicon_url = :faviconUrl WHERE url = :url")
    void updatePageInfo(String url, String title, String faviconUrl);
    
    // Cleanup
    @Query("DELETE FROM browser_history WHERE visited_at < :beforeDate")
    void deleteBefore(Date beforeDate);
    
    @Query("DELETE FROM browser_history WHERE visit_count = 1 AND visited_at < :beforeDate")
    void deleteSingleVisitsBefore(Date beforeDate);
} 