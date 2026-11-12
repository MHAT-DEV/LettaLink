package com.example.domain.tts

import java.util.Locale

data class TtsVoice(
    val id: String,
    val name: String,
    val language: String,
    val locale: Locale,
    val quality: String = "Normal",
    val isOfflineAvailable: Boolean = true
)

data class TtsCapabilities(
    val supportsStreaming: Boolean,
    val supportsCustomPitch: Boolean,
    val supportsCustomSpeed: Boolean,
    val supportedLanguages: List<String> = emptyList(),
    val isOfflineSupported: Boolean = true
)

data class TtsConnectionResult(
    val isSuccessful: Boolean,
    val message: String,
    val latencyMs: Long = 0L
)

interface TtsProvider {
    val providerId: String
    val displayName: String
    val isLocalOnly: Boolean

    suspend fun getCapabilities(): TtsCapabilities
    suspend fun getVoices(language: String? = null): List<TtsVoice>
    suspend fun testConnection(): TtsConnectionResult
    suspend fun speak(
        text: String,
        voice: String? = null,
        speed: Float = 1.0f,
        pitch: Float = 1.0f,
        locale: Locale? = null,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    ): Boolean

    suspend fun stop()
}
