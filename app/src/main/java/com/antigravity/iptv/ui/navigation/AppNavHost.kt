package com.antigravity.iptv.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.antigravity.iptv.IptvApplication
import com.antigravity.iptv.ui.channels.ChannelListScreen
import com.antigravity.iptv.ui.channels.ChannelListViewModel
import com.antigravity.iptv.ui.components.MiniPlayerBar
import com.antigravity.iptv.ui.favorites.FavoritesScreen
import com.antigravity.iptv.ui.favorites.FavoritesViewModel
import com.antigravity.iptv.ui.history.HistoryScreen
import com.antigravity.iptv.ui.history.HistoryViewModel
import com.antigravity.iptv.ui.home.HomeScreen
import com.antigravity.iptv.ui.home.HomeViewModel
import com.antigravity.iptv.ui.player.PlayerScreen
import com.antigravity.iptv.ui.player.PlayerViewModel
import com.antigravity.iptv.ui.playlists.AddEditPlaylistScreen
import com.antigravity.iptv.ui.playlists.PlaylistManagerScreen
import com.antigravity.iptv.ui.playlists.PlaylistViewModel
import com.antigravity.iptv.ui.search.SearchScreen
import com.antigravity.iptv.ui.search.SearchViewModel
import com.antigravity.iptv.ui.settings.SettingsScreen
import com.antigravity.iptv.ui.settings.SettingsViewModel

@androidx.media3.common.util.UnstableApi
@Composable
fun AppNavHost(
    navController: NavHostController,
    app: IptvApplication
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isPlayerScreen = currentRoute?.startsWith("player/") == true
    val settings by app.settingsRepository.settingsFlow.collectAsState(
        initial = com.antigravity.iptv.domain.model.UserSettings()
    )

    Scaffold(
        bottomBar = {
            if (!isPlayerScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    BottomNavDestination.values().forEach { destination ->
                        val isSelected = currentRoute == destination.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (!isPlayerScreen) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                // Home
                composable(Screen.Home.route) {
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = HomeViewModel.Factory(
                            app.channelRepository,
                            app.playlistRepository,
                            app.settingsRepository
                        )
                    )
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                        onNavigateToPlayer = { channelId ->
                            navController.navigate(Screen.Player.createRoute(channelId))
                        },
                        onNavigateToPlaylists = { navController.navigate(Screen.Playlists.route) },
                        onNavigateToChannelsWithCategory = { category ->
                            navController.navigate(Screen.Channels.route)
                        }
                    )
                }

                // Channels
                composable(Screen.Channels.route) {
                    val channelViewModel: ChannelListViewModel = viewModel(
                        factory = ChannelListViewModel.Factory(
                            app.channelRepository,
                            app.settingsRepository
                        )
                    )
                    ChannelListScreen(
                        viewModel = channelViewModel,
                        onNavigateToPlayer = { channelId ->
                            navController.navigate(Screen.Player.createRoute(channelId))
                        }
                    )
                }

                // Favorites
                composable(Screen.Favorites.route) {
                    val favoritesViewModel: FavoritesViewModel = viewModel(
                        factory = FavoritesViewModel.Factory(
                            app.channelRepository,
                            app.settingsRepository
                        )
                    )
                    FavoritesScreen(
                        viewModel = favoritesViewModel,
                        onNavigateToPlayer = { channelId ->
                            navController.navigate(Screen.Player.createRoute(channelId))
                        },
                        onNavigateToChannels = {
                            navController.navigate(Screen.Channels.route)
                        }
                    )
                }

                // Playlists
                composable(Screen.Playlists.route) {
                    val playlistViewModel: PlaylistViewModel = viewModel(
                        factory = PlaylistViewModel.Factory(app.playlistRepository)
                    )
                    PlaylistManagerScreen(
                        viewModel = playlistViewModel,
                        onNavigateToAdd = { navController.navigate(Screen.AddPlaylist.route) },
                        onNavigateToEdit = { id -> navController.navigate(Screen.EditPlaylist.createRoute(id)) }
                    )
                }

                // Add Playlist
                composable(Screen.AddPlaylist.route) {
                    val playlistViewModel: PlaylistViewModel = viewModel(
                        factory = PlaylistViewModel.Factory(app.playlistRepository)
                    )
                    AddEditPlaylistScreen(
                        playlistId = null,
                        viewModel = playlistViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Edit Playlist
                composable(
                    route = Screen.EditPlaylist.route,
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                    val playlistViewModel: PlaylistViewModel = viewModel(
                        factory = PlaylistViewModel.Factory(app.playlistRepository)
                    )
                    AddEditPlaylistScreen(
                        playlistId = playlistId,
                        viewModel = playlistViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Settings
                composable(Screen.Settings.route) {
                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = SettingsViewModel.Factory(
                            app.settingsRepository,
                            app.playlistRepository,
                            app.channelRepository
                        )
                    )
                    SettingsScreen(viewModel = settingsViewModel)
                }

                // Search
                composable(Screen.Search.route) {
                    val searchViewModel: SearchViewModel = viewModel(
                        factory = SearchViewModel.Factory(
                            app.channelRepository,
                            app.settingsRepository
                        )
                    )
                    SearchScreen(
                        viewModel = searchViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPlayer = { channelId ->
                            navController.navigate(Screen.Player.createRoute(channelId))
                        }
                    )
                }

                // Player
                composable(
                    route = Screen.Player.route,
                    arguments = listOf(navArgument("channelId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val channelId = backStackEntry.arguments?.getLong("channelId") ?: 0L
                    val playerViewModel: PlayerViewModel = viewModel(
                        factory = PlayerViewModel.Factory(
                            app.playerManager,
                            app.channelRepository,
                            app.settingsRepository
                        )
                    )
                    PlayerScreen(
                        channelId = channelId,
                        viewModel = playerViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            // Floating MiniPlayerBar
            if (!isPlayerScreen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                ) {
                    MiniPlayerBar(
                        playerManager = app.playerManager,
                        miniPlayerEnabled = settings.miniPlayerEnabled,
                        onExpandToPlayer = { channelId ->
                            navController.navigate(Screen.Player.createRoute(channelId))
                        }
                    )
                }
            }
        }
    }
}
