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
                
                // Detect videos after page loads
                videoDetector.detectVideosInWebView(webView);
            }
            
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                progressBar.setVisibility(View.GONE);
                statusText.setText(getString(R.string.error_loading_page) + ": " + description);
            }
        });
    }
    
    private void setupListeners() {
        // URL input
        urlEditText.setOnEditorActionListener((v, actionId, event) -> {
            String url = urlEditText.getText().toString().trim();
            if (!url.isEmpty()) {
                loadUrl(url);
            }
            return true;
        });
        
        // Navigation buttons
        backButton.setOnClickListener(v -> goBack());
        forwardButton.setOnClickListener(v -> goForward());
        refreshButton.setOnClickListener(v -> refresh());
        shareButton.setOnClickListener(v -> sharePage());
        bookmarkButton.setOnClickListener(v -> bookmarkPage());
        downloadButton.setOnClickListener(v -> showDownloadOptions());
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
        // TODO: Show video download dialog
        android.widget.Toast.makeText(requireContext(), 
            getString(R.string.video_detected) + ": " + videoInfo.getUrl(), 
            android.widget.Toast.LENGTH_LONG).show();
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