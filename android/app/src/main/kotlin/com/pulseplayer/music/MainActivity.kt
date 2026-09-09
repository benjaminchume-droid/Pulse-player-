package com.pulseplayer.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
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
    object Library : Screen("library", "Library", Icons.Default.List)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Mixes : Screen("mixes", "Mixes", Icons.Default.MusicNote)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
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

    var isNowPlayingOpen by remember { mutableStateOf(false) }

    // Open full player whenever a new track is selected / starts
    LaunchedEffect(currentSong?.id) {
        if (currentSong != null) {
            isNowPlayingOpen = true
        }
    }

    var isDockVisible by remember { mutableStateOf(true) }
    var dockSizeStyle by remember { mutableStateOf("Compact") }

    val navigationItems = listOf(
        Screen.Home, Screen.Library, Screen.Search, Screen.Mixes, Screen.Settings
    )

    BackHandler(enabled = true) {
        when {
            isNowPlayingOpen -> isNowPlayingOpen = false
            else -> onExit()
        }
    }

    val dockVisibleState = rememberUpdatedState(isDockVisible)
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -12f && dockVisibleState.value) isDockVisible = false
                else if (delta > 12f && !dockVisibleState.value) isDockVisible = true
                return Offset.Zero
            }
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
            modifier = Modifier.nestedScroll(nestedScrollConnection)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) { HomeScreen(viewModel) }
                    composable(Screen.Library.route) { LibraryScreen(viewModel) }
                    composable(Screen.Search.route) { SearchScreen(viewModel) }
                    composable(Screen.Mixes.route) { MixesScreen(viewModel) }
                    composable(Screen.Settings.route) { SettingsScreen(viewModel) }
                }

                if (currentSong != null && !isNowPlayingOpen) {
                    val playerBottomPadding = when {
                        !isDockVisible -> 32.dp
                        dockSizeStyle == "Expanded" -> 110.dp
                        dockSizeStyle == "Compact" -> 90.dp
                        else -> 74.dp
                    }
                    val smoothBottomOffset by animateDpAsState(
                        targetValue = playerBottomPadding,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "PlayerBottomOffset"
                    )

                    MiniPlayer(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = smoothBottomOffset),
                        song = currentSong,
                        isPlaying = isPlaying,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSkipNext = { viewModel.skipNext() },
                        onClick = { isNowPlayingOpen = true }
                    )
                }

                AnimatedVisibility(
                    visible = isDockVisible && !isNowPlayingOpen,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                ) {
                    val glassBackdropColor = if (isAmoled) Color(0xFF0F0F14) else Color(0x3D000000)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (dockSizeStyle == "Expanded") 74.dp else if (dockSizeStyle == "Compact") 58.dp else 46.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(glassBackdropColor)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(theme.accentColor, theme.glowColor.copy(alpha = 0.2f))
                                ),
                                shape = RoundedCornerShape(30.dp)
                            )
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { isDockVisible = false }) {
                            Icon(Icons.Default.VisibilityOff, "Retract", tint = theme.accentColor, modifier = Modifier.size(16.dp))
                        }
                        navigationItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    screen.icon,
                                    screen.title,
                                    tint = if (selected) theme.accentColor else Color(0x66FFFFFF),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        IconButton(onClick = {
                            dockSizeStyle = when (dockSizeStyle) {
                                "Expanded" -> "Compact"
                                "Compact" -> "Micro"
                                else -> "Expanded"
                            }
                        }) {
                            Icon(Icons.Default.ExpandMore, "Dock size", tint = theme.accentColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (!isDockVisible && !isNowPlayingOpen) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x7A000000))
                            .border(1.dp, theme.accentColor, CircleShape)
                            .clickable { isDockVisible = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, "Expand", tint = theme.accentColor)
                    }
                }
            }
        }

        NowPlayingOverlay(
            isOpen = isNowPlayingOpen,
            song = currentSong,
            isPlaying = isPlaying,
            playbackPosition = position,
            lyricsLoading = lyricsLoading,
            onClose = { isNowPlayingOpen = false },
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSkipNext = { viewModel.skipNext() },
            onSkipPrevious = { viewModel.skipPrevious() },
            onSeekTo = { viewModel.seekTo(it) },
            onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it) } },
            onFetchLyrics = { currentSong?.let { viewModel.fetchLyricsFor(it) } },
            onEnrichMetadata = { currentSong?.let { viewModel.enrichSongMetadata(it) } }
        )
    }
}
