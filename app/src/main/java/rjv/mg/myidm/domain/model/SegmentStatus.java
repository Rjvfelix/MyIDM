package rjv.mg.myidm.domain.model;

public enum SegmentStatus {
    PENDING,        // En attente
    DOWNLOADING,    // En cours de téléchargement
    PAUSED,         // En pause
    COMPLETED,      // Terminé avec succès
    FAILED,         // Échec
    CANCELLED,      // Annulé
    STALLED,        // Bloqué (timeout)
    VERIFYING       // Vérification en cours
} 