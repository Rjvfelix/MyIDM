package rjv.mg.myidm.data.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import android.content.Context;

import rjv.mg.myidm.data.database.dao.DownloadDao;
import rjv.mg.myidm.data.database.dao.DownloadSegmentDao;
import rjv.mg.myidm.data.database.dao.BrowserHistoryDao;
import rjv.mg.myidm.data.database.entity.DownloadEntity;
import rjv.mg.myidm.data.database.entity.DownloadSegmentEntity;
import rjv.mg.myidm.data.database.entity.BrowserHistoryEntity;
import rjv.mg.myidm.data.database.converter.DateConverter;
import rjv.mg.myidm.data.database.converter.DownloadStatusConverter;

@Database(
    entities = {
        DownloadEntity.class,
        DownloadSegmentEntity.class,
        BrowserHistoryEntity.class
    },
    version = 1,
    exportSchema = false
)
@TypeConverters({DateConverter.class, DownloadStatusConverter.class})
public abstract class DownloadDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "myidm_database";
    private static volatile DownloadDatabase INSTANCE;
    
    public abstract DownloadDao downloadDao();
    public abstract DownloadSegmentDao downloadSegmentDao();
    public abstract BrowserHistoryDao browserHistoryDao();
    
    public static DownloadDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DownloadDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        DownloadDatabase.class,
                        DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
} 