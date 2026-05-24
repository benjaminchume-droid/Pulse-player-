package com.pulseplayer.music.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.pulseplayer.music.MainActivity
import com.pulseplayer.music.data.Song
import kotlinx.coroutines.*
import java.io.File

class PlaybackService : Service(), AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    // Playback state variables
    var currentQueue: List<Song> = emptyList()
    var currentSongIndex: Int = -1
    var isShuffleEnabled: Boolean = false
    var isRepeatEnabled: Boolean = false
    var isPrepared: Boolean = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionUpdateJob: Job? = null

    // Notification IDs
    private val NOTIFICATION_ID = 5153
    private val CHANNEL_ID = "PulsePlayerPlaybackChannel"

    interface PlaybackListener {
        fun onSongChanged(song: Song?)
        fun onPlaybackStatusChanged(isPlaying: Boolean)
        fun onPositionUpdate(positionMs: Long)
    }

    private val listeners = mutableListOf<PlaybackListener>()

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initMediaPlayer()
        createNotificationChannel()
        registerMediaActionsReceiver()
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setOnCompletionListener(this@PlaybackService)
            setOnPreparedListener(this@PlaybackService)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun addListener(listener: PlaybackListener) {
        listeners.add(listener)
        // Initial sync of state to listener
        getCurrentSong()?.let { listener.onSongChanged(it) }
        listener.onPlaybackStatusChanged(isPlaying())
    }

    fun removeListener(listener: PlaybackListener) {
        listeners.remove(listener)
    }

    // Controls
    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        currentQueue = songs
        currentSongIndex = startIndex
        if (currentSongIndex in currentQueue.indices) {
            playSong(currentQueue[currentSongIndex])
        }
    }

    fun playSong(song: Song) {
        if (!requestAudioFocus()) return
        isPrepared = false
        mediaPlayer?.reset()
        try {
            val uri = if (song.path.startsWith("content://")) {
                Uri.parse(song.path)
            } else {
                Uri.fromFile(File(song.path))
            }
            mediaPlayer?.setDataSource(applicationContext, uri)
            mediaPlayer?.prepareAsync()
            
            notifySongChanged(song)
        } catch (e: Exception) {
            e.printStackTrace()
            // Gracefully move to next song on loading fails
            skipNext()
        }
    }

    fun play() {
        if (isPrepared && mediaPlayer?.isPlaying == false) {
            if (requestAudioFocus()) {
                mediaPlayer?.start()
                notifyPlaybackStatus(true)
                startPositionTracker()
                showNotification()
            }
        }
    }

    fun pause() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
            notifyPlaybackStatus(false)
            stopPositionTracker()
            showNotification()
            // Stop foreground state but retain notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
        }
    }

    fun togglePlayPause() {
        if (isPlaying()) {
            pause()
        } else {
            play()
        }
    }

    fun skipNext() {
        if (currentQueue.isEmpty()) return
        if (isRepeatEnabled && currentSongIndex in currentQueue.indices) {
            playSong(currentQueue[currentSongIndex])
            return
        }
        if (isShuffleEnabled) {
            currentSongIndex = currentQueue.indices.random()
        } else {
            currentSongIndex = (currentSongIndex + 1) % currentQueue.size
        }
        if (currentSongIndex in currentQueue.indices) {
            playSong(currentQueue[currentSongIndex])
        }
    }

    fun skipPrevious() {
        if (currentQueue.isEmpty()) return
        if (isShuffleEnabled) {
            currentSongIndex = currentQueue.indices.random()
        } else {
            currentSongIndex = if (currentSongIndex - 1 < 0) currentQueue.size - 1 else currentSongIndex - 1
        }
        if (currentSongIndex in currentQueue.indices) {
            playSong(currentQueue[currentSongIndex])
        }
    }

    fun seekTo(positionMs: Long) {
        if (isPrepared) {
            mediaPlayer?.seekTo(positionMs.toInt())
            notifyPosition(positionMs)
        }
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying == true

    fun getCurrentSong(): Song? {
        return if (currentSongIndex in currentQueue.indices) currentQueue[currentSongIndex] else null
    }

    fun getDuration(): Long {
        return if (isPrepared) mediaPlayer?.duration?.toLong() ?: 0L else getCurrentSong()?.duration ?: 0L
    }

    fun getPosition(): Long {
        return if (isPrepared) mediaPlayer?.currentPosition?.toLong() ?: 0L else 0L
    }

    // MediaPlayer callbacks
    override fun onPrepared(mp: MediaPlayer?) {
        isPrepared = true
        mp?.start()
        notifyPlaybackStatus(true)
        startPositionTracker()
        showNotification()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        notifyPlaybackStatus(false)
        stopPositionTracker()
        skipNext()
    }

    // Position updates
    private fun startPositionTracker() {
        positionUpdateJob?.cancel()
        positionUpdateJob = serviceScope.launch {
            while (isActive) {
                if (isPlaying()) {
                    notifyPosition(getPosition())
                }
                delay(1000)
            }
        }
    }

    private fun stopPositionTracker() {
        positionUpdateJob?.cancel()
    }

    // Listeners notifications dispatchers
    private fun notifySongChanged(song: Song) {
        listeners.forEach { it.onSongChanged(song) }
    }

    private fun notifyPlaybackStatus(isPlaying: Boolean) {
        listeners.forEach { it.onPlaybackStatusChanged(isPlaying) }
    }

    private fun notifyPosition(positionMs: Long) {
        listeners.forEach { it.onPositionUpdate(positionMs) }
    }

    // Audio Focus Configuration
    private fun requestAudioFocus(): Boolean {
        if (audioManager == null) return false
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this)
                .build()
            audioFocusRequest = focusRequest
            audioManager!!.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager!!.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaPlayer?.setVolume(0.2f, 0.2f)
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
                play()
            }
        }
    }

    // Media Notification controls & channels setup
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pulse Audio Service Playback Status Indicator",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Enables controls inside device notification tray"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showNotification() {
        val currentSong = getCurrentSong() ?: return
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpenIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying()) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseActionText = if (isPlaying()) "Pause" else "Play"

        // Setup control pending broadcasts
        val prevPending = PendingIntent.getBroadcast(this, 1, Intent("ACTION_PREV"), PendingIntent.FLAG_IMMUTABLE)
        val togglePending = PendingIntent.getBroadcast(this, 2, Intent("ACTION_TOGGLE"), PendingIntent.FLAG_IMMUTABLE)
        val nextPending = PendingIntent.getBroadcast(this, 3, Intent("ACTION_NEXT"), PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(currentSong.title)
            .setContentText(currentSong.artist)
            .setSubText(currentSong.album)
            .setContentIntent(pendingOpenIntent)
            .setOngoing(isPlaying())
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setShowActionsInCompactView(0, 1, 2))
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPending)
            .addAction(playPauseIcon, playPauseActionText, togglePending)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private val actionsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "ACTION_PREV" -> skipPrevious()
                "ACTION_TOGGLE" -> togglePlayPause()
                "ACTION_NEXT" -> skipNext()
            }
        }
    }

    private fun registerMediaActionsReceiver() {
        val filter = IntentFilter().apply {
            addAction("ACTION_PREV")
            addAction("ACTION_TOGGLE")
            addAction("ACTION_NEXT")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(actionsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(actionsReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
        try {
            unregisterReceiver(actionsReceiver)
        } catch (e: Exception) { }
    }
}
