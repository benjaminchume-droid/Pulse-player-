# Automatic Music Metadata Filling - Implementation Guide

## Overview

This document describes the complete, production-grade Automatic Music Metadata Filling system for Pulse Player Android.

## Architecture

### Core Components

```
recognition/
├── RecognitionEngine.kt          # Main interface
├── ShazamKitRecognitionEngine.kt # ShazamKit implementation
└── RecognitionSession.kt         # Microphone session management

metadata/
├── MetadataResolver.kt           # Normalize recognition results
├── MetadataPolicy.kt             # User preference enforcement
├── MetadataWriter.kt             # Write to audio files
└── CanonicalTrackMetadata.kt     # Standardized metadata model

scanning/
├── LibraryScanner.kt             # Background library scanning
├── MetadataScanWorker.kt         # WorkManager integration
├── ScanQueue.kt                  # Persistent scan queue
└── ScanRepository.kt             # Data access layer

history/
├── MetadataHistory.kt            # Change history
└── MetadataRollback.kt           # Rollback functionality
```

## Database Schema

### Tables Created (v2)

1. **metadata_scan_jobs** - Scan job tracking
2. **metadata_scan_items** - Individual file processing
3. **metadata_matches** - Recognition results
4. **metadata_changes** - Change history
5. **metadata_settings** - User preferences
6. **artwork_cache** - Cached artwork

## Implementation Status

### ✅ Completed

1. **Database Layer**
   - All entities created
   - DAOs implemented
   - Type converters
   - Migration from v1 to v2

2. **Recognition Engine**
   - RecognitionEngine interface
   - ShazamKitRecognitionEngine
   - Audio sample extraction
   - Confidence evaluation
   - Microphone session support

3. **Metadata Processing**
   - MetadataResolver
   - MetadataPolicy
   - MetadataWriter (MP3/M4A/FLAC)
   - Artwork embedding
   - Safe file modification

4. **Settings UI**
   - Complete settings screen
   - All user preferences
   - Real-time updates

5. **Dependencies**
   - ShazamKit Android SDK
   - JAudioTagger
   - WorkManager
   - Media3

### 🔄 In Progress

1. **Library Scanner**
   - Background worker
   - Progress tracking
   - Pause/resume
   - Error handling

2. **Playback Integration**
   - Recognition during playback
   - Cooldown mechanism
   - State management

3. **ViewModel**
   - MetadataViewModel
   - Scan status tracking
   - User interactions

4. **Confirmation UI**
   - Ambiguous match dialog
   - Always-confirm mode
   - Change preview

### 📋 Remaining

1. **Complete LibraryScanner** - Full background scanning implementation
2. **MetadataViewModel** - Bridge between UI and recognition
3. **Scan Status Screen** - Real-time progress UI
4. **History Screen** - View and rollback changes
5. **Permissions** - Microphone permission handling
6. **Testing** - Unit and integration tests

## Integration Steps

### 1. Add to MainActivity

```kotlin
class MainActivity : ComponentActivity() {
    private val musicDatabase by lazy { MusicDatabase.getDatabase(this) }
    private val metadataDao by lazy { musicDatabase.metadataDao() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize recognition engine
        val recognitionEngine = ShazamKitRecognitionEngine(
            context = this,
            apiKey = "YOUR_SHAZAM_API_KEY"
        )
        
        // Initialize metadata writer
        val metadataWriter = MetadataWriter(this)
    }
}
```

### 2. Add Settings Route

```kotlin
composable("settings/metadata") {
    val settings by metadataDao.getSettings().collectAsState(initial = MetadataSettings())
    
    MetadataSettingsScreen(
        settings = settings,
        onSettingsChange = { newSettings ->
            viewModelScope.launch {
                metadataDao.updateSettings(newSettings)
            }
        },
        onApply = { navController.popBackStack() }
    )
}
```

### 3. Add to PlaybackViewModel

