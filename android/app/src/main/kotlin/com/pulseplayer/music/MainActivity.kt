package com.pulseplayer.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.pulseplayer.music.ui.theme.NeonCyan
import com.pulseplayer.music.ui.theme.PulsePlayerTheme
import com.pulseplayer.music.viewmodel.PlaybackViewModel
import com.pulseplayer.music.viewmodel.PlaybackViewModelFactory

// Represents bottom navigation routes and visual glyphs
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Library : Screen("library", "Library", Icons.Default.List)
    object Playlists : Screen("playlist", "Playlists", Icons.Default.Star)
    object Search : Screen("search", "Search", Icons.Default.Search)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: PlaybackViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SQLite Room database cache layers
        val database = MusicDatabase.getDatabase(applicationContext)
        val factory = PlaybackViewModelFactory(applicationContext, database.musicDao())
        viewModel = ViewModelProvider(this, factory)[PlaybackViewModel::class.java]

        setContent {
            PulsePlayerTheme {
                MainLayoutContainer(viewModel)
            }
        }
    }
}

@Composable
fun MainLayoutContainer(viewModel: PlaybackViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Playback state synchronization from viewmodel flows
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.playbackPosition.collectAsState()

    var isNowPlayingOpen by remember { mutableStateOf(false) }

    val navigationItems = listOf(
        Screen.Home,
        Screen.Library,
        Screen.Playlists,
        Screen.Search,
        Screen.Settings
    )

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        // Holographic moving gradient background
        AnimatedGlowBackground(modifier = Modifier.fillMaxSize())

        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0x33000000), // Obsidian translucent bar
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(80.dp)
                ) {
                    navigationItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                unselectedIconColor = Color(0x66FFFFFF),
                                unselectedTextColor = Color(0x66FFFFFF),
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            },
            containerColor = Color.Transparent // Allows moving visual spheres to shine through
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Primary screen Host
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) { HomeScreen(viewModel) }
                    composable(Screen.Library.route) { LibraryScreen(viewModel) }
                    composable(Screen.Playlists.route) { PlaylistScreen(viewModel) }
                    composable(Screen.Search.route) { SearchScreen(viewModel) }
                    composable(Screen.Settings.route) { SettingsScreen(viewModel) }
                }

                // Floating toolbar for ongoing audio sessions
                MiniPlayer(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                    song = currentSong,
                    isPlaying = isPlaying,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSkipNext = { viewModel.skipNext() },
                    onClick = { isNowPlayingOpen = true }
                )
            }
        }

        // Full screen cover visualizer sheets
        NowPlayingOverlay(
            isOpen = isNowPlayingOpen,
            song = currentSong,
            isPlaying = isPlaying,
            playbackPosition = position,
            onClose = { isNowPlayingOpen = false },
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSkipNext = { viewModel.skipNext() },
            onSkipPrevious = { viewModel.skipPrevious() },
            onSeekTo = { viewModel.seekTo(it) },
            onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it) } }
        )
    }
}
