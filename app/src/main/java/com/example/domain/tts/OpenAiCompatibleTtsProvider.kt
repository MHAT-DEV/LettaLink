package com.example.domain.tts

import android.content.Context
import android.media.MediaPlayer
import com.example.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

class OpenAiCompatibleTtsProvider(
    private val context: Context,
    override val providerId: String,
    override val displayName: String,
    private val baseUrl: String,
    private val endpoint: String = "v1/audio/speech",
    private val apiKey: String = "",
    private val model: String = "tts-1",
    private val defaultVoiceName: String = "alloy"
) : TtsProvider {

    override val isLocalOnly: Boolean = false

    private var mediaPlayer: MediaPlayer? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    override suspend fun getCapabilities(): TtsCapabilities {
        return TtsCapabilities(
            supportsStreaming = false,
            supportsCustomPitch = false,
            supportsCustomSpeed = true,
            supportedLanguages = listOf("English", "Thai", "Chinese", "Japanese", "Multilingual"),
            isOfflineSupported = false
        )
    }

    override suspend fun getVoices(language: String?): List<TtsVoice> {
        val standardVoices = listOf("alloy", "echo", "fable", "onyx", "nova", "shimmer")
        return standardVoices.map { v ->
            TtsVoice(
                id = v,
                name = v.replaceFirstChar { it.uppercase() },
                language = "Multilingual",
                locale = Locale.ENGLISH,
                quality = "High Quality",
                isOfflineAvailable = false
            )
        }
    }

    override suspend fun testConnection(): TtsConnectionResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val normalizedBase = ApiClient.normalizeBaseUrl(baseUrl)
            val fullUrl = "$normalizedBase${endpoint.trim().removePrefix("/")}"

            // Test ping by sending a 1-word audio request or checking server reachable
            val jsonPayload = JSONObject().apply {
                put("model", model)
                put("input", "ping")
                put("voice", defaultVoiceName)
                put("response_format", "mp3")
            }.toString()

            val requestBuilder = Request.Builder()
                .url(fullUrl)
                .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))

            if (apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val latency = System.currentTimeMillis() - start

            if (response.isSuccessful) {
                TtsConnectionResult(
                    isSuccessful = true,
                    message = "Connected to TTS API at $fullUrl (HTTP ${response.code})",
                    latencyMs = latency
                )
            } else {
                val err = response.body?.string()?.take(150) ?: "HTTP ${response.code}"
                TtsConnectionResult(
                    isSuccessful = false,
                    message = "Server returned error: $err",
                    latencyMs = latency
                )
            }
        } catch (e: Exception) {
            TtsConnectionResult(
                isSuccessful = false,
                message = e.message ?: "Connection failed",
                latencyMs = System.currentTimeMillis() - start
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
    ): Boolean = withContext(Dispatchers.IO) {
        stop()

        try {
            val normalizedBase = ApiClient.normalizeBaseUrl(baseUrl)
            val fullUrl = "$normalizedBase${endpoint.trim().removePrefix("/")}"

            val activeVoice = voice?.ifBlank { defaultVoiceName } ?: defaultVoiceName

            // Check file cache first
            val cacheKey = md5("$fullUrl|$model|$activeVoice|$speed|$text")
            val cacheDir = File(context.cacheDir, "lettatalk_audio_cache").apply { mkdirs() }
            val audioFile = File(cacheDir, "$cacheKey.mp3")

            if (!audioFile.exists() || audioFile.length() == 0L) {
                val jsonPayload = JSONObject().apply {
                    put("model", model)
                    put("input", text)
                    put("voice", activeVoice)
                    put("response_format", "mp3")
                    put("speed", speed.coerceIn(0.25f, 4.0f))
                }.toString()

                val requestBuilder = Request.Builder()
                    .url(fullUrl)
                    .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))

                if (apiKey.isNotBlank()) {
                    requestBuilder.header("Authorization", "Bearer $apiKey")
                }

                val response = client.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful) {
                    val err = response.body?.string()?.take(200) ?: "HTTP ${response.code}"
                    withContext(Dispatchers.Main) { onError("TTS Request failed: $err") }
                    return@withContext false
                }

                val bytes = response.body?.bytes() ?: run {
                    withContext(Dispatchers.Main) { onError("Empty audio data returned") }
                    return@withContext false
                }

                FileOutputStream(audioFile).use { it.write(bytes) }
            }

            withContext(Dispatchers.Main) {
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(audioFile.absolutePath)
                        setOnPreparedListener { mp ->
                            onStart()
                            mp.start()
                        }
                        setOnCompletionListener {
                            onDone()
                            stopPlayback()
                        }
                        setOnErrorListener { _, what, extra ->
                            onError("MediaPlayer error: what=$what, extra=$extra")
                            stopPlayback()
                            true
                        }
                        prepareAsync()
                    }
                } catch (e: Exception) {
                    onError("Playback init error: ${e.message}")
                }
            }
            true
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onError("Synthesis failed: ${e.message}") }
            false
        }
    }

    private fun stopPlayback() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignore
        } finally {
            mediaPlayer = null
        }
    }

    override suspend fun stop() {
        withContext(Dispatchers.Main) {
            stopPlayback()
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digested = md.digest(input.toByteArray())
        return digested.joinToString("") { "%02x".format(it) }
    }
}
