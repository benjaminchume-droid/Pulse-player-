package com.pulseplayer.music

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Offset
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

    // Active visual settings
    val theme by RealmManager.currentTheme.collectAsState()
    val isAmoled by RealmManager.amoledMode.collectAsState()

    // Sound stats
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.playbackPosition.collectAsState()

    var isNowPlayingOpen by remember { mutableStateOf(false) }

    // Retractable floating dock states
    var isDockVisible by remember { mutableStateOf(true) }
    var dockSizeStyle by remember { mutableStateOf("Compact") } // "Expanded", "Compact", "Micro"

    val navigationItems = listOf(
        Screen.Home,
        Screen.Library,
        Screen.Search,
        Screen.Mixes,
        Screen.Settings
    )

    // Scroll gesture listener for retractable docking panel
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                val delta = available.y
                if (delta < -15f) { // Scrolling down
                    if (isDockVisible) isDockVisible = false
                } else if (delta > 15f) { // Scrolling up
                    if (!isDockVisible) isDockVisible = true
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isAmoled) Color.Black else BackgroundDark)
    ) {
        // Holographic moving gradient background
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
                // Primary screen Host
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

                // 1. Floating Mini player sitting cleanly directly above docking items
                if (currentSong != null) {
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

                // 2. Retractable Floating Glass Dock Navigation
                AnimatedVisibility(
                    visible = isDockVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(stiffness = Spring.StiffnessMedium)
                    ) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
                ) {
                    val glassBackdropColor = if (isAmoled) Color(0xFF0F0F14) else Color(0x3D000000)

                    // Unified Glass Dock Container
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
                        // Quick compact retract button
                        IconButton(onClick = { isDockVisible = false }) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "Retract Dock",
                                tint = theme.accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Navigation tabs mapping loop
                        navigationItems.forEach { screen ->
                            val selected = currentRoute == screen.route

                            Column(
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
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        tint = if (selected) theme.accentColor else Color(0x66FFFFFF),
                                        modifier = Modifier.size(if (dockSizeStyle == "Micro") 18.dp else 22.dp)
                                    )

                                    // Top-tier Active Tab Neon Under Glow Spark line
                                    if (selected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(top = 22.dp)
                                                .width(18.dp)
                                                .height(2.dp)
                                                .background(theme.accentColor, CircleShape)
                                        )
                                    }
                                }

                                if (dockSizeStyle == "Expanded") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = screen.title,
                                        color = if (selected) theme.accentColor else Color.LightGray,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Cycle Dock Style Sizes Button
                        IconButton(onClick = {
                            dockSizeStyle = when (dockSizeStyle) {
                                "Expanded" -> "Compact"
                                "Compact" -> "Micro"
                                else -> "Expanded"
                            }
                        }) {
                            Icon(
                                imageVector = if (dockSizeStyle == "Expanded") Icons.Default.ExpandMore 
                                              else if (dockSizeStyle == "Compact") Icons.Default.ExpandLess 
                                              else Icons.Default.SettingsInputAntenna,
                                contentDescription = "Cycle Dock Size",
                                tint = theme.accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // 3. Float Chevron handle visible only when dock is retracted to pull it back up
                if (!isDockVisible) {
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
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Expand Dock Controls",
                            tint = theme.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 4. Immersive Full screen player sheet overlay
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
