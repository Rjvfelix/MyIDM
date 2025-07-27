# MyIDM - Advanced Android Download Manager

Un gestionnaire de téléchargements Android avancé inspiré de 1DM (IDM+), avec navigateur intégré, téléchargements multi-segments et support torrent.

## 🚀 Fonctionnalités

### ✅ Implémentées
- ✅ Architecture MVVM + Clean Architecture avec Dagger Hilt
- ✅ Base de données Room complète avec entités et DAOs
- ✅ Téléchargement multi-segments (jusqu'à 32) avec MultiThreadDownloader
- ✅ Support torrent avec libtorrent4j et TorrentDownloader
- ✅ Détection automatique de vidéos avec VideoDetector
- ✅ Gestion des erreurs et retry automatique
- ✅ Pause/reprise des téléchargements
- ✅ Vérification d'intégrité (checksum MD5)
- ✅ Service de téléchargement en arrière-plan (Foreground Service)
- ✅ Interface utilisateur Material Design 3
- ✅ Navigation par onglets (Téléchargements, Navigateur, Paramètres)
- ✅ Gestion des permissions Android 11+
- ✅ FileProvider pour le partage sécurisé
- ✅ Exemples d'utilisation complets

### 📋 Prêtes pour Développement
- 📋 Fragments pour navigateur et paramètres
- 📋 Adapter RecyclerView pour la liste des téléchargements
- 📋 Dialogues d'ajout de téléchargement
- 📋 Navigateur intégré avec blocage de pubs
- 📋 Planificateur de téléchargements
- 📋 Chiffrement AES-256
- 📋 Conversion automatique M3U8 → MP4
- 📋 Fonctionnalités premium (30 téléchargements simultanés)

## 🏗️ Architecture

### Structure du Projet
```
app/src/main/java/rjv/mg/myidm/
├── data/
│   ├── database/
│   │   ├── DownloadDatabase.java
│   │   ├── entity/
│   │   │   ├── DownloadEntity.java
│   │   │   ├── DownloadSegmentEntity.java
│   │   │   └── BrowserHistoryEntity.java
│   │   ├── dao/
│   │   │   ├── DownloadDao.java
│   │   │   ├── DownloadSegmentDao.java
│   │   │   └── BrowserHistoryDao.java
│   │   └── converter/
│   │       ├── DateConverter.java
│   │       └── DownloadStatusConverter.java
├── domain/
│   ├── model/
│   │   ├── DownloadStatus.java
│   │   ├── DownloadType.java
│   │   └── SegmentStatus.java
│   ├── downloader/
│   │   ├── DownloadManager.java
│   │   ├── MultiThreadDownloader.java
│   │   └── TorrentDownloader.java
│   └── browser/
│       └── VideoDetector.java
├── ui/
│   ├── MainActivity.java
│   └── download/
│       └── DownloadsFragment.java
├── service/
│   └── DownloadService.java
├── viewmodel/
│   └── MainViewModel.java
├── di/
│   └── AppModule.java
└── MyIDMApplication.java
```

### Technologies Utilisées
- **Java 11** - Langage principal
- **Android SDK API 26+** - Support Android 8.0+
- **Material Design 3** - Interface moderne
- **Room Database** - Persistance des données
- **Dagger Hilt** - Injection de dépendances
- **LiveData** - Réactivité UI
- **WorkManager** - Tâches en arrière-plan
- **libtorrent4j** - Support torrent
- **ExoPlayer** - Lecture multimédia
- **Retrofit + OkHttp** - Requêtes réseau

## 📱 Interface Utilisateur

### Navigation
- **Onglet Téléchargements** : Liste des téléchargements avec filtres
- **Onglet Navigateur** : Navigateur intégré avec détection de vidéos
- **Onglet Paramètres** : Configuration de l'application

### Fonctionnalités UI
- **Filtres par statut** : Tous, En cours, En pause, Terminés, Échecs
- **Pull-to-refresh** : Actualisation de la liste
- **FAB** : Ajout rapide de téléchargements
- **Notifications persistantes** : Suivi des téléchargements
- **États vides** : Messages informatifs

## 🔧 Configuration

### Permissions Requises
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

### Dépendances Principales
```gradle
// Architecture Components
implementation 'androidx.room:room-runtime:2.6.1'
implementation 'androidx.work:work-runtime:2.9.1'
implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'

// Dependency Injection
implementation 'com.google.dagger:hilt-android:2.50'

// Torrent Support
implementation 'org.libtorrent4j:libtorrent4j:2.0.9'

// Media & Network
implementation 'androidx.media3:media3-exoplayer:1.2.1'
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
```

## 🚀 Utilisation

### Ajouter un Téléchargement
```java
// Via URL directe
DownloadEntity download = downloadManager.createDownloadFromUrl(url);
downloadManager.startDownload(download);

// Via navigateur intégré
videoDetector.detectVideosInWebView(webView);
```

### Gérer les Téléchargements
```java
// Pause/Reprise
downloadManager.pauseDownload(downloadId);
downloadManager.resumeDownload(downloadId);

// Annulation
downloadManager.cancelDownload(downloadId);

// Suppression
downloadManager.deleteDownload(downloadId);
```

### Monitoring
```java
// Observer les téléchargements
viewModel.getAllDownloads().observe(this, downloads -> {
    // Mettre à jour l'UI
});

// Observer les erreurs
viewModel.getError().observe(this, error -> {
    // Afficher l'erreur
});
```

## 🔒 Sécurité

### Permissions Minimales
- Stockage externe (téléchargements)
- Internet (téléchargements)
- Notifications (suivi)
- Service en premier plan (téléchargements en arrière-plan)

### Chiffrement (Prévu)
- AES-256 pour les liens sensibles
- Stockage local sécurisé
- Authentification biométrique

## 📊 Performance

### Optimisations
- **Multi-segments** : Jusqu'à 32 segments simultanés
- **File d'attente intelligente** : Priorité et gestion des ressources
- **Cache optimisé** : Réduction des requêtes réseau
- **Gestion mémoire** : Nettoyage automatique des ressources

### Limites
- **Gratuit** : 5 téléchargements simultanés
- **Premium** : 30 téléchargements simultanés
- **Segments** : 8-32 selon la taille du fichier

## 🛠️ Développement

### Compilation
```bash
# Cloner le projet
git clone <repository-url>
cd MyIDM

# Compiler
./gradlew assembleDebug

# Installer sur l'appareil
./gradlew installDebug
```

### Tests
```bash
# Tests unitaires
./gradlew test

# Tests d'intégration
./gradlew connectedAndroidTest
```

## 📈 Roadmap

### Version 1.0 (Actuelle)
- ✅ Architecture de base
- ✅ Téléchargements HTTP/HTTPS
- ✅ Support torrent
- ✅ Interface utilisateur

### Version 1.1 (Prochaine)
- 📋 Navigateur intégré
- 📋 Détection de vidéos avancée
- 📋 Conversion M3U8 → MP4
- 📋 Planificateur de téléchargements

### Version 1.2 (Future)
- 📋 Chiffrement AES-256
- 📋 Authentification biométrique
- 📋 Fonctionnalités premium
- 📋 Synchronisation cloud

## 🤝 Contribution

Les contributions sont les bienvenues ! Veuillez :

1. Fork le projet
2. Créer une branche feature (`git checkout -b feature/AmazingFeature`)
3. Commit vos changements (`git commit -m 'Add AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrir une Pull Request

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus de détails.

## 🙏 Remerciements

- **1DM (IDM+)** pour l'inspiration
- **libtorrent4j** pour le support torrent
- **Material Design** pour l'interface
- **Android Jetpack** pour l'architecture

---

**MyIDM** - Le gestionnaire de téléchargements Android le plus avancé ! 🚀 