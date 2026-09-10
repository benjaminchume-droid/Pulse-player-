package com.pulseplayer.music.viewmodel

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.net.Uri
import android.os.IBinder
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pulseplayer.music.data.MusicDao
import com.pulseplayer.music.data.Playlist
import com.pulseplayer.music.data.RepeatMode
import com.pulseplayer.music.data.Song
import com.pulseplayer.music.lyrics.LyricsRepository
import com.pulseplayer.music.metadata.MetadataEnricher
import com.pulseplayer.music.metadata.MetadataResolver
import com.pulseplayer.music.recognition.RecognitionCoordinator
import com.pulseplayer.music.recognition.RecognitionStatus
import com.pulseplayer.music.service.PlaybackService
import com.pulseplayer.music.sources.SourceRegistry
import com.pulseplayer.music.sources.StreamResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaybackViewModel(
    private val context: Context,
    private val musicDao: MusicDao
) : ViewModel(), PlaybackService.PlaybackListener {

    private var playbackService: PlaybackService? = null
    @Volatile private var isBound = false
    private var pendingPlay: Pair<List<Song>, Song>? = null

    private val recognition = RecognitionCoordinator(context)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _isEnriching = MutableStateFlow(false)
    val isEnriching: StateFlow<Boolean> = _isEnriching

    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode

    private val _searchResults = MutableStateFlow<List<StreamResult>>(emptyList())
    val searchResults: StateFlow<List<StreamResult>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _crossfadeSeconds = MutableStateFlow(0)
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds

    private val _userLevel = MutableStateFlow(1)
    val userLevel: StateFlow<Int> = _userLevel
    private val _userXp = MutableStateFlow(0)
    val userXp: StateFlow<Int> = _userXp
    private val _listeningStreak = MutableStateFlow(0)
    val listeningStreak: StateFlow<Int> = _listeningStreak

    init {
        loadCachedData()
        bindPlaybackService()
        viewModelScope.launch(Dispatchers.IO) {
            try { recognition.initialize() } catch (_: Exception) {}
        }
    }

    private fun loadCachedData() {
        viewModelScope.launch {
            try {
                _songs.value = musicDao.getAllSongs()
                _playlists.value = musicDao.getAllPlaylists()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun scanDeviceAudio() {
        if (_isScanning.value) return
        _isScanning.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val fetched = mutableListOf<Song>()
            try {
                val cr: ContentResolver = context.contentResolver
                val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION
                )
                val cursor: Cursor? = cr.query(
                    collection, projection, selection, null,
                    "${MediaStore.Audio.Media.TITLE} ASC"
                )
                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        var title = c.getString(titleCol) ?: "Unnamed Track"
                        // Never keep pure MediaStore-style numeric titles
                        if (title.all { it.isDigit() } || title.matches(Regex("^\\d{5,}$"))) {
                            title = "Unnamed Track"
                        }
                        val path = Uri.withAppendedPath(collection, id.toString()).toString()
                        fetched.add(
                            Song(
                                id = id,
                                title = title,
                                artist = c.getString(artistCol) ?: "Unknown Artist",
                                album = c.getString(albumCol) ?: "Unknown Album",
                                duration = c.getLong(durationCol),
                                path = path,
                                sourceType = "local"
                            )
                        )
                    }
                }
                if (fetched.isNotEmpty()) {
                    musicDao.insertSongs(fetched)
                    _songs.value = musicDao.getAllSongs()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) { _isScanning.value = false }
            }
        }
    }

    /** Audio-ID while playing — once per song id until force. */
    private fun maybeRecognizeWhilePlaying(song: Song) {
        if (song.metadataEnriched || song.recognitionId.isNotBlank()) return
        if (song.path.isBlank() || song.path.startsWith("http")) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val key = "${song.id}:${song.path}"
                val result = recognition.recognizeWhilePlaying(key, song.path)
                if (result.status != RecognitionStatus.SUCCESS) return@launch
                if (result.errorMessage == "skipped_already_identified") return@launch
                val match = result.matches.firstOrNull() ?: return@launch
                if (!match.isUsableTitle()) return@launch
                val canonical = MetadataResolver.fromMatch(match) ?: return@launch
                musicDao.updateMetadata(
                    id = song.id,
                    title = canonical.title,
                    artist = canonical.artist,
                    album = canonical.album ?: song.album,
                    albumArtist = canonical.albumArtist ?: "",
                    year = canonical.year ?: 0,
                    genre = canonical.genre ?: song.genre,
                    coverUrl = canonical.artworkUrl ?: song.coverUrl,
                    titleProvenance = "automatic",
                    artistProvenance = "automatic",
                    albumProvenance = "automatic",
                    recognitionId = match.id,
                    lastRecognizedAt = System.currentTimeMillis()
                )
                if (song.lyrics.isBlank()) {
                    val lyrics = LyricsRepository.fetchLyrics(
                        canonical.title, canonical.artist, canonical.album.orEmpty(),
                        (song.duration / 1000).toInt()
                    )
                    if (lyrics != null) musicDao.updateLyrics(song.id, lyrics.plain, lyrics.synced)
                }
                recognition.markIdentified(key)
                val updated = musicDao.getSongById(song.id)
                withContext(Dispatchers.Main) {
                    _songs.value = musicDao.getAllSongs()
                    if (_currentSong.value?.id == song.id) _currentSong.value = updated
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun enrichSongMetadata(song: Song) {
        viewModelScope.launch {
            try {
                // Prefer audio recognition once, then text enrichers
                maybeRecognizeWhilePlaying(song)
                val meta = MetadataEnricher.enrich(context, song)
                if (!MetadataEnricher.isValidTitle(meta.title)) return@launch
                musicDao.updateMetadata(
                    song.id, meta.title, meta.artist, meta.album,
                    meta.albumArtist, meta.year, meta.genre, meta.coverUrl
                )
                if (song.lyrics.isBlank()) {
                    val lyrics = LyricsRepository.fetchLyrics(
                        meta.title, meta.artist, meta.album, (song.duration / 1000).toInt()
                    )
                    if (lyrics != null) musicDao.updateLyrics(song.id, lyrics.plain, lyrics.synced)
                }
                _songs.value = musicDao.getAllSongs()
                if (_currentSong.value?.id == song.id) {
                    _currentSong.value = musicDao.getSongById(song.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun enrichAllMetadata() {
        if (_isEnriching.value) return
        _isEnriching.value = true
        viewModelScope.launch {
            try {
                musicDao.getAllSongs().forEach { song ->
                    try { enrichSongMetadata(song) } catch (_: Exception) {}
                }
                _songs.value = musicDao.getAllSongs()
            } finally {
                _isEnriching.value = false
            }
        }
    }

    fun fetchLyricsFor(song: Song) {
        if (_lyricsLoading.value) return
        _lyricsLoading.value = true
        viewModelScope.launch {
            try {
                val result = LyricsRepository.fetchLyrics(
                    song.title, song.artist, song.album, (song.duration / 1000).toInt()
                )
                if (result != null) {
                    musicDao.updateLyrics(song.id, result.plain, result.synced)
                    _songs.value = musicDao.getAllSongs()
                    if (_currentSong.value?.id == song.id) {
                        _currentSong.value = musicDao.getSongById(song.id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _lyricsLoading.value = false
            }
        }
    }

    fun searchOnline(query: String, sourceId: String? = null) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        _isSearching.value = true
        viewModelScope.launch {
            try {
                val plugins = if (sourceId != null) {
                    SourceRegistry.all.filter { it.id == sourceId }
                } else SourceRegistry.all
                _searchResults.value = plugins.flatMap {
                    try { it.search(query) } catch (_: Exception) { emptyList() }
                }
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun playStreamResult(result: StreamResult) {
        if (!result.canStreamInApp || result.streamUrl.isBlank()) {
            if (result.externalUrl.isNotBlank()) {
                SourceRegistry.openExternal(context, result.externalUrl)
            }
            return
        }
        val song = Song(
            id = result.id.hashCode().toLong(),
            title = result.title,
            artist = result.artist,
            album = result.album,
            duration = result.durationMs,
            path = result.streamUrl,
            coverUrl = result.coverUrl,
            sourceType = "stream",
            sourceId = result.source
        )
        viewModelScope.launch { musicDao.insertSong(song); _songs.value = musicDao.getAllSongs() }
        playSong(listOf(song), song)
    }

    fun createPlaylist(name: String, description: String = "", songIds: List<Long> = emptyList()) {
        viewModelScope.launch {
            musicDao.insertPlaylist(
                Playlist(
                    id = System.currentTimeMillis().toString(),
                    name = name,
                    description = description
                )
            )
            _playlists.value = musicDao.getAllPlaylists()
        }
    }

    fun playPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            val tracks = musicDao.getAllSongs()
            if (tracks.isNotEmpty()) playSong(tracks, tracks.first())
        }
    }

    fun playAlbum(album: String) {
        viewModelScope.launch {
            val tracks = musicDao.getSongsByAlbum(album)
            if (tracks.isNotEmpty()) playSong(tracks, tracks.first())
        }
    }

    private fun bindPlaybackService() {
        if (isBound) return
        try {
            context.bindService(
                Intent(context, PlaybackService::class.java),
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? PlaybackService.LocalBinder ?: return
            playbackService = binder.getService()
            playbackService?.addListener(this@PlaybackViewModel)
            isBound = true
            playbackService?.setCrossfadeSeconds(_crossfadeSeconds.value)
            pendingPlay?.let { (list, song) ->
                pendingPlay = null
                val index = list.indexOf(song)
                playbackService?.setQueue(list, if (index != -1) index else 0)
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            try { playbackService?.removeListener(this@PlaybackViewModel) } catch (_: Exception) {}
            playbackService = null
            isBound = false
        }
    }

    fun playSong(songsList: List<Song>, songToPlay: Song) {
        _currentSong.value = songToPlay
        _playbackPosition.value = 0L
        val svc = playbackService
        if (svc != null && isBound) {
            val index = songsList.indexOf(songToPlay)
            svc.setQueue(songsList, if (index != -1) index else 0)
        } else {
            pendingPlay = songsList to songToPlay
            bindPlaybackService()
        }
        maybeRecognizeWhilePlaying(songToPlay)
        viewModelScope.launch {
            try {
                musicDao.incrementPlayCount(songToPlay.id)
                _userXp.value += 150
                if (_userXp.value >= 1000) {
                    _userLevel.value += 1
                    _userXp.value -= 1000
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun queuePlayNext(song: Song) { playbackService?.queuePlayNext(song) }
    fun cycleRepeatMode() {
        playbackService?.cycleRepeatMode()?.let { _repeatMode.value = it }
    }
    fun setSleepTimerMinutes(minutes: Int) { playbackService?.setSleepTimerMinutes(minutes) }
    fun setCrossfadeSeconds(sec: Int) {
        _crossfadeSeconds.value = sec.coerceIn(0, 25)
        playbackService?.setCrossfadeSeconds(sec)
    }
    fun togglePlayPause() { playbackService?.togglePlayPause() }
    fun skipNext() { playbackService?.skipNext() }
    fun skipPrevious() { playbackService?.skipPrevious() }
    fun seekTo(positionMs: Long) { playbackService?.seekTo(positionMs) }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val next = !song.isFavorite
            musicDao.updateFavorite(song.id, next)
            _songs.value = musicDao.getAllSongs()
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = _currentSong.value?.copy(isFavorite = next)
            }
        }
    }

    override fun onSongChanged(song: Song?) {
        _currentSong.value = song
        if (song != null) maybeRecognizeWhilePlaying(song)
    }
    override fun onPlaybackStatusChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }
    override fun onPositionUpdate(positionMs: Long) { _playbackPosition.value = positionMs }
    override fun onRepeatModeChanged(mode: RepeatMode) { _repeatMode.value = mode }

    override fun onCleared() {
        super.onCleared()
        try {
            if (isBound) {
                playbackService?.removeListener(this)
                context.unbindService(serviceConnection)
                isBound = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        playbackService = null
    }
}

class PlaybackViewModelFactory(
    private val context: Context,
    private val musicDao: MusicDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaybackViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaybackViewModel(context, musicDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
