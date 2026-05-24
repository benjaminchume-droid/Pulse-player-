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
    private var isBound = false

    // State Flows backing the beautiful reactive Jetpack Compose screen loop
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

    // Onboarding and User stats simulation matching web
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
            val cachedSongs = musicDao.getAllSongs()
            _songs.value = cachedSongs
            _playlists.value = musicDao.getAllPlaylists()
        }
    }

    // MediaStore content scanning core mechanics
    fun scanDeviceAudio() {
        if (_isScanning.value) return
        _isScanning.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val fetchedSongs = mutableListOf<Song>()
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
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unnamed Track"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val album = c.getString(albumCol) ?: "Unknown Album"
                    val duration = c.getLong(durationCol)
                    val path = c.getString(dataCol) ?: ""

                    if (path.isNotEmpty()) {
                        fetchedSongs.add(
                            Song(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                duration = duration,
                                path = path
                            )
                        )
                    }
                }
            }

            if (fetchedSongs.isNotEmpty()) {
                musicDao.insertSongs(fetchedSongs)
                _songs.value = musicDao.getAllSongs()
            }
            
            withContext(Dispatchers.Main) {
                _isScanning.value = false
            }
        }
    }

    // Playback Service bindings
    private fun bindPlaybackService() {
        val intent = Intent(context, PlaybackService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlaybackService.LocalBinder
            playbackService = binder.getService()
            playbackService?.addListener(this@PlaybackViewModel)
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService?.removeListener(this@PlaybackViewModel)
            playbackService = null
            isBound = false
        }
    }

    // Controls bridges
    fun playSong(songsList: List<Song>, songToPlay: Song) {
        val index = songsList.indexOf(songToPlay)
        playbackService?.setQueue(songsList, if (index != -1) index else 0)
        
        // Dynamic Reward Metric integration
        viewModelScope.launch {
            musicDao.incrementPlayCount(songToPlay.id)
            incrementExperiencePoints()
        }
    }

    fun togglePlayPause() {
        playbackService?.togglePlayPause()
    }

    fun skipNext() {
        playbackService?.skipNext()
    }

    fun skipPrevious() {
        playbackService?.skipPrevious()
    }

    fun seekTo(positionMs: Long) {
        playbackService?.seekTo(positionMs)
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val nextFav = !song.isFavorite
            musicDao.updateFavorite(song.id, nextFav)
            _songs.value = musicDao.getAllSongs()
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = _currentSong.value?.copy(isFavorite = nextFav)
            }
        }
    }

    fun createPlaylist(name: String, description: String = "", songIds: List<Long> = emptyList()) {
        viewModelScope.launch {
            val playlist = Playlist(name = name, description = description, songIds = songIds)
            musicDao.insertPlaylist(playlist)
            _playlists.value = musicDao.getAllPlaylists()
        }
    }

    // Gamification level progress tracker matching React specs
    private fun incrementExperiencePoints() {
        _userXp.value += 150
        if (_userXp.value >= 1000) {
            _userLevel.value += 1
            _userXp.value -= 1000
        }
        _listeningStreak.value = (_listeningStreak.value + 1).coerceAtMost(30)
    }

    // Callback listeners implementing PlaybackService notifications standard
    override fun onSongChanged(song: Song?) {
        _currentSong.value = song
    }

    override fun onPlaybackStatusChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    override fun onPositionUpdate(positionMs: Long) {
        _playbackPosition.value = positionMs
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            playbackService?.removeListener(this)
            context.unbindService(serviceConnection)
            isBound = false
        }
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
