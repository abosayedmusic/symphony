package io.github.zyrouge.symphony.ui.components

import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.zyrouge.symphony.services.groove.Groove
import io.github.zyrouge.symphony.services.groove.repositories.AlbumArtistRepository
import io.github.zyrouge.symphony.ui.helpers.ViewContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumArtistGrid(
    context: ViewContext,
    albumArtistNames: List<String>,
    albumArtistsCount: Int? = null,
) {
    val sortBy by context.symphony.settings.lastUsedAlbumArtistsSortBy.flow.collectAsState()
    val sortReverse by context.symphony.settings.lastUsedAlbumArtistsSortReverse.flow.collectAsState()
    val sortedAlbumArtistNames by remember(albumArtistNames, sortBy, sortReverse) {
        derivedStateOf {
            context.symphony.groove.albumArtist.sort(albumArtistNames, sortBy, sortReverse)
        }
    }
    val horizontalGridColumns by context.symphony.settings.lastUsedAlbumArtistsHorizontalGridColumns.flow.collectAsState()
    val verticalGridColumns by context.symphony.settings.lastUsedAlbumArtistsVerticalGridColumns.flow.collectAsState()
    val gridColumns by remember(horizontalGridColumns, verticalGridColumns) {
        derivedStateOf {
            ResponsiveGridColumns(horizontalGridColumns, verticalGridColumns)
        }
    }
    var showModifyLayoutSheet by remember { mutableStateOf(false) }

    MediaSortBarScaffold(
        mediaSortBar = {
            MediaSortBar(
                context,
                reverse = sortReverse,
                onReverseChange = {
                    context.symphony.settings.lastUsedAlbumArtistsSortReverse.setValue(it)
                },
                sort = sortBy,
                sorts = AlbumArtistRepository.SortBy.entries
                    .associateWith { x -> ViewContext.parameterizedFn { x.label(context) } },
                onSortChange = {
                    context.symphony.settings.lastUsedAlbumArtistsSortBy.setValue(it)
                },
                label = {
                    Text(
                        context.symphony.t.XArtists(
                            (albumArtistsCount ?: albumArtistNames.size).toString()
                        )
                    )
                },
                onShowModifyLayout = {
                    showModifyLayoutSheet = true
                },
            )
        },
        content = {
            when {
                albumArtistNames.isEmpty() -> IconTextBody(
                    icon = { modifier ->
                        Icon(
                            Icons.Filled.Person,
                            null,
                            modifier = modifier,
                        )
                    },
                    content = { Text(context.symphony.t.DamnThisIsSoEmpty) }
                )

                else -> ResponsiveGrid(gridColumns) {
                    itemsIndexed(
                        sortedAlbumArtistNames,
                        key = { i, x -> "$i-$x" },
                        contentType = { _, _ -> Groove.Kind.ARTIST }
                    ) { _, albumArtistName ->
                        context.symphony.groove.albumArtist.get(albumArtistName)
                            ?.let { albumArtist ->
                                AlbumArtistTile(context, albumArtist)
                            }
                    }
                }
            }

            if (showModifyLayoutSheet) {
                ResponsiveGridSizeAdjustBottomSheet(
                    context,
                    columns = gridColumns,
                    onColumnsChange = {
                        context.symphony.settings.lastUsedAlbumArtistsHorizontalGridColumns.setValue(
                            it.horizontal
                        )
                        context.symphony.settings.lastUsedAlbumArtistsVerticalGridColumns.setValue(
                            it.vertical
                        )
                    },
                    onDismissRequest = {
                        showModifyLayoutSheet = false
                    }
                )
            }
        }
    )
}

private fun AlbumArtistRepository.SortBy.label(context: ViewContext) = when (this) {
    AlbumArtistRepository.SortBy.CUSTOM -> context.symphony.t.Custom
    AlbumArtistRepository.SortBy.ARTIST_NAME -> context.symphony.t.Artist
    AlbumArtistRepository.SortBy.ALBUMS_COUNT -> context.symphony.t.AlbumCount
    AlbumArtistRepository.SortBy.TRACKS_COUNT -> context.symphony.t.TrackCount
}
