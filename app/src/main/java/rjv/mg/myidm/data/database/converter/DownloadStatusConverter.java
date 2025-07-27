package rjv.mg.myidm.data.database.converter;

import androidx.room.TypeConverter;
import rjv.mg.myidm.domain.model.DownloadStatus;
import rjv.mg.myidm.domain.model.DownloadType;
import rjv.mg.myidm.domain.model.SegmentStatus;

public class DownloadStatusConverter {
    
    @TypeConverter
    public static DownloadStatus toDownloadStatus(String value) {
        return value == null ? null : DownloadStatus.valueOf(value);
    }
    
    @TypeConverter
    public static String fromDownloadStatus(DownloadStatus status) {
        return status == null ? null : status.name();
    }
    
    @TypeConverter
    public static DownloadType toDownloadType(String value) {
        return value == null ? null : DownloadType.valueOf(value);
    }
    
    @TypeConverter
    public static String fromDownloadType(DownloadType type) {
        return type == null ? null : type.name();
    }
    
    @TypeConverter
    public static SegmentStatus toSegmentStatus(String value) {
        return value == null ? null : SegmentStatus.valueOf(value);
    }
    
    @TypeConverter
    public static String fromSegmentStatus(SegmentStatus status) {
        return status == null ? null : status.name();
    }
} 