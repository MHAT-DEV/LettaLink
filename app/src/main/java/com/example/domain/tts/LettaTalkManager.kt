package com.example.domain.tts

import android.content.Context
import com.example.data.local.TtsProviderDao
import com.example.data.local.TtsProviderEntity
import com.example.data.local.TtsSettingsDao
import com.example.data.local.TtsSettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

enum class PlaybackStatus {
    IDLE,
    BUFFERING,
    PLAYING,
    PAUSED,
    ERROR
}

data class TalkPlaybackState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val currentMessageId: String? = null,
    val currentAgentName: String? = null,
    val currentSentence: String? = null,
    val errorMessage: String? = null,
    val activeProviderTitle: String = "Android Device TTS"
)

class LettaTalkManager(
    private val context: Context,
    private val ttsProviderDao: TtsProviderDao,
    private val ttsSettingsDao: TtsSettingsDao,
    private val scope: CoroutineScope
) {
    private val nativeProvider = AndroidNativeTtsProvider(context)
    private var customProvider: OpenAiCompatibleTtsProvider? = null

    private val _playbackState = MutableStateFlow(TalkPlaybackState())
    val playbackState: StateFlow<TalkPlaybackState> = _playbackState.asStateFlow()

    private val _settings = MutableStateFlow(TtsSettingsEntity())
    val settings: StateFlow<TtsSettingsEntity> = _settings.asStateFlow()

    private var currentSpeakJob: Job? = null
    private var sentenceQueue = mutableListOf<String>()
    private var currentSentenceIndex = 0

    init {
        scope.launch {
            loadSettingsAndProvider()
        }
    }

    suspend fun loadSettingsAndProvider() = withContext(Dispatchers.IO) {
        // Load or create default settings
        val storedSettings = ttsSettingsDao.getSettings() ?: TtsSettingsEntity().also {
            ttsSettingsDao.saveSettings(it)
        }
        _settings.value = storedSettings

        // Load active provider
        val activeEntity = ttsProviderDao.getDefaultProvider()
        if (activeEntity != null && activeEntity.providerType != "ANDROID_NATIVE") {
            customProvider = OpenAiCompatibleTtsProvider(
                context = context,
                providerId = activeEntity.id.toString(),
                displayName = activeEntity.name,
                baseUrl = activeEntity.baseUrl,
                endpoint = activeEntity.endpoint,
                apiKey = activeEntity.apiKey,
                model = activeEntity.model,
                defaultVoiceName = activeEntity.defaultVoice
            )
            _playbackState.value = _playbackState.value.copy(activeProviderTitle = activeEntity.name)
        } else {
            customProvider = null
            _playbackState.value = _playbackState.value.copy(activeProviderTitle = nativeProvider.displayName)
        }
    }

    fun updateSettings(newSettings: TtsSettingsEntity) {
        scope.launch(Dispatchers.IO) {
            ttsSettingsDao.saveSettings(newSettings)
            _settings.value = newSettings
        }
    }

    private fun getActiveProvider(): TtsProvider {
        return customProvider ?: nativeProvider
    }

    /**
     * Speaks a specific message manually triggered by the user pressing the Play button.
     */
    fun speakMessage(messageId: String, rawText: String, agentName: String = "Agent") {
        if (_playbackState.value.currentMessageId == messageId && _playbackState.value.status == PlaybackStatus.PLAYING) {
            // Already playing this message, toggle pause/stop
            stop()
            return
        }

        stop()

        val normalized = TextNormalizer.normalize(
            rawText = rawText,
            skipCodeBlocks = _settings.value.skipCodeBlocks,
            skipUrls = _settings.value.skipUrls,
            skipJsonMetadata = _settings.value.skipJsonMetadata
        )

        if (normalized.isBlank()) {
            _playbackState.value = TalkPlaybackState(
                status = PlaybackStatus.ERROR,
                errorMessage = "No readable text content in message."
            )
            return
        }

        sentenceQueue = TextNormalizer.segmentIntoSentences(normalized).toMutableList()
        currentSentenceIndex = 0

        _playbackState.value = TalkPlaybackState(
            status = PlaybackStatus.BUFFERING,
            currentMessageId = messageId,
            currentAgentName = agentName,
            currentSentence = sentenceQueue.firstOrNull(),
            activeProviderTitle = getActiveProvider().displayName
        )

        playNextSentenceInQueue(messageId, agentName)
    }

    private fun playNextSentenceInQueue(messageId: String, agentName: String) {
        currentSpeakJob?.cancel()

        if (currentSentenceIndex >= sentenceQueue.size) {
            _playbackState.value = TalkPlaybackState(
                status = PlaybackStatus.IDLE,
                activeProviderTitle = getActiveProvider().displayName
            )
            return
        }

        val sentence = sentenceQueue[currentSentenceIndex]
        val provider = getActiveProvider()
        val currentLocale = if (_settings.value.autoLanguageDetection) {
            TextNormalizer.detectLocale(sentence)
        } else {
            when (_settings.value.defaultLanguage) {
                "th-TH" -> Locale("th", "TH")
                "zh-CN" -> Locale.CHINESE
                "ja-JP" -> Locale.JAPANESE
                else -> Locale.ENGLISH
            }
        }

        currentSpeakJob = scope.launch {
            _playbackState.value = _playbackState.value.copy(
                status = PlaybackStatus.PLAYING,
                currentMessageId = messageId,
                currentAgentName = agentName,
                currentSentence = sentence
            )

            provider.speak(
                text = sentence,
                speed = _settings.value.speechRate,
                pitch = _settings.value.pitch,
                locale = currentLocale,
                onStart = {
                    _playbackState.value = _playbackState.value.copy(
                        status = PlaybackStatus.PLAYING,
                        currentSentence = sentence
                    )
                },
                onDone = {
                    currentSentenceIndex++
                    playNextSentenceInQueue(messageId, agentName)
                },
                onError = { err ->
                    _playbackState.value = TalkPlaybackState(
                        status = PlaybackStatus.ERROR,
                        errorMessage = err,
                        activeProviderTitle = provider.displayName
                    )
                }
            )
        }
    }

    /**
     * Automatic Read Aloud hook triggered when an AI agent response completes.
     */
    fun onAgentResponseCompleted(
        agentId: String,
        agentName: String,
        messageId: String,
        rawText: String
    ) {
        val mode = _settings.value.autoReadAloudMode
        if (mode == "OFF" || mode == "MANUAL_ONLY") return

        // Auto read aloud triggered!
        speakMessage(messageId, rawText, agentName)
    }

    fun stop() {
        currentSpeakJob?.cancel()
        currentSpeakJob = null
        sentenceQueue.clear()
        currentSentenceIndex = 0

        scope.launch {
            try {
                getActiveProvider().stop()
            } catch (e: Exception) {
                // Ignore
            }
            _playbackState.value = TalkPlaybackState(
                status = PlaybackStatus.IDLE,
                activeProviderTitle = getActiveProvider().displayName
            )
        }
    }

    fun testSpeak(sampleText: String) {
        val testId = "test_utterance"
        speakMessage(testId, sampleText, "Test Voice")
    }

    fun clearAudioCache(): Long {
        val cacheDir = File(context.cacheDir, "lettatalk_audio_cache")
        var freedBytes = 0L
        if (cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { file ->
                freedBytes += file.length()
                file.delete()
            }
        }
        return freedBytes
    }

    suspend fun getAvailableVoices(): List<TtsVoice> {
        return getActiveProvider().getVoices()
    }
}
