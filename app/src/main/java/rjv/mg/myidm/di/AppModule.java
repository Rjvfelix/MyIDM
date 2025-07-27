package rjv.mg.myidm.di;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import rjv.mg.myidm.data.database.DownloadDatabase;
import rjv.mg.myidm.data.database.dao.BrowserHistoryDao;
import rjv.mg.myidm.data.database.dao.DownloadDao;
import rjv.mg.myidm.data.database.dao.DownloadSegmentDao;
import rjv.mg.myidm.domain.browser.VideoDetector;
import rjv.mg.myidm.domain.downloader.DownloadManager;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {
    
    @Provides
    @Singleton
    public DownloadDatabase provideDownloadDatabase(@ApplicationContext Context context) {
        return DownloadDatabase.getInstance(context);
    }
    
    @Provides
    @Singleton
    public DownloadDao provideDownloadDao(DownloadDatabase database) {
        return database.downloadDao();
    }
    
    @Provides
    @Singleton
    public DownloadSegmentDao provideDownloadSegmentDao(DownloadDatabase database) {
        return database.downloadSegmentDao();
    }
    
    @Provides
    @Singleton
    public BrowserHistoryDao provideBrowserHistoryDao(DownloadDatabase database) {
        return database.browserHistoryDao();
    }
    
    @Provides
    @Singleton
    public VideoDetector provideVideoDetector() {
        return new VideoDetector();
    }
    
    @Provides
    @Singleton
    public DownloadManager provideDownloadManager(
            @ApplicationContext Context context,
            DownloadDao downloadDao,
            DownloadSegmentDao segmentDao,
            VideoDetector videoDetector) {
        return new DownloadManager(context, downloadDao, segmentDao, videoDetector);
    }
} 