```kotlin
class PlaybackViewModel(
    private val recognitionEngine: RecognitionEngine,
    private val metadataWriter: MetadataWriter,
    private val metadataDao: MetadataDao
) : ViewModel() {
    
    fun onSongStart(song: Song) {
        viewModelScope.launch {
            val settings = metadataDao.getSettingsSync()
            if (settings?.automaticMetadataFillingEnabled == true &&
                settings.recognitionMode == RecognitionMode.ONLY_WHEN_PLAYING) {
                recognizeCurrentSong(song)
            }
        }
    }
    
    private suspend fun recognizeCurrentSong(song: Song) {
        val result = recognitionEngine.recognizeFromFile(song.filePath)
        // Process result and apply metadata
    }
}
```

## ShazamKit Setup

### 1. Get API Key

1. Visit https://www.shazam.com/myshazam
2. Create developer account
3. Register your app
4. Get API key

### 2. Add API Key

Add to `local.properties`:
```
shazam.api.key=YOUR_API_KEY
```

Or use BuildConfig in `build.gradle`:
```groovy
buildTypes {
    release {
        buildConfigField "String", "SHAZAM_API_KEY", '"YOUR_API_KEY"'
    }
}
```

## Testing Checklist

### Unit Tests
- [ ] MetadataPolicy determines correct changes
- [ ] MetadataResolver normalizes correctly
- [ ] Confidence evaluation works
- [ ] File validation works

### Integration Tests
- [ ] Recognition from file works
- [ ] Metadata writing works
- [ ] Artwork embedding works
- [ ] MediaStore re-indexing works

### UI Tests
- [ ] Settings screen saves correctly
- [ ] Scan status updates
- [ ] Confirmation dialogs work
- [ ] History shows changes

### Manual Tests
- [ ] Test with real MP3 files
- [ ] Test with M4A files
- [ ] Test with FLAC files
- [ ] Test with missing metadata
- [ ] Test with existing metadata
- [ ] Test microphone recognition
- [ ] Test background scanning
- [ ] Test pause/resume
- [ ] Test rollback

## Production Deployment

### 1. Configure ProGuard

Add to `proguard-rules.pro`:
```
# ShazamKit
-keep class com.shazam.shazamkit.** { *; }

# JAudioTagger
-keep class org.jaudiotagger.** { *; }

# Metadata entities
-keep class com.pulseplayer.music.data.Metadata** { *; }
```

### 2. Enable Minification

Update `build.gradle`:
```groovy
buildTypes {
    release {
        minifyEnabled true
        shrinkResources true
    }
}
```

### 3. Permissions

Add to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="28" />
```

### 4. Privacy Policy

Update privacy policy to include:
- Microphone usage for recognition
- No permanent recording of audio
- Metadata enrichment from third-party services

## Performance Considerations

1. **Memory**: Process one file at a time, don't load entire files
2. **Network**: Cache recognition results, batch artwork downloads
3. **Battery**: Use WorkManager with appropriate constraints
4. **Storage**: Limit artwork cache size, cleanup old entries

## Error Handling

All operations must handle:
- Network failures
- Permission denials
- File corruption
- Unsupported formats
- Recognition failures
- Write failures

Never claim success unless operation actually succeeded.

## Legal Considerations

1. **ShazamKit**: Use only official API, no scraping
2. **Spotify**: Don't use Spotify APIs for this feature
3. **Artwork**: Only use artwork from authorized sources
4. **Privacy**: Don't store raw microphone audio
5. **Copyright**: Respect content ownership

## Next Steps

1. Complete LibraryScanner implementation
2. Add MetadataViewModel
3. Create scan status UI
4. Add history/rollback UI
5. Implement confirmation dialogs
6. Add comprehensive tests
7. Performance optimization
8. Production deployment

---

**Status**: Core architecture complete, integration in progress
**Version**: 1.0.0-alpha
**Last Updated**: 2026-09-09
