package rjv.mg.myidm.domain.model;

public enum DownloadType {
    HTTP_HTTPS,     // Téléchargement HTTP/HTTPS standard
    TORRENT,        // Téléchargement torrent
    FTP,            // Téléchargement FTP
    SFTP,           // Téléchargement SFTP
    M3U8,           // Flux HLS (M3U8)
    DASH,           // Flux MPEG-DASH
    YOUTUBE,        // Vidéo YouTube
    FACEBOOK,       // Vidéo Facebook
    INSTAGRAM,      // Vidéo Instagram
    TWITTER,        // Vidéo Twitter
    TIKTOK,         // Vidéo TikTok
    CUSTOM          // Type personnalisé
} 