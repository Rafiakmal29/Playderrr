// SongDetailsSheet.kt

package com.playder.ui.player.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.playder.LocalDatabase
import com.playder.LocalPlayerConnection
import com.playder.R
import com.playder.models.MediaMetadata
import com.playder.ui.utils.resize
import com.music.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * What tapping the title and artist on the player opens: the song at the top, a row for each of
 * its artists (with the artist's own photo), one for its album, and "About song", which opens the
 * song's description in a second sheet stacked on this one. Small: it is as tall as what's in it.
 */
@Composable
fun SongDetailsSheet(
    onDismiss: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String) -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val metadata by playerConnection.mediaMetadata.collectAsState()
    val song = metadata ?: return

    var showAbout by remember { mutableStateOf(false) }
    // Shared by this sheet and the about sheet, so the about sheet stacks on this one (it shrinks
    // back and peeks its handle above) instead of opening over it as a second dimmed sheet.
    val stack = rememberMenuSheetStack()

    CompositionLocalProvider(LocalMenuSheetStack provides stack) {
        MenuSheetSurface(onDismiss = onDismiss, wrapHeight = true) { _ ->
            val closeSheet = LocalMenuSheetClose.current
            SheetColumn {
                SongHeader(song)
                Spacer(modifier = Modifier.height(18.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Every artist credited, each its own row: a duet or a feature lists two or
                    // three. One with no id (a local file) has no page to open, so it isn't offered.
                    song.artists.filter { !it.id.isNullOrBlank() }.forEach { artist ->
                        val artistId = artist.id!!
                        val avatar by rememberArtistAvatar(artistId)
                        DetailRow(
                            title = "View ${artist.name}",
                            onClick = { onOpenArtist(artistId) },
                        ) {
                            DetailImage(
                                url = avatar,
                                shape = CircleShape,
                                placeholder = R.drawable.person,
                            )
                        }
                    }
                    song.album?.let { album ->
                        DetailRow(
                            title = "View ${album.title}",
                            onClick = { onOpenAlbum(album.id) },
                        ) {
                            DetailImage(
                                url = song.thumbnailUrl?.resize(120, 120),
                                shape = RoundedCornerShape(7.dp),
                                placeholder = R.drawable.album,
                            )
                        }
                    }
                    DetailRow(
                        title = "About song",
                        onClick = { showAbout = true },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(DetailImageSize)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color.White.copy(alpha = 0.9f)),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                                tint = Color.Black.copy(alpha = 0.75f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
                DoneButton(onClick = closeSheet)
            }
        }

        if (showAbout) {
            AboutSongSheet(song = song, onDismiss = { showAbout = false })
        }
    }
}

/** "About song": the song's description, stacked on the details sheet; done goes back to it. */
@Composable
private fun AboutSongSheet(song: MediaMetadata, onDismiss: () -> Unit) {
    // null while it loads; "" when YouTube had nothing to say
    val description by produceState<String?>(initialValue = null, song.id) {
        value = withContext(Dispatchers.IO) {
            YouTube.getMediaInfo(song.id).getOrNull()?.description?.trim().orEmpty()
        }
    }

    MenuSheetSurface(onDismiss = onDismiss, wrapHeight = true) { _ ->
        val closeSheet = LocalMenuSheetClose.current
        SheetColumn {
            SongHeader(song)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "About song",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.04).em,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(10.dp))
            // A long description scrolls here, between the title and the button, which stays put
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = when {
                        description == null -> "…"
                        description.isNullOrEmpty() -> "There's no description for this song."
                        else -> description.orEmpty()
                    },
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            DoneButton(onClick = closeSheet)
        }
    }
}

@Composable
private fun ColumnScope.SheetColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f, fill = false)
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 22.dp),
        content = content,
    )
}

/** The song itself at the top of both sheets, like the player's action menu has it. */
@Composable
private fun SongHeader(song: MediaMetadata) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = song.thumbnailUrl?.resize(240, 240),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val artists = song.artists.joinToString(", ") { it.name }
            if (artists.isNotBlank()) {
                Text(
                    text = artists,
                    color = Color(0xFFB3B3B3),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val DetailImageSize = 28.dp

/** A row of the details sheet: the picture it's about, then what tapping it does. */
@Composable
private fun DetailRow(
    title: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MenuRowSurface)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        leading()
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** A photo or cover in a row; the glyph shows until (or instead of, if there's none) it loads. */
@Composable
private fun DetailImage(url: String?, shape: Shape, placeholder: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(DetailImageSize)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.9f)),
    ) {
        Icon(
            painter = painterResource(placeholder),
            contentDescription = null,
            tint = Color.Black.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp),
        )
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(DetailImageSize),
            )
        }
    }
}

/** The mock's "done": a full-width pill, lighter than the rows so it reads as the way out. */
@Composable
private fun DoneButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.24f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Text(text = "done", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/** Artist id -> photo url, for this run: the sheet asks again every time it opens. */
private val ArtistAvatars = ConcurrentHashMap<String, String>()

/**
 * The artist's own photo for [artistId]: the library's copy if the artist is in it, otherwise the
 * one on their YouTube Music page. null until (or unless) one is found.
 */
@Composable
private fun rememberArtistAvatar(artistId: String): State<String?> {
    val database = LocalDatabase.current
    return produceState(initialValue = ArtistAvatars[artistId], artistId) {
        if (value != null) return@produceState
        val url = withContext(Dispatchers.IO) {
            database.artist(artistId).first()?.artist?.thumbnailUrl
                ?: YouTube.artist(artistId).getOrNull()?.artist?.thumbnail
        }?.resize(120, 120)
        if (url != null) {
            ArtistAvatars[artistId] = url
            value = url
        }
    }
}
