/**
 * @fileName TrackList.kt
 */
package com.example.vnmusic.track

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TrackList(
    tracks: List<Track>,
    currentTrack: Track?,
    onTrackClick: (Track) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(tracks) { track ->
            val isSelected = currentTrack?.path == track.path
            Text(
                text = track.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTrackClick(track) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                style = if (isSelected) MaterialTheme.typography.bodyLarge
                else MaterialTheme.typography.bodyMedium
            )
        }
    }
}
