package rjv.mg.myidm.ui.download;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import dagger.hilt.android.AndroidEntryPoint;
import rjv.mg.myidm.R;
import rjv.mg.myidm.domain.model.DownloadStatus;

@AndroidEntryPoint
public class DownloadsFragment extends Fragment {
    
    private DownloadsViewModel viewModel;
    private DownloadsAdapter adapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ChipGroup filterChipGroup;
    private TextView emptyStateText;
    private FloatingActionButton fabAddDownload;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_downloads, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(DownloadsViewModel.class);
        
        // Initialize views
        initializeViews(view);
        setupRecyclerView();
        setupObservers();
        setupFilterChips();
        setupSwipeRefresh();
    }
    
    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        filterChipGroup = view.findViewById(R.id.filter_chip_group);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        fabAddDownload = view.findViewById(R.id.fab_add_download);
        
        // Setup FAB
        fabAddDownload.setOnClickListener(v -> showAddDownloadDialog());
    }
    
    private void setupRecyclerView() {
        adapter = new DownloadsAdapter();
        adapter.setOnItemClickListener(new DownloadsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // Handle item click
                showDownloadDetails(position);
            }
            
            @Override
            public void onPauseClick(int position) {
                // Handle pause click
                viewModel.pauseDownload(position);
            }
            
            @Override
            public void onResumeClick(int position) {
                // Handle resume click
                viewModel.resumeDownload(position);
            }
            
            @Override
            public void onCancelClick(int position) {
                // Handle cancel click
                viewModel.cancelDownload(position);
            }
            
            @Override
            public void onDeleteClick(int position) {
                // Handle delete click
                viewModel.deleteDownload(position);
            }
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }
    
    private void setupObservers() {
        // Observe downloads
        viewModel.getDownloads().observe(getViewLifecycleOwner(), downloads -> {
            adapter.submitList(downloads);
            updateEmptyState(downloads.isEmpty());
        });
        
        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            swipeRefreshLayout.setRefreshing(isLoading);
        });
        
        // Observe error
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showError(error);
            }
        });
        
        // Observe active downloads count
        viewModel.getActiveDownloadsCount().observe(getViewLifecycleOwner(), count -> {
            updateActiveDownloadsCount(count);
        });
    }
    
    private void setupFilterChips() {
        // Add filter chips
        addFilterChip("Tous", null);
        addFilterChip("En cours", DownloadStatus.DOWNLOADING);
        addFilterChip("En pause", DownloadStatus.PAUSED);
        addFilterChip("Terminés", DownloadStatus.COMPLETED);
        addFilterChip("Échecs", DownloadStatus.FAILED);
        
        // Set default selection
        filterChipGroup.check(R.id.chip_all);
    }
    
    private void addFilterChip(String text, DownloadStatus status) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                viewModel.setFilter(status);
            }
        });
        filterChipGroup.addView(chip);
    }
    
    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.refreshDownloads();
        });
    }
    
    private void showAddDownloadDialog() {
        AddDownloadDialogFragment dialog = new AddDownloadDialogFragment();
        dialog.show(getChildFragmentManager(), "add_download");
    }
    
    private void showDownloadDetails(int position) {
        // TODO: Show download details dialog
    }
    
    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyStateText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateActiveDownloadsCount(int count) {
        // Update UI to show active downloads count
        if (count > 0) {
            // Show active downloads indicator
        } else {
            // Hide active downloads indicator
        }
    }
    
    private void showError(String error) {
        // Show error message using Snackbar or Toast
    }
    
    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshDownloads();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up any resources
    }
} 