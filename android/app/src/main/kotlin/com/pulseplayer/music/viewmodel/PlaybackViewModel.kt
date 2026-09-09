package com.pulseplayer.music.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pulseplayer.music.data.MusicDao
import com.pulseplayer.music.data.Song
import com.pulseplayer.music.web.WebDownloadManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Represents the current state of a download operation.
 */
sealed class DownloadState {
    object Idle : DownloadState()
    object Queued : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    object Writing : DownloadState()
    object Completed : DownloadState()
    data class Failed(val message: String? = null) : DownloadState()
}

/**
 * ViewModel managing playback state and download operations.
 */
class PlaybackViewModel(
    application: Application,
    private val musicDao: MusicDao,
    private val webDownloadManager: WebDownloadManager
) : AndroidViewModel(application) {

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _downloadedSongs = MutableStateFlow<List<Song>>(emptyList())
    val downloadedSongs: StateFlow<List<Song>> = _downloadedSongs.asStateFlow()

    fun setCurrentSong(song: Song?) {
        _currentSong.value = song
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun play() {
        _isPlaying.value = true
    }

    fun pause() {
        _isPlaying.value = false
    }

    /**
     * Initiates a download for the specified song with progress callbacks.
     * Runs in a background coroutine using viewModelScope.
     */
    fun downloadTrack(song: Song) {
        viewModelScope.launch {
            try {
                _downloadState.value = DownloadState.Queued

                webDownloadManager.downloadTrack(
                    song = song,
                    onProgress = { progress ->
                        _downloadState.value = DownloadState.Downloading(progress)
                    },
                    onWrite = {
                        _downloadState.value = DownloadState.Writing
                    },
                    onComplete = { downloadedSong ->
                        _downloadState.value = DownloadState.Completed
                        // Automatically add to local database
                        musicDao.insertSong(downloadedSong)
                        // Refresh downloaded songs list
                        refreshDownloadedSongs()
                    },
                    onError = { error ->
                        _downloadState.value = DownloadState.Failed(error.message)
                    }
                )
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Failed(e.message)
            }
        }
    }

    /**
     * Resets the download state to idle.
     */
    fun resetDownloadState() {
        _downloadState.value = DownloadState.Idle
    }

    /**
     * Loads downloaded songs from the database.
     */
    fun loadDownloadedSongs() {
        viewModelScope.launch {
            musicDao.getDownloadedSongs().collect { songs ->
                _downloadedSongs.value = songs
            }
        }
    }

    /**
     * Refreshes the downloaded songs list.
     */
    private fun refreshDownloadedSongs() {
        viewModelScope.launch {
            musicDao.getDownloadedSongs().collect { songs ->
                _downloadedSongs.value = songs
            }
        }
    }

    /**
     * Returns a Flow of all downloaded songs.
     */
    fun getDownloadedSongsFlow(): Flow<List<Song>> {
        return musicDao.getDownloadedSongs()
    }
}
