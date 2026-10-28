// ShareEditorSheets.kt
//
// The editors, step 3 of a lyrics share. The Card editor shows the picture with its card; a tap on
// the card tries the next cover colour. The Cinematic editor ("CoolEditor") outlines the song and
// the words: the words open the word editor ("CoolfontEditor", in ShareWordEditor.kt), and the
// arrow over the song moves it under the words.

package com.playder.ui.player.share

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playder.ui.component.PopupSheetState
import com.playder.ui.component.pressBounce
import com.playder.ui.theme.InterFontFamily

/** How tall the picture is in an editor: most of the sheet, leaving the title and the buttons room. */
@Composable
private fun previewHeight() = (LocalConfiguration.current.screenHeightDp * 0.58f).dp

// ---------------------------------------------------------------------------------------------
// Card

internal fun PopupSheetState.showCardEditor(session: LyricsShareSession) {
    val popup = this
    show(buttonBar = { ShareActions(session) }) {
        ShareTitle("Share", "Card styles")
        Box(Modifier.fillMaxWidth().height(previewHeight())) {
            SharePreview(
                session = session,
                capture = true,
                modifier = Modifier.fillMaxSize(),
                onCardTap = {
                    if (session.palette.isNotEmpty()) session.cardColor = (session.cardColor + 1) % (session.palette.size + 2)
                },
            )
            EditBackgroundPill(Modifier.align(Alignment.TopEnd)) { popup.showBackgroundPicker(session) }
        }
        Text(
            "Tap the card to try another colour",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = ShareMuted,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp, bottom = 6.dp),
        )
    }
}

@Composable
private fun EditBackgroundPill(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        "Edit background",
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = (-0.2).sp,
        color = Color.White,
        modifier = modifier
            .padding(12.dp)
            .pressBounce(interaction)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.38f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

// ---------------------------------------------------------------------------------------------
// Cinematic

internal fun PopupSheetState.showCinematicEditor(session: LyricsShareSession) {
    val popup = this
    show(buttonBar = { ShareActions(session) }) {
        ShareTitle("CoolEditor", beta = true)
        val regions = remember { ShareRegions() }
        Box(Modifier.fillMaxWidth().height(previewHeight())) {
            SharePreview(
                session = session,
                capture = true,
                regions = regions,
                modifier = Modifier.fillMaxSize(),
                overlay = {
                    EditOutlines(
                        regions = regions,
                        headerBelow = session.headerBelow,
                        onWords = { popup.showWordEditor(session) },
                        onMoveHeader = { session.headerBelow = !session.headerBelow },
                    )
                },
            )
            EditBackgroundPill(Modifier.align(Alignment.TopEnd)) { popup.showBackgroundPicker(session) }
        }
        Text(
            "Tap the words to style them, the arrow to move the song",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = ShareMuted,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 10.dp, bottom = 6.dp),
        )
    }
}

