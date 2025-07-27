package rjv.mg.myidm.ui.browser;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;
import rjv.mg.myidm.R;
import rjv.mg.myidm.domain.browser.VideoDetector;
import rjv.mg.myidm.domain.downloader.DownloadManager;

import javax.inject.Inject;

@AndroidEntryPoint
public class BrowserFragment extends Fragment {
    
    private WebView webView;
    private TextInputEditText urlEditText;
    private ImageButton backButton;
    private ImageButton forwardButton;
    private ImageButton refreshButton;
    private ImageButton shareButton;
    private ImageButton bookmarkButton;
    private ImageButton downloadButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private TextView downloadBadge;
    
    private VideoDetector videoDetector;
    private boolean canGoBack = false;
    private int detectedVideosCount = 0;
    
    @Inject
    rjv.mg.myidm.domain.downloader.DownloadManager downloadManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_browser, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize VideoDetector
        videoDetector = new VideoDetector();
        
        // Initialize views
        initializeViews(view);
        setupWebView();
        setupListeners();
        setupVideoDetection();
        
        // Load default page
        loadUrl("https://www.google.com");
    }
    
    private void initializeViews(View view) {
        webView = view.findViewById(R.id.web_view);
        urlEditText = view.findViewById(R.id.url_edit_text);
        backButton = view.findViewById(R.id.back_button);
        forwardButton = view.findViewById(R.id.forward_button);
        refreshButton = view.findViewById(R.id.refresh_button);
        shareButton = view.findViewById(R.id.share_button);
        bookmarkButton = view.findViewById(R.id.bookmark_button);
        downloadButton = view.findViewById(R.id.download_button);
        progressBar = view.findViewById(R.id.progress_bar);
        statusText = view.findViewById(R.id.status_text);
        downloadBadge = view.findViewById(R.id.download_badge);
    }
    
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setDefaultTextEncodingName("utf-8");
        
        // Enable network interception
        webView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_NO_CACHE);
        
        // Add JavaScript interface for video detection
        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void onVideosDetected(String videoData) {
                try {
                    org.json.JSONArray jsonArray = new org.json.JSONArray(videoData);
                    detectedVideosCount = jsonArray.length();
                    
                    requireActivity().runOnUiThread(() -> {
                        updateDownloadBadge(detectedVideosCount);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            @android.webkit.JavascriptInterface
            public void onVideoStarted(String videoData) {
                try {
                    org.json.JSONObject video = new org.json.JSONObject(videoData);
                    String url = video.getString("url");
                    String title = video.getString("title");
                    
                    VideoDetector.VideoInfo videoInfo = new VideoDetector.VideoInfo(url);
                    videoInfo.setTitle(title);
                    requireActivity().runOnUiThread(() -> {
                        // Show notification that video is playing
                        android.widget.Toast.makeText(requireContext(), 
                            getString(R.string.video_playing) + ": " + title, 
                            android.widget.Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "Android");
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                statusText.setText(R.string.loading);
                updateNavigationButtons();
                
                // Reset video count for new page
                detectedVideosCount = 0;
                updateDownloadBadge(0);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                statusText.setText("");
                urlEditText.setText(url);
                updateNavigationButtons();
                
                // Inject JavaScript for video detection
                injectVideoDetectionScript();
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                progressBar.setVisibility(View.GONE);
                statusText.setText(R.string.error_loading_page);
            }
            
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Detect video URLs but don't show dialog automatically
                if (isVideoUrl(url)) {
                    requireActivity().runOnUiThread(() -> {
                        detectedVideosCount++;
                        updateDownloadBadge(detectedVideosCount);
                    });
                }
                
                return super.shouldInterceptRequest(view, request);
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Detect video URLs in navigation but don't show dialog automatically
                if (isVideoUrl(url)) {
                    requireActivity().runOnUiThread(() -> {
                        detectedVideosCount++;
                        updateDownloadBadge(detectedVideosCount);
                    });
                    return true; // Intercept the URL
                }
                
                return false;
            }
        });
    }
    
    private boolean isVideoUrl(String url) {
        if (url == null) return false;
        
        // Video file extensions
        String[] videoExtensions = {".mp4", ".avi", ".mkv", ".mov", ".wmv", ".flv", ".webm", ".m4v", ".3gp", ".ts"};
        for (String ext : videoExtensions) {
            if (url.toLowerCase().contains(ext)) return true;
        }
        
        // Streaming URLs
        if (url.contains(".m3u8") || url.contains(".mpd")) return true;
        
        // Video CDN patterns
        String[] videoPatterns = {
            "video", "media", "stream", "play", "content", "asset"
        };
        for (String pattern : videoPatterns) {
            if (url.toLowerCase().contains(pattern)) return true;
        }
        
        return false;
    }
    
    private String getVideoType(String url) {
        if (url.contains(".m3u8")) return "application/x-mpegURL";
        if (url.contains(".mpd")) return "application/dash+xml";
        if (url.contains(".mp4")) return "video/mp4";
        if (url.contains(".webm")) return "video/webm";
        return "video/mp4";
    }
    
    private void setupListeners() {
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                loadUrl(urlEditText.getText().toString());
                return true;
            }
            return false;
        });
        
        backButton.setOnClickListener(v -> goBack());
        forwardButton.setOnClickListener(v -> goForward());
        refreshButton.setOnClickListener(v -> refresh());
        shareButton.setOnClickListener(v -> sharePage());
        bookmarkButton.setOnClickListener(v -> bookmarkPage());
        downloadButton.setOnClickListener(v -> {
            if (detectedVideosCount > 0) {
                // Show quality selection dialog for detected videos
                showQualitySelectionDialog(null);
            } else {
                // Force video detection without showing dialog
                forceVideoDetection();
                android.widget.Toast.makeText(requireContext(), 
                    R.string.scanning_for_videos, 
                    android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void forceVideoDetection() {
        // Inject aggressive video detection script
        String aggressiveScript = "javascript:" +
            "(function() {" +
            "    console.log('Force detecting videos...');" +
            "    " +
            "    // Method 1: Find all video elements" +
            "    var videos = document.querySelectorAll('video');" +
            "    console.log('Found ' + videos.length + ' video elements');" +
            "    " +
            "    for (var i = 0; i < videos.length; i++) {" +
            "        var video = videos[i];" +
            "        var url = video.src || video.currentSrc;" +
            "        if (url) {" +
            "            console.log('Video URL found:', url);" +
            "            window.Android.onVideosDetected(JSON.stringify([{" +
            "                url: url," +
            "                type: 'video/mp4'," +
            "                title: document.title" +
            "            }]));" +
            "        }" +
            "    }" +
            "    " +
            "    // Method 2: Find all source elements" +
            "    var sources = document.querySelectorAll('source');" +
            "    console.log('Found ' + sources.length + ' source elements');" +
            "    " +
            "    for (var i = 0; i < sources.length; i++) {" +
            "        var source = sources[i];" +
            "        var url = source.src;" +
            "        if (url) {" +
            "            console.log('Source URL found:', url);" +
            "            window.Android.onVideosDetected(JSON.stringify([{" +
            "                url: url," +
            "                type: source.type || 'video/mp4'," +
            "                title: document.title" +
            "            }]));" +
            "        }" +
            "    }" +
            "    " +
            "    // Method 3: Search in page content" +
            "    var pageText = document.body.innerText;" +
            "    var urlRegex = /https?:\\/\\/[^\\s\"']*\\.(mp4|webm|m3u8|mpd)(\\?[^\\s\"']*)?/gi;" +
            "    var match;" +
            "    while ((match = urlRegex.exec(pageText)) !== null) {" +
            "        console.log('URL found in page text:', match[0]);" +
            "        window.Android.onVideosDetected(JSON.stringify([{" +
            "            url: match[0]," +
            "            type: 'video/mp4'," +
            "            title: document.title" +
            "        }]));" +
            "    }" +
            "    " +
            "    // Method 4: Check iframes" +
            "    var iframes = document.querySelectorAll('iframe');" +
            "    console.log('Found ' + iframes.length + ' iframe elements');" +
            "    " +
            "    for (var i = 0; i < iframes.length; i++) {" +
            "        var iframe = iframes[i];" +
            "        var src = iframe.src;" +
            "        if (src && (src.includes('video') || src.includes('player'))) {" +
            "            console.log('Video iframe found:', src);" +
            "            window.Android.onVideosDetected(JSON.stringify([{" +
            "                url: src," +
            "                type: 'embed'," +
            "                title: document.title" +
            "            }]));" +
            "        }" +
            "    }" +
            "    " +
            "    // Method 5: Check for video players" +
            "    var players = document.querySelectorAll('[class*=\"player\"], [id*=\"player\"], [class*=\"video\"], [id*=\"video\"]');" +
            "    console.log('Found ' + players.length + ' potential video players');" +
            "    " +
            "    for (var i = 0; i < players.length; i++) {" +
            "        var player = players[i];" +
            "        var video = player.querySelector('video');" +
            "        if (video && video.src) {" +
            "            console.log('Video in player found:', video.src);" +
            "            window.Android.onVideosDetected(JSON.stringify([{" +
            "                url: video.src," +
            "                type: 'video/mp4'," +
            "                title: document.title" +
            "            }]));" +
            "        }" +
            "    }" +
            "})();";
        
        webView.evaluateJavascript(aggressiveScript, null);
    }
    
    private void setupVideoDetection() {
        videoDetector.setDetectionCallback(new VideoDetector.VideoDetectionCallback() {
            @Override
            public void onVideoDetected(VideoDetector.VideoInfo videoInfo) {
                showVideoDetectedDialog(videoInfo);
            }
            
            @Override
            public void onStreamDetected(VideoDetector.StreamInfo streamInfo) {
                showStreamDetectedDialog(streamInfo);
            }
            
            @Override
            public void onMultipleVideosDetected(java.util.List<VideoDetector.VideoInfo> videos) {
                showMultipleVideosDialog(videos);
            }
        });
    }
    
    private void injectVideoDetectionScript() {
        String script = "javascript:" +
            "(function() {" +
            "    var videos = [];" +
            "    var videoStreams = [];" +
            "    var monitoredVideos = new Set();" +
            "    " +
            "    // Monitor video elements for changes" +
            "    function monitorVideos() {" +
            "        var videoElements = document.querySelectorAll('video');" +
            "        for (var i = 0; i < videoElements.length; i++) {" +
            "            var video = videoElements[i];" +
            "            " +
            "            // Get current playing source" +
            "            if (video.src || video.currentSrc) {" +
            "                var currentUrl = video.src || video.currentSrc;" +
            "                var videoInfo = {" +
            "                    url: currentUrl," +
            "                    type: video.type || 'video/mp4'," +
            "                    title: video.title || document.title," +
            "                    width: video.videoWidth," +
            "                    height: video.videoHeight," +
            "                    duration: video.duration," +
            "                    isPlaying: !video.paused," +
            "                    currentTime: video.currentTime" +
            "                };" +
            "                " +
            "                // Check if this is a new video" +
            "                var exists = videos.find(v => v.url === currentUrl);" +
            "                if (!exists) {" +
            "                    videos.push(videoInfo);" +
            "                    console.log('New video detected:', videoInfo);" +
            "                }" +
            "                " +
            "                // Monitor for video start" +
            "                if (!monitoredVideos.has(currentUrl)) {" +
            "                    monitoredVideos.add(currentUrl);" +
            "                    " +
            "                    // Listen for play event" +
            "                    video.addEventListener('play', function() {" +
            "                        console.log('Video started playing:', videoInfo);" +
            "                        window.Android.onVideoStarted(JSON.stringify(videoInfo));" +
            "                    });" +
            "                    " +
            "                    // Listen for loadstart event" +
            "                    video.addEventListener('loadstart', function() {" +
            "                        console.log('Video loading started:', videoInfo);" +
            "                        window.Android.onVideoStarted(JSON.stringify(videoInfo));" +
            "                    });" +
            "                }" +
            "            }" +
            "            " +
            "            // Get all available sources" +
            "            var sources = video.querySelectorAll('source');" +
            "            for (var j = 0; j < sources.length; j++) {" +
            "                var source = sources[j];" +
            "                if (source.src) {" +
            "                    var streamInfo = {" +
            "                        url: source.src," +
            "                        type: source.type || 'video/mp4'," +
            "                        title: video.title || document.title," +
            "                        quality: source.getAttribute('data-quality') || 'auto'," +
            "                        size: source.getAttribute('data-size') || ''," +
            "                        bitrate: source.getAttribute('data-bitrate') || ''" +
            "                    };" +
            "                    videoStreams.push(streamInfo);" +
            "                }" +
            "            }" +
            "        }" +
            "    }" +
            "    " +
            "    // Monitor network requests for video streams" +
            "    function monitorNetworkRequests() {" +
            "        if (window.performance && window.performance.getEntriesByType) {" +
            "            var entries = window.performance.getEntriesByType('resource');" +
            "            for (var i = 0; i < entries.length; i++) {" +
            "                var entry = entries[i];" +
            "                var url = entry.name;" +
            "                " +
            "                // Detect video streams" +
            "                if (url.match(/\\.(m3u8|mpd|ts|mp4|webm)(\\?.*)?$/i)) {" +
            "                    var streamInfo = {" +
            "                        url: url," +
            "                        type: url.match(/\\.m3u8/i) ? 'application/x-mpegURL' : " +
            "                             url.match(/\\.mpd/i) ? 'application/dash+xml' : 'video/mp4'," +
            "                        title: document.title," +
            "                        quality: 'auto'," +
            "                        size: ''," +
            "                        bitrate: ''" +
            "                    };" +
            "                    videoStreams.push(streamInfo);" +
            "                }" +
            "            }" +
            "        }" +
            "    }" +
            "    " +
            "    // Detect HLS/DASH manifests" +
            "    function detectStreamingManifests() {" +
            "        var scripts = document.querySelectorAll('script');" +
            "        for (var i = 0; i < scripts.length; i++) {" +
            "            var script = scripts[i];" +
            "            if (script.textContent) {" +
            "                var content = script.textContent;" +
            "                " +
            "                // Find HLS manifests" +
            "                var hlsMatches = content.match(/https?:\\/\\/[^\\s\"']*\\.m3u8(\\?[^\\s\"']*)?/gi);" +
            "                if (hlsMatches) {" +
            "                    for (var j = 0; j < hlsMatches.length; j++) {" +
            "                        videoStreams.push({" +
            "                            url: hlsMatches[j]," +
            "                            type: 'application/x-mpegURL'," +
            "                            title: document.title," +
            "                            quality: 'HLS Stream'," +
            "                            size: ''," +
            "                            bitrate: ''" +
            "                        });" +
            "                    }" +
            "                }" +
            "                " +
            "                // Find DASH manifests" +
            "                var dashMatches = content.match(/https?:\\/\\/[^\\s\"']*\\.mpd(\\?[^\\s\"']*)?/gi);" +
            "                if (dashMatches) {" +
            "                    for (var j = 0; j < dashMatches.length; j++) {" +
            "                        videoStreams.push({" +
            "                            url: dashMatches[j]," +
            "                            type: 'application/dash+xml'," +
            "                            title: document.title," +
            "                            quality: 'DASH Stream'," +
            "                            size: ''," +
            "                            bitrate: ''" +
            "                        });" +
            "                    }" +
            "                }" +
            "            }" +
            "        }" +
            "    }" +
            "    " +
            "    // Run detection" +
            "    monitorVideos();" +
            "    monitorNetworkRequests();" +
            "    detectStreamingManifests();" +
            "    " +
            "    // Combine results" +
            "    var allVideos = videos.concat(videoStreams);" +
            "    " +
            "    if (allVideos.length > 0) {" +
            "        console.log('Detected videos:', allVideos);" +
            "        window.Android.onVideosDetected(JSON.stringify(allVideos));" +
            "    }" +
            "    " +
            "    // Set up continuous monitoring" +
            "    setInterval(function() {" +
            "        monitorVideos();" +
            "    }, 2000); // Check every 2 seconds" +
            "})();";
        
        webView.evaluateJavascript(script, null);
    }
    
    private void loadUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        webView.loadUrl(url);
    }
    
    private void loadTestVideoPage() {
        // Create a simple test page with video for debugging
        String testHtml = "<!DOCTYPE html>" +
            "<html><head><title>Test Video Page</title></head>" +
            "<body>" +
            "<h1>Test Video Detection</h1>" +
            "<video width='320' height='240' controls>" +
            "<source src='https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4' type='video/mp4'>" +
            "Your browser does not support the video tag." +
            "</video>" +
            "<br><br>" +
            "<video width='320' height='240' controls>" +
            "<source src='https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4' type='video/mp4'>" +
            "Your browser does not support the video tag." +
            "</video>" +
            "</body></html>";
        
        webView.loadData(testHtml, "text/html", "UTF-8");
    }
    
    public void goBack() {
        if (webView.canGoBack()) {
            webView.goBack();
        }
    }
    
    public void goForward() {
        if (webView.canGoForward()) {
            webView.goForward();
        }
    }
    
    public void refresh() {
        webView.reload();
    }
    
    public boolean canGoBack() {
        return webView.canGoBack();
    }
    
    private void updateNavigationButtons() {
        backButton.setEnabled(webView.canGoBack());
        forwardButton.setEnabled(webView.canGoForward());
    }
    
    private void sharePage() {
        String url = webView.getUrl();
        if (url != null) {
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, url);
            startActivity(android.content.Intent.createChooser(shareIntent, getString(R.string.share_via)));
        }
    }
    
    private void bookmarkPage() {
        String url = webView.getUrl();
        String title = webView.getTitle();
        if (url != null && title != null) {
            // TODO: Add to bookmarks
            android.widget.Toast.makeText(requireContext(), R.string.bookmark_added, android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showDownloadOptions() {
        // TODO: Show download options dialog
        android.widget.Toast.makeText(requireContext(), R.string.download_options, android.widget.Toast.LENGTH_SHORT).show();
    }
    
    private void showVideoDetectedDialog(VideoDetector.VideoInfo videoInfo) {
        // Create quality options dialog like 1DM
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_quality)
            .setMessage(getString(R.string.video_detected) + "\n\n" + videoInfo.getTitle())
            .setPositiveButton(R.string.download, (dialog, which) -> {
                showQualitySelectionDialog(videoInfo);
            })
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.preview, (dialog, which) -> {
                previewVideo(videoInfo);
            })
            .show();
    }
    
    private void showQualitySelectionDialog(VideoDetector.VideoInfo videoInfo) {
        // Create quality options
        String[] qualities = {
            "1080p HD (1920x1080) - ~500MB",
            "720p HD (1280x720) - ~250MB", 
            "480p SD (854x480) - ~150MB",
            "360p SD (640x360) - ~100MB",
            "Auto - Best Quality"
        };
        
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_quality)
            .setItems(qualities, (dialog, which) -> {
                String selectedQuality = qualities[which];
                addVideoToDownloadQueue(videoInfo, selectedQuality);
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }
    
    private void addVideoToDownloadQueue(VideoDetector.VideoInfo videoInfo, String quality) {
        // Create download entity
        rjv.mg.myidm.data.database.entity.DownloadEntity download = new rjv.mg.myidm.data.database.entity.DownloadEntity();
        download.setUrl(videoInfo.getUrl());
        download.setFilename(videoInfo.getTitle() + " [" + quality + "].mp4");
        download.setType(rjv.mg.myidm.domain.model.DownloadType.HTTP_HTTPS);
        download.setStatus(rjv.mg.myidm.domain.model.DownloadStatus.PENDING);
        download.setSegmentCount(8); // Default segments
        
        // Add to download manager
        downloadManager.startDownload(download);
        
        android.widget.Toast.makeText(requireContext(), 
            getString(R.string.download_started) + " - " + quality, 
            android.widget.Toast.LENGTH_SHORT).show();
    }
    
    private void addVideoToDownloadQueue(VideoDetector.VideoInfo videoInfo) {
        // Default quality
        addVideoToDownloadQueue(videoInfo, "Auto - Best Quality");
    }
    
    private void previewVideo(VideoDetector.VideoInfo videoInfo) {
        // Open video in external player or browser
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
        intent.setData(android.net.Uri.parse(videoInfo.getUrl()));
        startActivity(intent);
    }
    
    private void showStreamDetectedDialog(VideoDetector.StreamInfo streamInfo) {
        // TODO: Show stream download dialog
        android.widget.Toast.makeText(requireContext(), 
            getString(R.string.stream_detected) + ": " + streamInfo.getMasterUrl(), 
            android.widget.Toast.LENGTH_LONG).show();
    }
    
    private void showMultipleVideosDialog(java.util.List<VideoDetector.VideoInfo> videos) {
        // TODO: Show multiple videos selection dialog
        android.widget.Toast.makeText(requireContext(), 
            getString(R.string.multiple_videos_detected) + " (" + videos.size() + ")", 
            android.widget.Toast.LENGTH_LONG).show();
    }
    
    private void updateDownloadBadge(int count) {
        if (downloadBadge != null) {
            if (count > 0) {
                downloadBadge.setVisibility(View.VISIBLE);
                downloadBadge.setText(String.valueOf(count));
            } else {
                downloadBadge.setVisibility(View.GONE);
            }
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        webView.onPause();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        webView.onResume();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webView != null) {
            webView.destroy();
        }
    }
} 