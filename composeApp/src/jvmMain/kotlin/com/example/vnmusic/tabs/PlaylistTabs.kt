/**
 * @fileName PlaylistTabs.kt
 */
package com.example.vnmusic.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlaylistTabs(
    tabs: List<PlaylistTab>,
    activeTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (Int) -> Unit,
    onTabRenamed: (Int, String) -> Unit
) {
    if (tabs.isEmpty()) return

    val listState = rememberLazyListState()
    var menuTabIndex by remember { mutableStateOf<Int?>(null) }
    var editingTabIndex by remember { mutableStateOf<Int?>(null) }
    var editedTitle by remember { mutableStateOf("") }

    // Обработка колеса мыши для горизонтальной прокрутки
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val delta = event.changes.first().scrollDelta.y
                // Прокручиваем вправо/влево в зависимости от направления колеса
                if (delta > 0f) {
                    listState.dispatchRawDelta(50f) // вправо
                } else if (delta < 0f) {
                    listState.dispatchRawDelta(-50f) // влево
                }
            }
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(tabs.size) { index ->
                val tab = tabs[index]
                val isActive = index == activeTabIndex
                val tabShape = RoundedCornerShape(14.dp)
                Box {
                    Surface(
                        shape = tabShape,
                        color = if (isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = tabShape
                            )
                            .clickable { onTabSelected(index) }
                            .onPointerEvent(PointerEventType.Press) { event ->
                                if (event.buttons.isSecondaryPressed) {
                                    onTabSelected(index)
                                    menuTabIndex = index
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { onTabClosed(index) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close tab",
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = menuTabIndex == index,
                        onDismissRequest = { menuTabIndex = null }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                editedTitle = tab.title
                                editingTabIndex = index
                                menuTabIndex = null
                            }
                        )
                    }
                }
            }
        }

        val indexBeingEdited = editingTabIndex
        if (indexBeingEdited != null) {
            AlertDialog(
                onDismissRequest = { editingTabIndex = null },
                title = { Text("Rename tab") },
                text = {
                    TextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        singleLine = true,
                        label = { Text("Name") }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val title = editedTitle.trim()
                            if (title.isNotEmpty()) {
                                onTabRenamed(indexBeingEdited, title)
                                editingTabIndex = null
                            }
                        },
                        enabled = editedTitle.isNotBlank()
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingTabIndex = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}