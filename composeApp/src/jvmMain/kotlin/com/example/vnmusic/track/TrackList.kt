/**
 * @fileName TrackList.kt
 */
package com.example.vnmusic.track

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TrackList(
    tracks: List<Track>,
    selectedTrack: Track?,
    onTrackClick: (Track) -> Unit,
    onTrackRemove: (Track) -> Unit
) {
    val listState = rememberLazyListState()
    val tracksByFolder = tracks.groupBy { it.folderKey }

    LaunchedEffect(selectedTrack?.path, tracks) {
        var selectedIndex = 0
        for (folderTracks in tracksByFolder.values) {
            val indexInFolder = folderTracks.indexOfFirst { it.path == selectedTrack?.path }
            if (indexInFolder >= 0) {
                selectedIndex += indexInFolder
                break
            }
            selectedIndex += folderTracks.size + 1
        }
        if (tracks.any { it.path == selectedTrack?.path }) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState
    ) {
        tracksByFolder.forEach { (folderPath, folderTracks) ->
            val folderName = folderTracks.first().folderName.ifBlank { folderPath }
            item(key = "folder:$folderPath") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 4.dp, top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
            items(items = folderTracks, key = { it.path }) { track ->
                var isHovered by remember(track.path) { mutableStateOf(false) }
                val isSelected = selectedTrack?.path == track.path
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackClick(track) }
                        .onPointerEvent(PointerEventType.Enter) { isHovered = true }
                        .onPointerEvent(PointerEventType.Exit) { isHovered = false }
                        .padding(vertical = 2.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = track.name,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 6.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        style = if (isSelected) MaterialTheme.typography.bodyLarge
                        else MaterialTheme.typography.bodyMedium
                    )
                    Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                        if (isHovered) {
                            IconButton(
                                onClick = { onTrackRemove(track) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove track from playlist",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
