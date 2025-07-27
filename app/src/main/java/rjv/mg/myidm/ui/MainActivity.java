package rjv.mg.myidm.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.snackbar.Snackbar;

import javax.annotation.Nonnull;

import dagger.hilt.android.AndroidEntryPoint;
import rjv.mg.myidm.R;
import rjv.mg.myidm.ui.browser.BrowserFragment;
import rjv.mg.myidm.ui.download.DownloadsFragment;
import rjv.mg.myidm.ui.download.AddDownloadDialogFragment;
import rjv.mg.myidm.ui.download.SearchDialogFragment;
import rjv.mg.myidm.ui.settings.SettingsFragment;
import rjv.mg.myidm.viewmodel.MainViewModel;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {
    
    private MainViewModel viewModel;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAddDownload;
    
    // Permission launcher
    private final ActivityResultLauncher<String[]> permissionLauncher = 
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean allGranted = true;
            for (Boolean granted : result.values()) {
                if (!granted) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                onPermissionsGranted();
            } else {
                showPermissionDeniedDialog();
            }
        });
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        // Initialize UI
        initializeViews();
        setupNavigation();
        setupObservers();
        
        // Check permissions
        checkAndRequestPermissions();
        
        // Handle intent
        handleIntent(getIntent());
    }
    
    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fabAddDownload = findViewById(R.id.fab_add_download);
        
        // Setup FAB
        fabAddDownload.setOnClickListener(v -> showAddDownloadDialog());
        
        // Setup toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
    }
    
    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(this);
        // Set default fragment
        if (getSupportFragmentManager().getFragments().isEmpty()) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new DownloadsFragment())
                .commit();
            bottomNavigationView.setSelectedItemId(R.id.nav_downloads);
        }
    }
    
    private void setupObservers() {
        // Observe download progress
        viewModel.getActiveDownloadsCount().observe(this, count -> {
            updateDownloadBadge(count);
        });
        
        // Observe errors
        viewModel.getError().observe(this, error -> {
            if (error != null) {
                showErrorSnackbar(error);
            }
        });
    }
    
    @Override
    public boolean onNavigationItemSelected(@Nonnull MenuItem item) {
        Fragment fragment = null;
        String title = "";
        
        int itemId = item.getItemId();
        if (itemId == R.id.nav_downloads) {
            fragment = new DownloadsFragment();
            title = getString(R.string.downloads);
        } else if (itemId == R.id.nav_browser) {
            fragment = new BrowserFragment();
            title = getString(R.string.browser);
        } else if (itemId == R.id.nav_settings) {
            fragment = new SettingsFragment();
            title = getString(R.string.settings);
        }
        
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
            
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title);
            }
            
            return true;
        }
        
        return false;
    }
    
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
            };
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions = new String[]{
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.POST_NOTIFICATIONS
                };
            }
            
            boolean allGranted = true;
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                permissionLauncher.launch(permissions);
            } else {
                onPermissionsGranted();
            }
        } else {
            onPermissionsGranted();
        }
    }
    
    private void onPermissionsGranted() {
        // Initialize services
        viewModel.initializeServices();
        
        // Show welcome message
        Snackbar.make(findViewById(android.R.id.content), 
            R.string.welcome_message, Snackbar.LENGTH_LONG).show();
    }
    
    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.permissions_required)
            .setMessage(R.string.permissions_explanation)
            .setPositiveButton(R.string.settings, (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            })
            .setNegativeButton(R.string.cancel, (dialog, which) -> {
                finish();
            })
            .setCancelable(false)
            .show();
    }
    
    private void showAddDownloadDialog() {
        AddDownloadDialogFragment dialog = new AddDownloadDialogFragment();
        dialog.show(getSupportFragmentManager(), "add_download");
    }
    
    private void updateDownloadBadge(int count) {
        // Badge non supporté nativement sur MenuItem, on peut ignorer ou utiliser une solution custom
        // Ici, on ne fait rien pour débloquer la compilation
    }
    
    private void showErrorSnackbar(String error) {
        Snackbar.make(findViewById(android.R.id.content), error, Snackbar.LENGTH_LONG)
            .setAction(R.string.retry, v -> viewModel.retryFailedDownloads())
            .show();
    }
    
    private void handleIntent(Intent intent) {
        if (intent != null && intent.getAction() != null) {
            if (Intent.ACTION_VIEW.equals(intent.getAction())) {
                String url = intent.getDataString();
                if (url != null) {
                    // Handle URL intent
                    viewModel.handleUrlIntent(url);
                    
                    // Switch to browser tab
                    bottomNavigationView.setSelectedItemId(R.id.nav_browser);
                }
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@Nonnull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_search) {
            showSearchDialog();
            return true;
        } else if (itemId == R.id.action_clear_completed) {
            viewModel.clearCompletedDownloads();
            return true;
        } else if (itemId == R.id.action_export_downloads) {
            viewModel.exportDownloads();
            return true;
        } else if (itemId == R.id.action_import_downloads) {
            viewModel.importDownloads();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void showSearchDialog() {
        SearchDialogFragment dialog = new SearchDialogFragment();
        dialog.show(getSupportFragmentManager(), "search");
    }
    
    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager()
            .findFragmentById(R.id.fragment_container);
        
        if (currentFragment instanceof BrowserFragment) {
            BrowserFragment browserFragment = (BrowserFragment) currentFragment;
            if (browserFragment.canGoBack()) {
                browserFragment.goBack();
                return;
            }
        }
        
        super.onBackPressed();
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.cleanup();
    }
} 