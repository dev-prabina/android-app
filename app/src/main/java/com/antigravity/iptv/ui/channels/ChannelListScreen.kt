package com.antigravity.iptv.ui.channels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.iptv.domain.model.Channel
import com.antigravity.iptv.domain.model.LayoutStyle
import com.antigravity.iptv.ui.components.CategoryChipRow
import com.antigravity.iptv.ui.components.ChannelCard
import com.antigravity.iptv.ui.components.EmptyStateView
import com.antigravity.iptv.ui.components.LanguageChipRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelListScreen(
    viewModel: ChannelListViewModel,
    onNavigateToPlayer: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Filter channels...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column {
                            Text(
                                text = "Channels",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${state.channels.size} of ${state.totalCount} channels",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) viewModel.setSearchQuery("")
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Filled.Clear else Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    }

                    // Layout switcher
                    IconButton(
                        onClick = {
                            val nextStyle = when (state.userSettings.layoutStyle) {
                                LayoutStyle.GRID -> LayoutStyle.LIST
                                LayoutStyle.LIST -> LayoutStyle.COMPACT
                                LayoutStyle.COMPACT -> LayoutStyle.GRID
                            }
                            viewModel.setLayoutStyle(nextStyle)
                        }
                    ) {
                        Icon(
                            imageVector = when (state.userSettings.layoutStyle) {
                                LayoutStyle.GRID -> Icons.Filled.GridView
                                LayoutStyle.LIST -> Icons.Filled.ViewList
                                LayoutStyle.COMPACT -> Icons.Filled.TableRows
                            },
                            contentDescription = "Toggle layout"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Chips
            CategoryChipRow(
                categories = state.categories,
                selectedCategory = state.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            // Language Chips (Regional filter including Odia, Hindi, etc.)
            LanguageChipRow(
                languages = state.languages,
                selectedLanguage = state.selectedLanguage,
                onLanguageSelected = { viewModel.selectLanguage(it) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Channel content
            if (state.channels.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.SearchOff,
                    title = "No Channels Found",
                    description = "No channels match the current category, language, or search filter.",
                    actionButtonText = "Reset Filters",
                    onActionClick = {
                        viewModel.selectCategory(null)
                        viewModel.selectLanguage(null)
                        viewModel.setSearchQuery("")
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                when (state.userSettings.layoutStyle) {
                    LayoutStyle.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.channels, key = { it.id }) { channel ->
                                ChannelCard(
                                    channel = channel,
                                    layoutStyle = LayoutStyle.GRID,
                                    showLogo = state.userSettings.showLogos,
                                    onChannelClick = { onNavigateToPlayer(channel.id) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(channel.id) }
                                )
                            }
                        }
                    }
                    LayoutStyle.LIST, LayoutStyle.COMPACT -> {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.channels, key = { it.id }) { channel ->
                                ChannelCard(
                                    channel = channel,
                                    layoutStyle = state.userSettings.layoutStyle,
                                    showLogo = state.userSettings.showLogos,
                                    onChannelClick = { onNavigateToPlayer(channel.id) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(channel.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
