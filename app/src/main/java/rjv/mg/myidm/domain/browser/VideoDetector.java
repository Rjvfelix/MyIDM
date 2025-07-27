package rjv.mg.myidm.domain.browser;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebViewClient;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rjv.mg.myidm.domain.model.DownloadType;

public class VideoDetector {
    private static final String TAG = "VideoDetector";
    
    // Video detection patterns
    private static final Pattern VIDEO_EXTENSION_PATTERN = Pattern.compile(
        "\\.(mp4|avi|mkv|mov|wmv|flv|webm|m4v|3gp|ts|m3u8|mpd)(\\?.*)?$", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern M3U8_PATTERN = Pattern.compile(
        "https?://[^\\s\"']*\\.m3u8(\\?[^\\s\"']*)?", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DASH_PATTERN = Pattern.compile(
        "https?://[^\\s\"']*\\.mpd(\\?[^\\s\"']*)?", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
        "(?:youtube\\.com/watch\\?v=|youtu\\.be/)([a-zA-Z0-9_-]{11})", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FACEBOOK_PATTERN = Pattern.compile(
        "facebook\\.com/[^/]+/videos/\\d+", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern INSTAGRAM_PATTERN = Pattern.compile(
        "instagram\\.com/p/[a-zA-Z0-9_-]+", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TIKTOK_PATTERN = Pattern.compile(
        "tiktok\\.com/@[^/]+/video/\\d+", 
        Pattern.CASE_INSENSITIVE
    );
    
    // Video quality patterns
    private static final Pattern QUALITY_PATTERN = Pattern.compile(
        "(1080p|720p|480p|360p|240p|144p)", 
        Pattern.CASE_INSENSITIVE
    );
    
    // JavaScript injection for video detection
    private static final String VIDEO_DETECTION_SCRIPT = 
        "javascript:(function() {" +
        "var videos = [];" +
        "var videoElements = document.querySelectorAll('video');" +
        "for(var i = 0; i < videoElements.length; i++) {" +
        "    var video = videoElements[i];" +
        "    if(video.src) {" +
        "        videos.push({" +
        "            src: video.src," +
        "            type: video.type || 'video/mp4'," +
        "            width: video.videoWidth," +
        "            height: video.videoHeight," +
        "            duration: video.duration" +
        "        });" +
        "    }" +
        "    if(video.currentSrc && video.currentSrc !== video.src) {" +
        "        videos.push({" +
        "            src: video.currentSrc," +
        "            type: video.type || 'video/mp4'," +
        "            width: video.videoWidth," +
        "            height: video.videoHeight," +
        "            duration: video.duration" +
        "        });" +
        "    }" +
        "}" +
        "var sources = document.querySelectorAll('source');" +
        "for(var i = 0; i < sources.length; i++) {" +
        "    var source = sources[i];" +
        "    if(source.src) {" +
        "        videos.push({" +
        "            src: source.src," +
        "            type: source.type || 'video/mp4'," +
        "            width: 0," +
        "            height: 0," +
        "            duration: 0" +
        "        });" +
        "    }" +
        "}" +
        "window.Android.onVideosDetected(JSON.stringify(videos));" +
        "})();";
    
    // Callbacks
    private VideoDetectionCallback detectionCallback;
    private VideoQualityCallback qualityCallback;
    
    public interface VideoDetectionCallback {
        void onVideoDetected(VideoInfo videoInfo);
        void onMultipleVideosDetected(List<VideoInfo> videos);
        void onStreamDetected(StreamInfo streamInfo);
    }
    
    public interface VideoQualityCallback {
        void onQualityAvailable(String url, List<VideoQuality> qualities);
        void onQualitySelected(String url, VideoQuality quality);
    }
    
    public static class VideoInfo {
        private String url;
        private String title;
        private String type;
        private long size;
        private int width;
        private int height;
        private long duration;
        private DownloadType downloadType;
        private Map<String, String> headers;
        private String referer;
        
        public VideoInfo(String url) {
            this.url = url;
            this.headers = new HashMap<>();
            this.downloadType = detectDownloadType(url);
        }
        
        // Getters and Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public DownloadType getDownloadType() { return downloadType; }
        public void setDownloadType(DownloadType downloadType) { this.downloadType = downloadType; }
        
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        
        public String getReferer() { return referer; }
        public void setReferer(String referer) { this.referer = referer; }
        
        public String getFilename() {
            if (title != null && !title.isEmpty()) {
                return sanitizeFilename(title);
            }
            
            // Extract filename from URL
            String filename = url.substring(url.lastIndexOf('/') + 1);
            if (filename.contains("?")) {
                filename = filename.substring(0, filename.indexOf('?'));
            }
            
            return filename.isEmpty() ? "video" : filename;
        }
        
        private String sanitizeFilename(String filename) {
            return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        }
    }
    
    public static class StreamInfo {
        private String masterUrl;
        private String type; // m3u8, mpd, etc.
        private List<VideoQuality> qualities;
        private Map<String, String> headers;
        private String referer;
        
        public StreamInfo(String masterUrl, String type) {
            this.masterUrl = masterUrl;
            this.type = type;
            this.qualities = new ArrayList<>();
            this.headers = new HashMap<>();
        }
        
        // Getters and Setters
        public String getMasterUrl() { return masterUrl; }
        public void setMasterUrl(String masterUrl) { this.masterUrl = masterUrl; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public List<VideoQuality> getQualities() { return qualities; }
        public void setQualities(List<VideoQuality> qualities) { this.qualities = qualities; }
        
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        
        public String getReferer() { return referer; }
        public void setReferer(String referer) { this.referer = referer; }
    }
    
    public static class VideoQuality {
        private String url;
        private String label;
        private int width;
        private int height;
        private int bitrate;
        private String codec;
        
        public VideoQuality(String url, String label) {
            this.url = url;
            this.label = label;
        }
        
        // Getters and Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        
        public int getBitrate() { return bitrate; }
        public void setBitrate(int bitrate) { this.bitrate = bitrate; }
        
        public String getCodec() { return codec; }
        public void setCodec(String codec) { this.codec = codec; }
        
        @Override
        public String toString() {
            return label + " (" + width + "x" + height + ")";
        }
    }
    
    public VideoDetector() {
        // Constructor
    }
    
    public void detectVideosInWebView(WebView webView) {
        if (webView == null) return;
        
        // Inject JavaScript to detect videos
        webView.evaluateJavascript(VIDEO_DETECTION_SCRIPT, null);
    }
    
    public void detectVideosInUrl(String url) {
        List<VideoInfo> videos = new ArrayList<>();
        
        // Check for direct video files
        if (isVideoUrl(url)) {
            VideoInfo video = new VideoInfo(url);
            videos.add(video);
        }
        
        // Check for streaming URLs
        if (isStreamingUrl(url)) {
            StreamInfo stream = createStreamInfo(url);
            if (detectionCallback != null) {
                detectionCallback.onStreamDetected(stream);
            }
        }
        
        // Check for social media videos
        VideoInfo socialVideo = detectSocialMediaVideo(url);
        if (socialVideo != null) {
            videos.add(socialVideo);
        }
        
        if (!videos.isEmpty()) {
            if (videos.size() == 1) {
                if (detectionCallback != null) {
                    detectionCallback.onVideoDetected(videos.get(0));
                }
            } else {
                if (detectionCallback != null) {
                    detectionCallback.onMultipleVideosDetected(videos);
                }
            }
        }
    }
    
    public void detectVideosInHtml(String html, String baseUrl) {
        List<VideoInfo> videos = new ArrayList<>();
        
        // Extract video URLs from HTML
        Set<String> videoUrls = extractVideoUrlsFromHtml(html);
        
        for (String url : videoUrls) {
            // Resolve relative URLs
            String absoluteUrl = resolveUrl(url, baseUrl);
            
            if (isVideoUrl(absoluteUrl)) {
                VideoInfo video = new VideoInfo(absoluteUrl);
                videos.add(video);
            }
        }
        
        // Extract streaming URLs
        Set<String> streamUrls = extractStreamingUrlsFromHtml(html);
        
        for (String url : streamUrls) {
            String absoluteUrl = resolveUrl(url, baseUrl);
            StreamInfo stream = createStreamInfo(absoluteUrl);
            if (detectionCallback != null) {
                detectionCallback.onStreamDetected(stream);
            }
        }
        
        if (!videos.isEmpty()) {
            if (videos.size() == 1) {
                if (detectionCallback != null) {
                    detectionCallback.onVideoDetected(videos.get(0));
                }
            } else {
                if (detectionCallback != null) {
                    detectionCallback.onMultipleVideosDetected(videos);
                }
            }
        }
    }
    
    private boolean isVideoUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        
        return VIDEO_EXTENSION_PATTERN.matcher(url).find();
    }
    
    private boolean isStreamingUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        
        return M3U8_PATTERN.matcher(url).find() || 
               DASH_PATTERN.matcher(url).find();
    }
    
    private StreamInfo createStreamInfo(String url) {
        if (M3U8_PATTERN.matcher(url).find()) {
            return new StreamInfo(url, "m3u8");
        } else if (DASH_PATTERN.matcher(url).find()) {
            return new StreamInfo(url, "mpd");
        }
        return null;
    }
    
    private VideoInfo detectSocialMediaVideo(String url) {
        if (YOUTUBE_PATTERN.matcher(url).find()) {
            VideoInfo video = new VideoInfo(url);
            video.setDownloadType(DownloadType.YOUTUBE);
            return video;
        } else if (FACEBOOK_PATTERN.matcher(url).find()) {
            VideoInfo video = new VideoInfo(url);
            video.setDownloadType(DownloadType.FACEBOOK);
            return video;
        } else if (INSTAGRAM_PATTERN.matcher(url).find()) {
            VideoInfo video = new VideoInfo(url);
            video.setDownloadType(DownloadType.INSTAGRAM);
            return video;
        } else if (TIKTOK_PATTERN.matcher(url).find()) {
            VideoInfo video = new VideoInfo(url);
            video.setDownloadType(DownloadType.TIKTOK);
            return video;
        }
        return null;
    }
    
    private Set<String> extractVideoUrlsFromHtml(String html) {
        Set<String> urls = new HashSet<>();
        
        // Extract from video tags
        Pattern videoPattern = Pattern.compile(
            "<video[^>]*src=[\"']([^\"']+)[\"'][^>]*>", 
            Pattern.CASE_INSENSITIVE
        );
        Matcher videoMatcher = videoPattern.matcher(html);
        while (videoMatcher.find()) {
            urls.add(videoMatcher.group(1));
        }
        
        // Extract from source tags
        Pattern sourcePattern = Pattern.compile(
            "<source[^>]*src=[\"']([^\"']+)[\"'][^>]*>", 
            Pattern.CASE_INSENSITIVE
        );
        Matcher sourceMatcher = sourcePattern.matcher(html);
        while (sourceMatcher.find()) {
            urls.add(sourceMatcher.group(1));
        }
        
        // Extract from object/embed tags
        Pattern objectPattern = Pattern.compile(
            "<(?:object|embed)[^>]*src=[\"']([^\"']+)[\"'][^>]*>", 
            Pattern.CASE_INSENSITIVE
        );
        Matcher objectMatcher = objectPattern.matcher(html);
        while (objectMatcher.find()) {
            urls.add(objectMatcher.group(1));
        }
        
        return urls;
    }
    
    private Set<String> extractStreamingUrlsFromHtml(String html) {
        Set<String> urls = new HashSet<>();
        
        // Extract M3U8 URLs
        Matcher m3u8Matcher = M3U8_PATTERN.matcher(html);
        while (m3u8Matcher.find()) {
            urls.add(m3u8Matcher.group());
        }
        
        // Extract DASH URLs
        Matcher dashMatcher = DASH_PATTERN.matcher(html);
        while (dashMatcher.find()) {
            urls.add(dashMatcher.group());
        }
        
        return urls;
    }
    
    private String resolveUrl(String url, String baseUrl) {
        if (url == null || url.isEmpty()) return url;
        
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        
        if (url.startsWith("/")) {
            // Absolute path
            try {
                URL base = new URL(baseUrl);
                return base.getProtocol() + "://" + base.getHost() + url;
            } catch (Exception e) {
                return url;
            }
        }
        
        // Relative path
        try {
            URL base = new URL(baseUrl);
            return new URL(base, url).toString();
        } catch (Exception e) {
            return url;
        }
    }
    
    public static DownloadType detectDownloadType(String url) {
        if (url == null || url.isEmpty()) return DownloadType.HTTP_HTTPS;
        
        if (M3U8_PATTERN.matcher(url).find()) {
            return DownloadType.M3U8;
        } else if (DASH_PATTERN.matcher(url).find()) {
            return DownloadType.DASH;
        } else if (YOUTUBE_PATTERN.matcher(url).find()) {
            return DownloadType.YOUTUBE;
        } else if (FACEBOOK_PATTERN.matcher(url).find()) {
            return DownloadType.FACEBOOK;
        } else if (INSTAGRAM_PATTERN.matcher(url).find()) {
            return DownloadType.INSTAGRAM;
        } else if (TIKTOK_PATTERN.matcher(url).find()) {
            return DownloadType.TIKTOK;
        } else if (url.startsWith("ftp://")) {
            return DownloadType.FTP;
        } else if (url.startsWith("sftp://")) {
            return DownloadType.SFTP;
        } else if (url.endsWith(".torrent")) {
            return DownloadType.TORRENT;
        }
        
        return DownloadType.HTTP_HTTPS;
    }
    
    public void setDetectionCallback(VideoDetectionCallback callback) {
        this.detectionCallback = callback;
    }
    
    public void setQualityCallback(VideoQualityCallback callback) {
        this.qualityCallback = callback;
    }
} 