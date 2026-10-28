// last.fm or libre.fm sign-in in a sheet on the service's own page, the password never reaches playder

package com.playder.ui.screens.settings.integrations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.playder.scrobble.ScrobbleSite
import com.playder.ui.component.SheetPillButton
import com.playder.ui.screens.search.SearchColors
import com.playder.ui.screens.settings.importsync.CaptureWebContent
import com.playder.viewmodels.IntegrationsViewModel
import com.music.lastfm.AudioscrobblerException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** how often to ask whether the user has allowed playder yet */
private const val CheckEveryMs = 2_500L

@Composable
fun ScrobbleSignInContent(
    site: ScrobbleSite,
    viewModel: IntegrationsViewModel,
    onSignedIn: (username: String) -> Unit,
    onClose: () -> Unit,
) {
    // tall enough for the whole login form, short enough to stay a sheet
    val pageHeight = (LocalConfiguration.current.screenHeightDp * 0.62f).dp
    var authUrl by remember { mutableStateOf<String?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    // a new attempt asks for a new token: after a failure, or when the old one runs out
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(attempt) {
        problem = null
        authUrl = null
        if (!site.api.isConfigured) {
            problem = "${site.label} isn't set up in this build of Playder."
            return@LaunchedEffect
        }
        val token = try {
            viewModel.requestToken(site)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            problem = "Couldn't reach ${site.label}. Check your connection and try again."
            return@LaunchedEffect
        }
        authUrl = site.api.authUrl(token)
        while (true) {
            delay(CheckEveryMs)
            val name = try {
                viewModel.finishSignIn(site, token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: AudioscrobblerException) {
                when {
                    e.isTokenExpired -> {
                        attempt++
                        return@LaunchedEffect
                    }
                    e.isTemporary -> null
                    else -> {
                        problem = "${site.label} said: ${e.message}. Try again."
                        return@LaunchedEffect
                    }
                }
            } catch (e: Exception) {
                // the connection dropped for a moment; the next check tries again
                null
            }
            if (name != null) {
                onSignedIn(name)
                return@LaunchedEffect
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text("Sign in to ${site.label}", color = SearchColors.Primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Log in, then allow Playder to scrobble. Your password stays with ${site.label}; Playder never sees it.",
                color = SearchColors.Secondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
        Spacer(Modifier.height(14.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(pageHeight)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(18.dp)),
        ) {
            val url = authUrl
            val issue = problem
            when {
                issue != null -> SignInProblem(issue, onRetry = { attempt++ })
                url == null -> CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                // a new token needs a new page, not the old one reloaded
                else -> key(url) {
                    CaptureWebContent(
                        startUrl = url,
                        playerOrigins = emptySet(),
                        desktopSite = false,
                        onHeader = { _, _, _ -> },
                        onTick = {},
                        onExit = onClose,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SignInProblem(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Text(message, color = SearchColors.Secondary, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        SheetPillButton(text = "Try again", onClick = onRetry)
    }
}
