package rjv.mg.myidm.domain.model;

public enum DownloadStatus {
    PENDING,        // En attente dans la file
    QUEUED,         // Dans la file d'attente
    DOWNLOADING,    // En cours de téléchargement
    PAUSED,         // En pause
    RESUMING,       // Reprise en cours
    COMPLETED,      // Terminé avec succès
    FAILED,         // Échec
    CANCELLED,      // Annulé par l'utilisateur
    MERGING,        // Fusion des segments en cours
    VERIFYING,      // Vérification d'intégrité
    ENCRYPTING,     // Chiffrement en cours
    DECRYPTING      // Déchiffrement en cours
} 