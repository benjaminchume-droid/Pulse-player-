package com.pulseplayer.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pulseplayer.music.data.MusicDatabase
import com.pulseplayer.music.ui.components.AnimatedGlowBackground
import com.pulseplayer.music.ui.components.MiniPlayer
import com.pulseplayer.music.ui.components.NowPlayingOverlay
import com.pulseplayer.music.ui.screens.*
import com.pulseplayer.music.ui.theme.BackgroundDark
import com.pulseplayer.music.ui.theme.PulsePlayerTheme
import com.pulseplayer.music.ui.theme.RealmManager
import com.pulseplayer.music.viewmodel.PlaybackViewModel
import com.pulseplayer.music.viewmodel.PlaybackViewModelFactory

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Library : Screen("library", "Songs", Icons.Default.LibraryMusic)
    object Playlists : Screen("playlists", "Playlists", Icons.Default.QueueMusic)
    object Downloads : Screen("downloads", "Downloads", Icons.Default.Download)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Connect : Screen("connect", "Connect", Icons.Default.Groups)
}

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: PlaybackViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = MusicDatabase.getDatabase(applicationContext)
        val factory = PlaybackViewModelFactory(applicationContext, database.musicDao())
        viewModel = ViewModelProvider(this, factory)[PlaybackViewModel::class.java]
        setContent {
            PulsePlayerTheme {
                MainLayoutContainer(viewModel, onExit = { finish() })
            }
        }
    }
}

@Composable
fun MainLayoutContainer(viewModel: PlaybackViewModel, onExit: () -> Unit) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val theme by RealmManager.currentTheme.collectAsState()
    val isAmoled by RealmManager.amoledMode.collectAsState()

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.playbackPosition.collectAsState()
    val lyricsLoading by viewModel.lyricsLoading.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()

    var isNowPlayingOpen by remember { mutableStateOf(false) }
    var fullScreenLyrics by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }

    LaunchedEffect(currentSong?.id) {
        if (currentSong != null) isNowPlayingOpen = true
    }

    val bottomTabs = listOf(
        Screen.Home,
        Screen.Library,
        Screen.Playlists,
        Screen.Downloads,
        Screen.Settings
    )

    BackHandler(enabled = true) {
        when {
            fullScreenLyrics -> fullScreenLyrics = false
            isNowPlayingOpen -> isNowPlayingOpen = false
            searchOpen -> searchOpen = false
            else -> onExit()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isAmoled) Color.Black else BackgroundDark)
    ) {
        AnimatedGlowBackground(modifier = Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (!isNowPlayingOpen) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pulse",
                            color = theme.accentColor,
                            fontSize = 22.sp,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = {
                                searchOpen = true
                                navController.navigate(Screen.Search.route) {
                                    launchSingleTop = true
                                }
                            }) {
                                Icon(Icons.Default.Search, "Search", tint = Color.White)
                            }
                            IconButton(onClick = {
                                navController.navigate(Screen.Connect.route) { launchSingleTop = true }
                            }) {
                                Icon(Icons.Default.Groups, "Listen Together", tint = Color.White)
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (!isNowPlayingOpen) {
                    Column {
                        if (currentSong != null) {
                            MiniPlayer(
                                song = currentSong,
                                isPlaying = isPlaying,
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.skipNext() },
                                onClick = { isNowPlayingOpen = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(if (isAmoled) Color(0xFF0F0F14) else Color(0x3D000000))
                                .border(
                                    1.dp,
                                    Brush.linearGradient(listOf(theme.accentColor, theme.glowColor.copy(alpha = 0.25f))),
                                    RoundedCornerShape(28.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bottomTabs.forEach { screen ->
                                val selected = currentRoute == screen.route
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        screen.icon,
                                        screen.title,
                                        tint = if (selected) theme.accentColor else Color(0x66FFFFFF),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        screen.title,
                                        color = if (selected) theme.accentColor else Color.Gray,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                composable(Screen.Home.route) { HomeScreen(viewModel) }
                composable(Screen.Library.route) { LibraryScreen(viewModel) }
                composable(Screen.Playlists.route) { PlaylistsScreen(viewModel) }
                composable(Screen.Downloads.route) { DownloadsScreen(viewModel) }
                composable(Screen.Settings.route) { SettingsScreen(viewModel) }
                composable(Screen.Search.route) {
                    SearchScreen(viewModel, onBack = {
                        searchOpen = false
                        navController.popBackStack()
                    })
                }
                composable(Screen.Connect.route) { ConnectScreen(viewModel) }
            }
        }

        NowPlayingOverlay(
            isOpen = isNowPlayingOpen,
            song = currentSong,
            isPlaying = isPlaying,
            playbackPosition = position,
            lyricsLoading = lyricsLoading,
            repeatMode = repeatMode,
            fullScreenLyrics = fullScreenLyrics,
            onFullScreenLyrics = { fullScreenLyrics = it },
            onClose = { isNowPlayingOpen = false },
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSkipNext = { viewModel.skipNext() },
            onSkipPrevious = { viewModel.skipPrevious() },
            onSeekTo = { viewModel.seekTo(it) },
            onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it) } },
            onFetchLyrics = { currentSong?.let { viewModel.fetchLyricsFor(it) } },
            onEnrichMetadata = { currentSong?.let { viewModel.enrichSongMetadata(it) } },
            onCycleRepeat = { viewModel.cycleRepeatMode() },
            onQueueNext = { /* handled in lists via swipe */ },
            onSleepTimer = { viewModel.setSleepTimerMinutes(it) }
        )
    }
}
