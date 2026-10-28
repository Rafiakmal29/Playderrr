// IntegrationsViewModel.kt

package com.playder.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playder.scrobble.ScrobbleSite
import com.playder.scrobble.Scrobbler
import com.playder.utils.dataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Settings → Integrations: the scrobbling accounts, and the plays still waiting to be sent. */
@HiltViewModel
class IntegrationsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val scrobbler: Scrobbler,
) : ViewModel() {

    /** The name each service is signed in as; a service that isn't signed in is left out. */
    val accounts: StateFlow<Map<ScrobbleSite, String>> = context.dataStore.data
        .map { prefs ->
            ScrobbleSite.entries.mapNotNull { site ->
                val name = prefs[site.usernameKey]?.takeIf { prefs[site.sessionKey] != null }
                name?.let { site to it }
            }.toMap()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val pending: StateFlow<Int> = scrobbler.pending

    /** What "Send now" did, for a short message; cleared once shown. */
    private val _sendResult = MutableStateFlow<String?>(null)
    val sendResult: StateFlow<String?> = _sendResult.asStateFlow()

    suspend fun requestToken(site: ScrobbleSite): String = scrobbler.requestToken(site)

    suspend fun finishSignIn(site: ScrobbleSite, token: String): String? = scrobbler.finishSignIn(site, token)

    fun signOut(site: ScrobbleSite) {
        viewModelScope.launch { scrobbler.signOut(site) }
    }

    fun sendNow() {
        viewModelScope.launch {
            _sendResult.value = if (scrobbler.send()) {
                "All sent"
            } else {
                "Couldn't send them yet. Playder keeps trying whenever you're online."
            }
        }
    }

    fun clearSendResult() {
        _sendResult.value = null
    }
}
