package rjv.mg.myidm.ui.download;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;

import dagger.hilt.android.AndroidEntryPoint;
import rjv.mg.myidm.R;
import rjv.mg.myidm.data.database.entity.DownloadEntity;
import rjv.mg.myidm.domain.model.DownloadType;

@AndroidEntryPoint
public class AddDownloadDialogFragment extends DialogFragment {
    
    private DownloadsViewModel viewModel;
    private TextInputLayout urlInputLayout;
    private EditText urlEditText;
    private TextInputLayout filenameInputLayout;
    private EditText filenameEditText;
    private Button addButton;
    private Button cancelButton;
    private ProgressBar progressBar;
    private TextView statusText;
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_MyIDM_Dialog);
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_download, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(DownloadsViewModel.class);
        
        // Initialize views
        initializeViews(view);
        setupListeners();
        setupObservers();
    }
    
    private void initializeViews(View view) {
        urlInputLayout = view.findViewById(R.id.url_input_layout);
        urlEditText = view.findViewById(R.id.url_edit_text);
        filenameInputLayout = view.findViewById(R.id.filename_input_layout);
        filenameEditText = view.findViewById(R.id.filename_edit_text);
        addButton = view.findViewById(R.id.add_button);
        cancelButton = view.findViewById(R.id.cancel_button);
        progressBar = view.findViewById(R.id.progress_bar);
        statusText = view.findViewById(R.id.status_text);
        
        // Set dialog title
        if (getDialog() != null) {
            getDialog().setTitle(R.string.add_download);
        }
    }
    
    private void setupListeners() {
        addButton.setOnClickListener(v -> validateAndAddDownload());
        cancelButton.setOnClickListener(v -> dismiss());
        
        // Auto-detect filename from URL
        urlEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (TextUtils.isEmpty(filenameEditText.getText())) {
                    String filename = extractFilenameFromUrl(s.toString());
                    if (!TextUtils.isEmpty(filename)) {
                        filenameEditText.setText(filename);
                    }
                }
            }
        });
    }
    
    private void setupObservers() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            addButton.setEnabled(!isLoading);
        });
        
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                statusText.setText(error);
                statusText.setVisibility(View.VISIBLE);
                viewModel.clearError();
            } else {
                statusText.setVisibility(View.GONE);
            }
        });
    }
    
    private void validateAndAddDownload() {
        String url = urlEditText.getText().toString().trim();
        String filename = filenameEditText.getText().toString().trim();
        
        // Validate URL
        if (TextUtils.isEmpty(url)) {
            urlInputLayout.setError(getString(R.string.error_url_required));
            return;
        }
        
        if (!isValidUrl(url)) {
            urlInputLayout.setError(getString(R.string.error_invalid_url));
            return;
        }
        
        // Validate filename
        if (TextUtils.isEmpty(filename)) {
            filenameInputLayout.setError(getString(R.string.error_filename_required));
            return;
        }
        
        // Clear errors
        urlInputLayout.setError(null);
        filenameInputLayout.setError(null);
        
        // Add download
        addDownload(url, filename);
    }
    
    private void addDownload(String url, String filename) {
        try {
            // Create download entity
            DownloadEntity download = new DownloadEntity(url, filename, DownloadType.HTTP_HTTPS);
            
            // Detect download type
            DownloadType type = detectDownloadType(url);
            download.setType(type);
            
            // Set default values
            download.setPriority(0);
            download.setMaxConcurrentSegments(8);
            download.setMaxRetries(3);
            
            // Add to database and start download
            viewModel.addDownload(download);
            
            // Show success message
            Toast.makeText(requireContext(), R.string.download_added_successfully, Toast.LENGTH_SHORT).show();
            
            // Dismiss dialog
            dismiss();
            
        } catch (Exception e) {
            statusText.setText(getString(R.string.error_adding_download) + ": " + e.getMessage());
            statusText.setVisibility(View.VISIBLE);
        }
    }
    
    private boolean isValidUrl(String url) {
        return android.util.Patterns.WEB_URL.matcher(url).matches();
    }
    
    private String extractFilenameFromUrl(String url) {
        if (TextUtils.isEmpty(url)) return "";
        
        try {
            // Extract filename from URL path
            String path = new java.net.URL(url).getPath();
            if (path.contains("/")) {
                String filename = path.substring(path.lastIndexOf("/") + 1);
                if (!TextUtils.isEmpty(filename) && filename.contains(".")) {
                    return filename;
                }
            }
            
            // If no filename in path, try to extract from query parameters
            if (url.contains("filename=")) {
                int start = url.indexOf("filename=") + 9;
                int end = url.indexOf("&", start);
                if (end == -1) end = url.length();
                return url.substring(start, end);
            }
            
        } catch (Exception e) {
            // Ignore parsing errors
        }
        
        return "";
    }
    
    private DownloadType detectDownloadType(String url) {
        String lowerUrl = url.toLowerCase();
        
        if (lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be")) {
            return DownloadType.YOUTUBE;
        } else if (lowerUrl.contains("facebook.com")) {
            return DownloadType.FACEBOOK;
        } else if (lowerUrl.contains("instagram.com")) {
            return DownloadType.INSTAGRAM;
        } else if (lowerUrl.contains("tiktok.com")) {
            return DownloadType.TIKTOK;
        } else if (lowerUrl.endsWith(".torrent")) {
            return DownloadType.TORRENT;
        } else if (lowerUrl.contains(".m3u8")) {
            return DownloadType.M3U8;
        } else if (lowerUrl.contains(".mpd")) {
            return DownloadType.DASH;
        } else {
            return DownloadType.HTTP_HTTPS;
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
        
        // Set dialog width
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
} 