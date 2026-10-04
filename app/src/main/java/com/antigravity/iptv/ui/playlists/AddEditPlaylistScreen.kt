package com.antigravity.iptv.ui.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.iptv.domain.model.Playlist
import com.antigravity.iptv.ui.theme.LiveBadgeColor
import com.antigravity.iptv.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlaylistScreen(
    playlistId: Long? = null,
    viewModel: PlaylistViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val existingPlaylist = remember(playlistId, playlists) {
        if (playlistId != null && playlistId > 0) {
            playlists.find { it.id == playlistId }
        } else null
    }

    var name by remember(existingPlaylist) { mutableStateOf(existingPlaylist?.name ?: "") }
    var description by remember(existingPlaylist) { mutableStateOf(existingPlaylist?.description ?: "") }
    var url by remember(existingPlaylist) { mutableStateOf(existingPlaylist?.url ?: "") }
    var epgUrl by remember(existingPlaylist) { mutableStateOf(existingPlaylist?.epgUrl ?: "") }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            viewModel.resetState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = if (existingPlaylist != null) "Edit Playlist" else "Add Playlist",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Playlist Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Playlist Name *") },
                placeholder = { Text("e.g., My Entertainment Channels") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Playlist URL
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("M3U / M3U8 Playlist URL *") },
                placeholder = { Text("https://example.com/playlist.m3u") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("Personal IPTV list") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // EPG URL
            OutlinedTextField(
                value = epgUrl,
                onValueChange = { epgUrl = it },
                label = { Text("EPG XMLTV URL (Optional)") },
                placeholder = { Text("https://example.com/epg.xml") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Test Connection Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.testUrl(url) },
                    enabled = url.isNotBlank() && !state.isTestingUrl
                ) {
                    if (state.isTestingUrl) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Test Connection")
                }
            }

            if (state.testUrlResult != null) {
                val isSuccess = state.testUrlResult?.startsWith("Success") == true
                Surface(
                    color = if (isSuccess) SuccessGreen.copy(alpha = 0.15f) else LiveBadgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                            contentDescription = null,
                            tint = if (isSuccess) SuccessGreen else LiveBadgeColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.testUrlResult ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccess) SuccessGreen else LiveBadgeColor
                        )
                    }
                }
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = LiveBadgeColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Button
            Button(
                onClick = {
                    if (existingPlaylist != null) {
                        viewModel.updatePlaylist(existingPlaylist, name, description, url, epgUrl)
                    } else {
                        viewModel.addPlaylist(name, description, url, epgUrl)
                    }
                },
                enabled = name.isNotBlank() && url.isNotBlank() && !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (existingPlaylist != null) "Update & Sync Playlist" else "Save & Sync Playlist")
            }
        }
    }
}
