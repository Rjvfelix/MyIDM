package rjv.mg.myidm.ui.browser;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
    
    private VideoDetector videoDetector;
    private boolean canGoBack = false;
    
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
        
        // Add JavaScript interface for video detection
        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void onVideosDetected(String videoData) {
                try {
                    org.json.JSONArray jsonArray = new org.json.JSONArray(videoData);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        org.json.JSONObject video = jsonArray.getJSONObject(i);
                        String url = video.getString("url");
                        String type = video.getString("type");
                        String title = video.getString("title");
                        
                        VideoDetector.VideoInfo videoInfo = new VideoDetector.VideoInfo(url);
                        videoInfo.setType(type);
                        videoInfo.setTitle(title);
                        requireActivity().runOnUiThread(() -> {
                            showVideoDetectedDialog(videoInfo);
                        });
                    }
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
        });
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
            // Trigger video detection manually
            injectVideoDetectionScript();
            android.widget.Toast.makeText(requireContext(), 
                R.string.scanning_for_videos, 
                android.widget.Toast.LENGTH_SHORT).show();
        });
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
            "    " +
            "    // Detect HTML5 video elements" +
            "    var videoElements = document.querySelectorAll('video');" +
            "    for (var i = 0; i < videoElements.length; i++) {" +
            "        var video = videoElements[i];" +
            "        var sources = video.querySelectorAll('source');" +
            "        " +
            "        if (sources.length > 0) {" +
            "            for (var j = 0; j < sources.length; j++) {" +
            "                var source = sources[j];" +
            "                if (source.src) {" +
            "                    videos.push({" +
            "                        url: source.src," +
            "                        type: source.type || 'video/mp4'," +
            "                        title: video.title || document.title," +
            "                        width: video.videoWidth," +
            "                        height: video.videoHeight" +
            "                    });" +
            "                }" +
            "            }" +
            "        } else if (video.src) {" +
            "            videos.push({" +
            "                url: video.src," +
            "                type: video.type || 'video/mp4'," +
            "                title: video.title || document.title," +
            "                width: video.videoWidth," +
            "                height: video.videoHeight" +
            "            });" +
            "        }" +
            "    }" +
            "    " +
            "    // Detect iframe embeds (YouTube, Vimeo, etc.)" +
            "    var iframes = document.querySelectorAll('iframe');" +
            "    for (var i = 0; i < iframes.length; i++) {" +
            "        var iframe = iframes[i];" +
            "        if (iframe.src) {" +
            "            var src = iframe.src.toLowerCase();" +
            "            if (src.includes('youtube.com') || src.includes('youtu.be') || " +
            "                src.includes('vimeo.com') || src.includes('dailymotion.com') || " +
            "                src.includes('facebook.com') || src.includes('instagram.com')) {" +
            "                videos.push({" +
            "                    url: iframe.src," +
            "                    type: 'embed'," +
            "                    title: iframe.title || document.title" +
            "                });" +
            "            }" +
            "        }" +
            "    }" +
            "    " +
            "    // Detect video links in page" +
            "    var links = document.querySelectorAll('a[href]');" +
            "    for (var i = 0; i < links.length; i++) {" +
            "        var link = links[i];" +
            "        var href = link.href.toLowerCase();" +
            "        if (href.match(/\\.(mp4|avi|mkv|mov|wmv|flv|webm|m4v|3gp|ts|m3u8|mpd)(\\?.*)?$/)) {" +
            "            videos.push({" +
            "                url: link.href," +
            "                type: 'direct'," +
            "                title: link.textContent.trim() || document.title" +
            "            });" +
            "        }" +
            "    }" +
            "    " +
            "    // Detect streaming URLs in page content" +
            "    var pageText = document.body.innerText;" +
            "    var urlRegex = /https?:\\/\\/[^\\s\"']*\\.(m3u8|mpd)(\\?[^\\s\"']*)?/gi;" +
            "    var match;" +
            "    while ((match = urlRegex.exec(pageText)) !== null) {" +
            "        videos.push({" +
            "            url: match[0]," +
            "            type: 'stream'," +
            "            title: document.title" +
            "        });" +
            "    }" +
            "    " +
            "    if (videos.length > 0) {" +
            "        window.Android.onVideosDetected(JSON.stringify(videos));" +
            "    }" +
            "})();";
        
        webView.evaluateJavascript(script, null);
    }
    
    private void loadUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        webView.loadUrl(url);
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
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.video_detected)
            .setMessage(getString(R.string.download_video) + "\n\n" + videoInfo.getTitle())
            .setPositiveButton(R.string.download, (dialog, which) -> {
                // Add download to queue
                addVideoToDownloadQueue(videoInfo);
            })
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.preview, (dialog, which) -> {
                // Preview video
                previewVideo(videoInfo);
            })
            .show();
    }
    
    private void addVideoToDownloadQueue(VideoDetector.VideoInfo videoInfo) {
        // Create download entity
        rjv.mg.myidm.data.database.entity.DownloadEntity download = new rjv.mg.myidm.data.database.entity.DownloadEntity();
        download.setUrl(videoInfo.getUrl());
        download.setFilename(videoInfo.getTitle() + ".mp4");
        download.setType(rjv.mg.myidm.domain.model.DownloadType.HTTP_HTTPS);
        download.setStatus(rjv.mg.myidm.domain.model.DownloadStatus.PENDING);
        download.setSegmentCount(8); // Default segments
        
        // Add to download manager
        downloadManager.startDownload(download);
        
        android.widget.Toast.makeText(requireContext(), 
            getString(R.string.download_started), 
            android.widget.Toast.LENGTH_SHORT).show();
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