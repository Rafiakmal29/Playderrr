package com.playder.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.playder.constants.AiProviderKey
import com.playder.constants.DeeplApiKey
import com.playder.constants.DeeplFormalityKey
import com.playder.constants.LyricsRomanizeBelarusianKey
import com.playder.constants.LyricsRomanizeBulgarianKey
import com.playder.constants.LyricsRomanizeChineseKey
import com.playder.constants.LyricsRomanizeHindiKey
import com.playder.constants.LyricsRomanizeJapaneseKey
import com.playder.constants.LyricsRomanizeKoreanKey
import com.playder.constants.LyricsRomanizeKyrgyzKey
import com.playder.constants.LyricsRomanizeMacedonianKey
import com.playder.constants.LyricsRomanizePunjabiKey
import com.playder.constants.LyricsRomanizeRussianKey
import com.playder.constants.LyricsRomanizeSerbianKey
import com.playder.constants.LyricsRomanizeUkrainianKey
import com.playder.constants.OpenRouterApiKey
import com.playder.constants.OpenRouterBaseUrlKey
import com.playder.constants.OpenRouterModelKey
import com.playder.constants.TranslateLanguageKey
import com.playder.constants.TranslateLyricsKey
import com.playder.constants.TranslateModeKey
import com.playder.constants.TranslateSourceLanguageKey
import com.playder.db.MusicDatabase
import com.playder.lyrics.LyricsEntry
import com.playder.lyrics.LyricsTranslationHelper
import com.playder.lyrics.LyricsUtils
import com.playder.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fills [lines] with their readings and translations, the way the lyrics page shows them: the
 * romanisation (a sub-line, or ruby over Japanese and Chinese words) the settings ask for, and the
 * translation when it's switched on - from the database if one matches, otherwise translated now.
 * Each line carries them in its own flows ([LyricsEntry.romanizedTextFlow], [LyricsEntry.rubyFlow],
 * [LyricsEntry.translatedTextFlow]), so whoever draws the lines just collects those.
 *
 * Shared by the lyrics page and the player styles that perform the lyrics, so both read the same.
 */
