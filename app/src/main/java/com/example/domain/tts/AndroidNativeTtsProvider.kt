package com.example.domain.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

class AndroidNativeTtsProvider(
    private val context: Context
) : TtsProvider {

    override val providerId: String = "android_native"
    override val displayName: String = "Android Device TTS (Offline & Local)"
    override val isLocalOnly: Boolean = true

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val initDeferred = CompletableDeferred<Boolean>()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                initDeferred.complete(true)
            } else {
                isInitialized = false
                initDeferred.complete(false)
            }
        }
    }

    private suspend fun ensureInitialized(): Boolean {
        if (isInitialized) return true
        return withContext(Dispatchers.IO) {
            try {
                initDeferred.await()
            } catch (e: Exception) {
                false
            }
        }
    }

    override suspend fun getCapabilities(): TtsCapabilities {
        ensureInitialized()
        val languages = mutableListOf<String>()
        try {
            tts?.availableLanguages?.forEach { loc ->
                languages.add(loc.displayLanguage)
            }
        } catch (e: Exception) {
            languages.addAll(listOf("English", "Thai", "Chinese", "Japanese"))
        }

        return TtsCapabilities(
            supportsStreaming = false,
            supportsCustomPitch = true,
            supportsCustomSpeed = true,
            supportedLanguages = languages.distinct(),
            isOfflineSupported = true
        )
    }

    override suspend fun getVoices(language: String?): List<TtsVoice> {
        ensureInitialized()
        val list = mutableListOf<TtsVoice>()
        try {
            tts?.voices?.forEach { voice ->
                if (language == null || voice.locale.language.equals(language, ignoreCase = true)) {
                    list.add(
                        TtsVoice(
                            id = voice.name,
                            name = voice.name,
                            language = voice.locale.displayLanguage,
                            locale = voice.locale,
                            quality = if (voice.quality == android.speech.tts.Voice.QUALITY_VERY_HIGH) "High Quality" else "Standard",
                            isOfflineAvailable = !voice.isNetworkConnectionRequired
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Voices fallback
        }
        return list
    }

    override suspend fun testConnection(): TtsConnectionResult {
        val ready = ensureInitialized()
        return if (ready) {
            val engine = tts?.defaultEngine ?: "Default System Engine"
            val count = tts?.voices?.size ?: 0
            TtsConnectionResult(
                isSuccessful = true,
                message = "Engine '$engine' ready with $count installed voice(s).",
                latencyMs = 5
            )
        } else {
            TtsConnectionResult(
                isSuccessful = false,
                message = "Android TextToSpeech engine failed to initialize.",
                latencyMs = 0
            )
        }
    }

    override suspend fun speak(
        text: String,
        voice: String?,
        speed: Float,
        pitch: Float,
        locale: Locale?,
        onStart: () -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ): Boolean = withContext(Dispatchers.Main) {
        val ready = ensureInitialized()
        if (!ready) {
            onError("Android TTS is not ready")
            return@withContext false
        }

        val engine = tts ?: run {
            onError("TTS engine is null")
            return@withContext false
        }

        // Apply pitch & rate
        engine.setPitch(pitch.coerceIn(0.5f, 2.0f))
        engine.setSpeechRate(speed.coerceIn(0.5f, 2.5f))

        // Apply Locale / Language
        val targetLocale = locale ?: TextNormalizer.detectLocale(text)
        val langResult = engine.setLanguage(targetLocale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to English if target language data missing
            engine.setLanguage(Locale.ENGLISH)
        }

        // Apply Voice if specified
        if (!voice.isNullOrBlank()) {
            engine.voices?.find { it.name == voice }?.let {
                engine.voice = it
            }
        }

        val utteranceId = UUID.randomUUID().toString()

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                if (id == utteranceId) onStart()
            }

            override fun onDone(id: String?) {
                if (id == utteranceId) onDone()
            }

            override fun onError(id: String?) {
                if (id == utteranceId) onError("Playback error on utterance")
            }

            override fun onError(id: String?, errorCode: Int) {
                if (id == utteranceId) onError("TTS Error code: $errorCode")
            }
        })

        val params = Bundle()
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        result == TextToSpeech.SUCCESS
    }

    override suspend fun stop() {
        withContext(Dispatchers.Main) {
            try {
                tts?.stop()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
