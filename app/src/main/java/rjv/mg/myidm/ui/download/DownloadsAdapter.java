package rjv.mg.myidm.ui.download;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import rjv.mg.myidm.R;
import rjv.mg.myidm.data.database.entity.DownloadEntity;
import rjv.mg.myidm.domain.model.DownloadStatus;
import rjv.mg.myidm.domain.model.DownloadType;

public class DownloadsAdapter extends ListAdapter<DownloadEntity, DownloadsAdapter.DownloadViewHolder> {
    
    private OnItemClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    
    public DownloadsAdapter() {
        super(new DiffUtil.ItemCallback<DownloadEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull DownloadEntity oldItem, @NonNull DownloadEntity newItem) {
                return oldItem.getId() == newItem.getId();
            }
            
            @Override
            public boolean areContentsTheSame(@NonNull DownloadEntity oldItem, @NonNull DownloadEntity newItem) {
                return oldItem.getStatus() == newItem.getStatus() &&
                       oldItem.getProgress() == newItem.getProgress() &&
                       oldItem.getDownloadedSize() == newItem.getDownloadedSize() &&
                       oldItem.getDownloadSpeed() == newItem.getDownloadSpeed();
            }
        });
    }
    
    @NonNull
    @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_download, parent, false);
        return new DownloadViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
        DownloadEntity download = getItem(position);
        holder.bind(download);
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    public class DownloadViewHolder extends RecyclerView.ViewHolder {
        
        private final MaterialCardView cardView;
        private final ImageView fileTypeIcon;
        private final TextView filenameText;
        private final TextView urlText;
        private final TextView sizeText;
        private final TextView speedText;
        private final TextView statusText;
        private final TextView progressText;
        private final TextView dateText;
        private final ProgressBar progressBar;
        private final Chip typeChip;
        private final ImageButton pauseResumeButton;
        private final ImageButton cancelButton;
        private final ImageButton deleteButton;
        private final ImageButton moreButton;
        
        public DownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            
            cardView = itemView.findViewById(R.id.card_view);
            fileTypeIcon = itemView.findViewById(R.id.file_type_icon);
            filenameText = itemView.findViewById(R.id.filename_text);
            urlText = itemView.findViewById(R.id.url_text);
            sizeText = itemView.findViewById(R.id.size_text);
            speedText = itemView.findViewById(R.id.speed_text);
            statusText = itemView.findViewById(R.id.status_text);
            progressText = itemView.findViewById(R.id.progress_text);
            dateText = itemView.findViewById(R.id.date_text);
            progressBar = itemView.findViewById(R.id.progress_bar);
            typeChip = itemView.findViewById(R.id.type_chip);
            pauseResumeButton = itemView.findViewById(R.id.pause_resume_button);
            cancelButton = itemView.findViewById(R.id.cancel_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
            moreButton = itemView.findViewById(R.id.more_button);
            
            setupClickListeners();
        }
        
        private void setupClickListeners() {
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(getAdapterPosition());
                }
            });
            
            pauseResumeButton.setOnClickListener(v -> {
                if (listener != null) {
                    DownloadEntity download = getItem(getAdapterPosition());
                    if (download.getStatus() == DownloadStatus.DOWNLOADING || 
                        download.getStatus() == DownloadStatus.RESUMING) {
                        listener.onPauseClick(getAdapterPosition());
                    } else if (download.getStatus() == DownloadStatus.PAUSED) {
                        listener.onResumeClick(getAdapterPosition());
                    }
                }
            });
            
            cancelButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCancelClick(getAdapterPosition());
                }
            });
            
            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(getAdapterPosition());
                }
            });
            
            moreButton.setOnClickListener(v -> {
                // TODO: Show more options menu
            });
        }
        
        public void bind(DownloadEntity download) {
            // Set filename
            filenameText.setText(download.getFilename());
            
            // Set URL (truncated)
            String url = download.getUrl();
            if (url.length() > 50) {
                url = url.substring(0, 47) + "...";
            }
            urlText.setText(url);
            
            // Set file type icon
            setFileTypeIcon(download.getType());
            
            // Set type chip
            typeChip.setText(getTypeDisplayName(download.getType()));
            
            // Set size
            if (download.getFileSize() > 0) {
                sizeText.setText(formatFileSize(download.getFileSize()));
            } else {
                sizeText.setText("Taille inconnue");
            }
            
            // Set progress
            int progress = download.getProgress();
            progressBar.setProgress(progress);
            progressText.setText(progress + "%");
            
            // Set speed
            if (download.getDownloadSpeed() > 0) {
                speedText.setText(formatSpeed(download.getDownloadSpeed()));
            } else {
                speedText.setText("");
            }
            
            // Set status
            statusText.setText(getStatusDisplayName(download.getStatus()));
            statusText.setTextColor(getStatusColor(download.getStatus()));
            
            // Set date
            if (download.getCreatedAt() != null) {
                dateText.setText(dateFormat.format(download.getCreatedAt()));
            }
            
            // Update action buttons based on status
            updateActionButtons(download.getStatus());
        }
        
        private void setFileTypeIcon(DownloadType type) {
            switch (type) {
                case TORRENT:
                    fileTypeIcon.setImageResource(R.drawable.ic_torrent);
                    break;
                case M3U8:
                case DASH:
                    fileTypeIcon.setImageResource(R.drawable.ic_stream);
                    break;
                case YOUTUBE:
                case FACEBOOK:
                case INSTAGRAM:
                case TIKTOK:
                    fileTypeIcon.setImageResource(R.drawable.ic_social);
                    break;
                default:
                    fileTypeIcon.setImageResource(R.drawable.ic_file);
                    break;
            }
        }
        
        private String getTypeDisplayName(DownloadType type) {
            switch (type) {
                case TORRENT: return "Torrent";
                case M3U8: return "HLS";
                case DASH: return "DASH";
                case YOUTUBE: return "YouTube";
                case FACEBOOK: return "Facebook";
                case INSTAGRAM: return "Instagram";
                case TIKTOK: return "TikTok";
                default: return "HTTP";
            }
        }
        
        private String getStatusDisplayName(DownloadStatus status) {
            switch (status) {
                case PENDING: return "En attente";
                case QUEUED: return "En file d'attente";
                case DOWNLOADING: return "Téléchargement en cours";
                case PAUSED: return "En pause";
                case RESUMING: return "Reprise en cours";
                case COMPLETED: return "Terminé";
                case FAILED: return "Échec";
                case CANCELLED: return "Annulé";
                case MERGING: return "Fusion en cours";
                case VERIFYING: return "Vérification en cours";
                default: return "Inconnu";
            }
        }
        
        private int getStatusColor(DownloadStatus status) {
            switch (status) {
                case DOWNLOADING:
                case RESUMING:
                    return itemView.getContext().getColor(R.color.status_downloading);
                case COMPLETED:
                    return itemView.getContext().getColor(R.color.status_completed);
                case FAILED:
                    return itemView.getContext().getColor(R.color.status_failed);
                case PAUSED:
                    return itemView.getContext().getColor(R.color.status_paused);
                case CANCELLED:
                    return itemView.getContext().getColor(R.color.status_cancelled);
                default:
                    return itemView.getContext().getColor(R.color.status_pending);
            }
        }
        
        private void updateActionButtons(DownloadStatus status) {
            switch (status) {
                case DOWNLOADING:
                case RESUMING:
                    pauseResumeButton.setImageResource(R.drawable.ic_pause);
                    pauseResumeButton.setVisibility(View.VISIBLE);
                    cancelButton.setVisibility(View.VISIBLE);
                    deleteButton.setVisibility(View.GONE);
                    break;
                case PAUSED:
                    pauseResumeButton.setImageResource(R.drawable.ic_play);
                    pauseResumeButton.setVisibility(View.VISIBLE);
                    cancelButton.setVisibility(View.VISIBLE);
                    deleteButton.setVisibility(View.GONE);
                    break;
                case COMPLETED:
                    pauseResumeButton.setVisibility(View.GONE);
                    cancelButton.setVisibility(View.GONE);
                    deleteButton.setVisibility(View.VISIBLE);
                    break;
                case FAILED:
                case CANCELLED:
                    pauseResumeButton.setVisibility(View.GONE);
                    cancelButton.setVisibility(View.GONE);
                    deleteButton.setVisibility(View.VISIBLE);
                    break;
                default:
                    pauseResumeButton.setVisibility(View.GONE);
                    cancelButton.setVisibility(View.VISIBLE);
                    deleteButton.setVisibility(View.GONE);
                    break;
            }
        }
        
        private String formatFileSize(long bytes) {
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
        
        private String formatSpeed(long bytesPerSecond) {
            if (bytesPerSecond < 1024) return bytesPerSecond + " B/s";
            if (bytesPerSecond < 1024 * 1024) return String.format("%.1f KB/s", bytesPerSecond / 1024.0);
            return String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0));
        }
    }
    
    public interface OnItemClickListener {
        void onItemClick(int position);
        void onPauseClick(int position);
        void onResumeClick(int position);
        void onCancelClick(int position);
        void onDeleteClick(int position);
    }
} 