package com.pulseplayer.music.viewmodel

import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.os.IBinder
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pulseplayer.music.data.MusicDao
import com.pulseplayer.music.data.Playlist
import com.pulseplayer.music.data.Song
import com.pulseplayer.music.lyrics.LyricsRepository
import com.pulseplayer.music.metadata.MetadataEnricher
import com.pulseplayer.music.service.PlaybackService
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

    private val _userLevel = MutableStateFlow(1)
    val userLevel: StateFlow<Int> = _userLevel

    private val _userXp = MutableStateFlow(0)
    val userXp: StateFlow<Int> = _userXp

    private val _listeningStreak = MutableStateFlow(0)
    val listeningStreak: StateFlow<Int> = _listeningStreak

    init {
        loadCachedData()
        bindPlaybackService()
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
            val fetchedSongs = mutableListOf<Song>()
            try {
                val contentResolver: ContentResolver = context.contentResolver
                val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
                val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.DATA
                )

                val cursor: Cursor? = contentResolver.query(uri, projection, selection, null, sortOrder)
                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                    while (c.moveToNext()) {
                        val path = c.getString(dataCol) ?: ""
                        if (path.isEmpty()) continue
                        fetchedSongs.add(
                            Song(
                                id = c.getLong(idCol),
                                title = c.getString(titleCol) ?: "Unnamed Track",
                                artist = c.getString(artistCol) ?: "Unknown Artist",
                                album = c.getString(albumCol) ?: "Unknown Album",
                                duration = c.getLong(durationCol),
                                path = path
                            )
                        )
                    }
                }

                if (fetchedSongs.isNotEmpty()) {
                    musicDao.insertSongs(fetchedSongs)
                    _songs.value = musicDao.getAllSongs()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) { _isScanning.value = false }
            }
        }
    }

    /** Enrich one song from embedded tags + MusicBrainz */
    fun enrichSongMetadata(song: Song) {
        viewModelScope.launch {
            try {
                val meta = MetadataEnricher.enrich(context, song)
                musicDao.updateMetadata(
                    id = song.id,
                    title = meta.title,
                    artist = meta.artist,
                    album = meta.album,
                    albumArtist = meta.albumArtist,
                    year = meta.year,
                    genre = meta.genre,
                    coverUrl = meta.coverUrl
                )
                _songs.value = musicDao.getAllSongs()
                if (_currentSong.value?.id == song.id) {
                    _currentSong.value = musicDao.getSongById(song.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Batch enrich library (skips already enriched) */
    fun enrichAllMetadata() {
        if (_isEnriching.value) return
        _isEnriching.value = true
        viewModelScope.launch {
            try {
                val list = musicDao.getAllSongs().filter { !it.metadataEnriched }
                list.forEach { song ->
                    try {
                        val meta = MetadataEnricher.enrich(context, song)
                        musicDao.updateMetadata(
                            id = song.id,
                            title = meta.title,
                            artist = meta.artist,
                            album = meta.album,
                            albumArtist = meta.albumArtist,
                            year = meta.year,
                            genre = meta.genre,
                            coverUrl = meta.coverUrl
                        )
                    } catch (_: Exception) {}
                }
                _songs.value = musicDao.getAllSongs()
            } finally {
                _isEnriching.value = false
            }
        }
    }

    /** Fetch lyrics for a song and store in DB */
    fun fetchLyricsFor(song: Song) {
        if (_lyricsLoading.value) return
        _lyricsLoading.value = true
        viewModelScope.launch {
            try {
                val result = LyricsRepository.fetchLyrics(
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    durationSec = (song.duration / 1000).toInt()
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

    private fun bindPlaybackService() {
        if (isBound) return
        try {
            val intent = Intent(context, PlaybackService::class.java)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                val binder = service as? PlaybackService.LocalBinder ?: return
                playbackService = binder.getService()
                playbackService?.addListener(this@PlaybackViewModel)
                isBound = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            try { playbackService?.removeListener(this@PlaybackViewModel) } catch (_: Exception) {}
            playbackService = null
            isBound = false
        }
    }

    fun playSong(songsList: List<Song>, songToPlay: Song) {
        val index = songsList.indexOf(songToPlay)
        playbackService?.setQueue(songsList, if (index != -1) index else 0)
        viewModelScope.launch {
            try {
                musicDao.incrementPlayCount(songToPlay.id)
                incrementExperiencePoints()
                // Auto-fetch lyrics if missing
                val fresh = musicDao.getSongById(songToPlay.id)
                if (fresh != null && fresh.lyrics.isBlank() && fresh.syncedLyrics.isBlank()) {
                    fetchLyricsFor(fresh)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun togglePlayPause() { playbackService?.togglePlayPause() }
    fun skipNext() { playbackService?.skipNext() }
    fun skipPrevious() { playbackService?.skipPrevious() }
    fun seekTo(positionMs: Long) { playbackService?.seekTo(positionMs) }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            try {
                val nextFav = !song.isFavorite
                musicDao.updateFavorite(song.id, nextFav)
                _songs.value = musicDao.getAllSongs()
                if (_currentSong.value?.id == song.id) {
                    _currentSong.value = _currentSong.value?.copy(isFavorite = nextFav)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createPlaylist(name: String, description: String = "", songIds: List<Long> = emptyList()) {
        viewModelScope.launch {
            try {
                musicDao.insertPlaylist(Playlist(name = name, description = description, songIds = songIds))
                _playlists.value = musicDao.getAllPlaylists()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun incrementExperiencePoints() {
        _userXp.value += 150
        if (_userXp.value >= 1000) {
            _userLevel.value += 1
            _userXp.value -= 1000
        }
        _listeningStreak.value = (_listeningStreak.value + 1).coerceAtMost(30)
    }

    override fun onSongChanged(song: Song?) { _currentSong.value = song }
    override fun onPlaybackStatusChanged(isPlaying: Boolean) { _isPlaying.value = isPlaying }
    override fun onPositionUpdate(positionMs: Long) { _playbackPosition.value = positionMs }

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
        throw IllegalArgumentException("Unknown ViewModel class representation")
    }
}