@Composable
fun LyricsReadings(
    lines: List<LyricsEntry>,
    songId: String?,
    database: MusicDatabase,
    /** the stored row, for a translation saved with it */
    lyricsEntity: com.playder.db.entities.LyricsEntity?,
) {
    val context = LocalContext.current
    // Romanisation preferences. Every one of these keys existed already and none of them were
    // read by this screen — it romanised unconditionally, so turning any of them off did nothing.
    val (romanizeJapanese) = rememberPreference(LyricsRomanizeJapaneseKey, defaultValue = true)
    val (romanizeKorean) = rememberPreference(LyricsRomanizeKoreanKey, defaultValue = true)
    val (romanizeChinese) = rememberPreference(LyricsRomanizeChineseKey, defaultValue = true)
    val (romanizeRussian) = rememberPreference(LyricsRomanizeRussianKey, defaultValue = true)
    val (romanizeUkrainian) = rememberPreference(LyricsRomanizeUkrainianKey, defaultValue = true)
    val (romanizeSerbian) = rememberPreference(LyricsRomanizeSerbianKey, defaultValue = true)
    val (romanizeBulgarian) = rememberPreference(LyricsRomanizeBulgarianKey, defaultValue = true)
    val (romanizeBelarusian) = rememberPreference(LyricsRomanizeBelarusianKey, defaultValue = true)
    val (romanizeKyrgyz) = rememberPreference(LyricsRomanizeKyrgyzKey, defaultValue = true)
    val (romanizeMacedonian) = rememberPreference(LyricsRomanizeMacedonianKey, defaultValue = true)
    val (romanizeHindi) = rememberPreference(LyricsRomanizeHindiKey, defaultValue = true)
    val (romanizePunjabi) = rememberPreference(LyricsRomanizePunjabiKey, defaultValue = true)

    // Detect the script ONCE over the whole lyric body rather than per line.
    //
    // Per-line detection was the "multi language exists but not at their target" bug: in a song
    // that mixes English and Japanese, the English lines detect as nothing and the Japanese ones
    // as Japanese, so romanisation appeared under a scattered subset of lines. Worse, a Japanese
    // line made mostly of kanji looks identical to Chinese character-by-character, so individual
    // lines of one song were being sent to the pinyin romanizer and came back as Mandarin
    // readings of Japanese words. A song has one language; decide it from all the evidence.
    val bodyScript = remember(lines) {
        LyricsUtils.dominantScript(lines.joinToString(" ") { it.text })
    }
    val romanizerEnabled = when (bodyScript) {
        LyricsUtils.Script.KANA -> romanizeJapanese
        LyricsUtils.Script.HANGUL -> romanizeKorean
        LyricsUtils.Script.HAN -> romanizeChinese
        LyricsUtils.Script.DEVANAGARI -> romanizeHindi
        LyricsUtils.Script.CYRILLIC -> true
        else -> false
    }

    LaunchedEffect(lines, bodyScript, romanizerEnabled) {
        if (!romanizerEnabled) {
            lines.forEach { it.romanizedTextFlow.value = null }
            return@LaunchedEffect
        }
        // One sequential pass on a background dispatcher, not one coroutine per line. Launching a
        // coroutine per line meant a 60-line song fired 60 concurrent kuromoji tokenizations the
        // instant the lyrics loaded, which is what stalled the player on opening a Japanese track.
        withContext(Dispatchers.Default) {
            val body = lines.joinToString(" ") { it.text }
            // Gurmukhi and the individual Cyrillic languages aren't separable by codepoint block
            // alone, so they still go through the existing detectors — but on the whole body.
            val punjabi = LyricsUtils.isPunjabi(body)
            val cyrillicAllowed = when {
                bodyScript != LyricsUtils.Script.CYRILLIC -> false
                LyricsUtils.isUkrainian(body) -> romanizeUkrainian
                LyricsUtils.isSerbian(body) -> romanizeSerbian
                LyricsUtils.isBelarusian(body) -> romanizeBelarusian
                LyricsUtils.isKyrgyz(body) -> romanizeKyrgyz
                LyricsUtils.isMacedonian(body) -> romanizeMacedonian
                LyricsUtils.isBulgarian(body) -> romanizeBulgarian
                else -> romanizeRussian
            }

            suspend fun romanizeOne(text: String): String? = when {
                punjabi && romanizePunjabi -> LyricsUtils.romanizePunjabi(text)
                bodyScript == LyricsUtils.Script.KANA -> LyricsUtils.romanizeJapanese(text)
                bodyScript == LyricsUtils.Script.HANGUL -> LyricsUtils.romanizeKorean(text)
                bodyScript == LyricsUtils.Script.HAN -> LyricsUtils.romanizeChinese(text)
                bodyScript == LyricsUtils.Script.DEVANAGARI -> LyricsUtils.romanizeHindi(text)
                cyrillicAllowed -> LyricsUtils.romanizeCyrillic(text)
                else -> null
            }

            for (entry in lines) {
                if (entry.text.isBlank()) continue

                // Japanese and Chinese get readings placed ABOVE each word, the way furigana works
                // on a printed lyric sheet, rather than the whole line's romanisation dumped
                // underneath as one unbroken string. Everything else stays a sub-line: Cyrillic
                // and Devanagari are alphabetic, so a per-word reading over them adds nothing.
                val ruby = LyricsUtils.rubyFor(entry.text, bodyScript)
                entry.rubyFlow.value = ruby

                entry.romanizedTextFlow.value = if (ruby != null) {
                    // The ruby row carries the reading now; a duplicate underneath is just noise.
                    null
                } else {
                    romanizeOne(entry.text)?.takeIf { it.isNotBlank() && it != entry.text }
                }
            }
        }
    }

    // Translations. One effect owns them: whenever the lyrics, the on/off toggle, or any
    // translation setting changes, it shows the stored translation if one matches and otherwise
    // translates. Before, translating only happened inside a collector of the button's trigger
    // that had captured the language at the moment the screen opened, so after switching from
    // English to Indonesian it went on translating into English; the "whole session" switch was
    // saved but never read; and "show original" only blanked the lines until the next database
    // emission put the stored translation back.
    val (translateLanguage) = rememberPreference(TranslateLanguageKey, defaultValue = "en")
    val (translateSourceLanguage) = rememberPreference(TranslateSourceLanguageKey, defaultValue = "auto")
    val (translateMode) = rememberPreference(TranslateModeKey, defaultValue = "line")
    val (translateSessionWide) = rememberPreference(TranslateLyricsKey, defaultValue = false)
    val translationOn by LyricsTranslationHelper.translationOn.collectAsState()

    val (aiProvider) = rememberPreference(AiProviderKey, defaultValue = "OpenRouter")
    val (openRouterKey) = rememberPreference(OpenRouterApiKey, defaultValue = "")
    val (openRouterBaseUrl) = rememberPreference(
        OpenRouterBaseUrlKey,
        defaultValue = "https://openrouter.ai/api/v1",
    )
    val (openRouterModel) = rememberPreference(OpenRouterModelKey, defaultValue = "")
    val (deeplKey) = rememberPreference(DeeplApiKey, defaultValue = "")
    val (deeplFormality) = rememberPreference(DeeplFormalityKey, defaultValue = "default")

    LaunchedEffect(songId, translateSessionWide) {
        LyricsTranslationHelper.onSongChanged(songId, translateSessionWide)
    }

    // Read, not keyed: saving a translation writes this row, and restarting on that write would
    // cancel the translation that just finished.
    val latestLyricsEntity by androidx.compose.runtime.rememberUpdatedState(lyricsEntity)

    LaunchedEffect(
        lines, translationOn, translateLanguage, translateSourceLanguage, translateMode,
        aiProvider, openRouterKey, deeplKey,
    ) {
        if (lines.isEmpty()) return@LaunchedEffect
        if (!translationOn) {
            lines.forEach { it.translatedTextFlow.value = null }
            return@LaunchedEffect
        }
        val target = translateLanguage.ifBlank { "en" }
        if (LyricsTranslationHelper.loadTranslationsFromDatabase(
                lyrics = lines,
                lyricsEntity = latestLyricsEntity,
                targetLanguage = if (hasAiKeyFor(aiProvider, openRouterKey, deeplKey)) {
                    target
                } else {
                    LyricsTranslationHelper.languageTag(translateSourceLanguage, target)
                },
                mode = translateMode,
            )
        ) return@LaunchedEffect

        // Google Translate needs no account, so it is used whenever the chosen AI provider has no
        // key. (This used to check for ANY key, so DeepL picked with only an OpenRouter key saved
        // went to DeepL and failed with "API key required".)
        if (!hasAiKeyFor(aiProvider, openRouterKey, deeplKey)) {
            LyricsTranslationHelper.translateWithGoogle(
                lyrics = lines,
                targetLanguage = target,
                mode = translateMode,
                sourceLanguage = translateSourceLanguage,
                scope = this,
                songId = songId.orEmpty(),
                database = database,
            )
        } else {
            LyricsTranslationHelper.translateLyrics(
                lyrics = lines,
                targetLanguage = target,
                apiKey = openRouterKey,
                baseUrl = openRouterBaseUrl,
                model = openRouterModel,
                mode = translateMode,
                scope = this,
                context = context,
                provider = aiProvider,
                deeplApiKey = deeplKey,
                deeplFormality = deeplFormality,
                songId = songId.orEmpty(),
                database = database,
            )
        }
    }

    LaunchedEffect(lines) {
        LyricsTranslationHelper.clearTranslationsTrigger.collect {
            lines.forEach { it.translatedTextFlow.value = null }
        }
    }
}

internal fun hasAiKeyFor(provider: String, openRouterKey: String, deeplKey: String): Boolean =
    if (provider == "DeepL") deeplKey.isNotBlank() else openRouterKey.isNotBlank()
