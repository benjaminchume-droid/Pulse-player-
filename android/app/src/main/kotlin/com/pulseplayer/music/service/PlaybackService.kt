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
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.pulseplayer.music.MainActivity
import com.pulseplayer.music.data.Song
import kotlinx.coroutines.*
import java.io.File

class PlaybackService : Service(), AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var mediaSession: MediaSessionCompat? = null

    // Playback state variables
    var currentQueue: List<Song> = emptyList()
    var currentSongIndex: Int = -1
    var isShuffleEnabled: Boolean = false
    var isRepeatEnabled: Boolean = false
    @Volatile var isPrepared: Boolean = false

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
        initMediaSession()
        createNotificationChannel()
        registerMediaActionsReceiver()
        // Immediately show a placeholder notification so startForegroundService is satisfied
        showPlaceholderNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always ensure we are in the foreground as soon as possible
        if (getCurrentSong() != null) {
            showNotification()
        } else {
            showPlaceholderNotification()
        }
        return START_STICKY
    }

    private fun initMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setOnCompletionListener(this@PlaybackService)
            setOnPreparedListener(this@PlaybackService)
            setOnErrorListener(this@PlaybackService)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "PulsePlayerSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { play() }
                override fun onPause() { pause() }
                override fun onSkipToNext() { skipNext() }
                override fun onSkipToPrevious() { skipPrevious() }
                override fun onSeekTo(pos: Long) { seekTo(pos) }
                override fun onStop() { pause() }
            })
            isActive = true
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun addListener(listener: PlaybackListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
        getCurrentSong()?.let { listener.onSongChanged(it) }
        listener.onPlaybackStatusChanged(isPlaying())
    }

    fun removeListener(listener: PlaybackListener) {
        listeners.remove(listener)
    }

    // Controls
    fun setQueue(songs: List<Song>, startIndex: Int = 0) {
        currentQueue = songs
        currentSongIndex = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
        if (currentSongIndex in currentQueue.indices) {
            playSong(currentQueue[currentSongIndex])
        }
    }

    fun playSong(song: Song) {
        // Ensure service is running as foreground
        try {
            val intent = Intent(applicationContext, PlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(intent)
            } else {
                applicationContext.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!requestAudioFocus()) return
        isPrepared = false

        try {
            mediaPlayer?.reset()
            val uri = when {
                song.path.startsWith("content://") -> Uri.parse(song.path)
                song.path.startsWith("file://") -> Uri.parse(song.path)
                else -> Uri.fromFile(File(song.path))
            }
            mediaPlayer?.setDataSource(applicationContext, uri)
            mediaPlayer?.prepareAsync()

            notifySongChanged(song)
            updateMediaSessionMetadata(song)
            showNotification()
        } catch (e: Exception) {
            e.printStackTrace()
            // Gracefully move to next song on loading fails
            serviceScope.launch {
                delay(300)
                skipNext()
            }
        }
    }

    fun play() {
        if (isPrepared && mediaPlayer?.isPlaying == false) {
            if (requestAudioFocus()) {
                try {
                    mediaPlayer?.start()
                    notifyPlaybackStatus(true)
                    startPositionTracker()
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    showNotification()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
            notifyPlaybackStatus(false)
            stopPositionTracker()
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            showNotification()
            // Keep notification but allow system to manage service priority
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
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
        // If more than 3 seconds in, restart current song instead of previous
        if (getPosition() > 3000) {
            seekTo(0)
            return
        }
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
            try {
                mediaPlayer?.seekTo(positionMs.toInt().coerceAtLeast(0))
                notifyPosition(positionMs)
                updatePlaybackState(if (isPlaying()) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isPlaying(): Boolean = try {
        mediaPlayer?.isPlaying == true
    } catch (e: Exception) {
        false
    }

    fun getCurrentSong(): Song? {
        return if (currentSongIndex in currentQueue.indices) currentQueue[currentSongIndex] else null
    }

    fun getDuration(): Long {
        return try {
            if (isPrepared) mediaPlayer?.duration?.toLong()?.coerceAtLeast(0L) ?: 0L
            else getCurrentSong()?.duration ?: 0L
        } catch (e: Exception) {
            getCurrentSong()?.duration ?: 0L
        }
    }

    fun getPosition(): Long {
        return try {
            if (isPrepared) mediaPlayer?.currentPosition?.toLong()?.coerceAtLeast(0L) ?: 0L else 0L
        } catch (e: Exception) {
            0L
        }
    }

    // MediaPlayer callbacks
    override fun onPrepared(mp: MediaPlayer?) {
        isPrepared = true
        try {
            mp?.start()
            notifyPlaybackStatus(true)
            startPositionTracker()
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            showNotification()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        notifyPlaybackStatus(false)
        stopPositionTracker()
        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
        skipNext()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        isPrepared = false
        notifyPlaybackStatus(false)
        // Try next track instead of crashing
        serviceScope.launch {
            delay(400)
            skipNext()
        }
        return true // error handled
    }

    // Position updates
    private fun startPositionTracker() {
        positionUpdateJob?.cancel()
        positionUpdateJob = serviceScope.launch {
            while (isActive) {
                if (isPlaying()) {
                    notifyPosition(getPosition())
                }
                delay(500)
            }
        }
    }

    private fun stopPositionTracker() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    // Listeners notifications dispatchers
    private fun notifySongChanged(song: Song) {
        listeners.toList().forEach { it.onSongChanged(song) }
    }

    private fun notifyPlaybackStatus(isPlaying: Boolean) {
        listeners.toList().forEach { it.onPlaybackStatusChanged(isPlaying) }
    }

    private fun notifyPosition(positionMs: Long) {
        listeners.toList().forEach { it.onPositionUpdate(positionMs) }
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
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                try { mediaPlayer?.setVolume(0.2f, 0.2f) } catch (_: Exception) {}
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                try { mediaPlayer?.setVolume(1.0f, 1.0f) } catch (_: Exception) {}
                play()
            }
        }
    }

    private fun updateMediaSessionMetadata(song: Song) {
        mediaSession?.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
                .build()
        )
    }

    private fun updatePlaybackState(state: Int) {
        val pos = getPosition()
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SEEK_TO or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
                .setState(state, pos, 1.0f)
                .build()
        )
    }

    // Media Notification controls & channels setup
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pulse Audio Service Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Playback controls and status"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showPlaceholderNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Pulse Player")
            .setContentText("Ready")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun showNotification() {
        val currentSong = getCurrentSong()
        if (currentSong == null) {
            showPlaceholderNotification()
            return
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying()) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseActionText = if (isPlaying()) "Pause" else "Play"

        val prevPending = PendingIntent.getBroadcast(this, 1, Intent("ACTION_PREV"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val togglePending = PendingIntent.getBroadcast(this, 2, Intent("ACTION_TOGGLE"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val nextPending = PendingIntent.getBroadcast(this, 3, Intent("ACTION_NEXT"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val style = MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
            .setMediaSession(mediaSession?.sessionToken)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(currentSong.title)
            .setContentText(currentSong.artist)
            .setSubText(currentSong.album)
            .setContentIntent(pendingOpenIntent)
            .setOngoing(isPlaying())
            .setOnlyAlertOnce(true)
            .setStyle(style)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPending)
            .addAction(playPauseIcon, playPauseActionText, togglePending)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
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
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(actionsReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopPositionTracker()
        try {
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        mediaSession?.release()
        mediaSession = null
        try {
            unregisterReceiver(actionsReceiver)
        } catch (_: Exception) {}
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(this)
        }
    }
}
