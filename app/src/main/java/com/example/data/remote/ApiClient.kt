package com.example.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class RawApiResponse(
    val statusCode: Int,
    val statusMessage: String,
    val durationMs: Long,
    val headers: Map<String, String>,
    val body: String,
    val isSuccessful: Boolean,
    val curlCommand: String
)

sealed class StreamTokenEvent {
    data class Token(val text: String) : StreamTokenEvent()
    data class Reasoning(val text: String) : StreamTokenEvent()
    data class ToolCall(val name: String, val args: String) : StreamTokenEvent()
    data class Usage(val promptTokens: Int, val completionTokens: Int, val totalTokens: Int) : StreamTokenEvent()
    data class Done(val totalText: String) : StreamTokenEvent()
    data class Error(val message: String) : StreamTokenEvent()
}

class ApiClient private constructor() {
    companion object {
        private val moshi: Moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        fun buildOkHttpClient(apiKey: String = ""): OkHttpClient {
            val builder = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)

            // Auth Interceptor
            builder.addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                if (apiKey.isNotBlank()) {
                    requestBuilder.header("Authorization", "Bearer $apiKey")
                }
                requestBuilder.header("Accept", "application/json")
                chain.proceed(requestBuilder.build())
            }

            return builder.build()
        }

        fun normalizeBaseUrl(url: String): String {
            var trimmed = url.trim()
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                trimmed = "http://$trimmed"
            }
            if (!trimmed.endsWith("/")) {
                trimmed = "$trimmed/"
            }
            return trimmed
        }

        fun createService(baseUrl: String, apiKey: String = ""): LettaApiService {
            val normalizedUrl = normalizeBaseUrl(baseUrl)
            val client = buildOkHttpClient(apiKey)

            return Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(LettaApiService::class.java)
        }

        suspend fun executeRaw(
            baseUrl: String,
            apiKey: String,
            method: String,
            endpoint: String,
            headers: Map<String, String>,
            requestBodyString: String?
        ): RawApiResponse = kotlinx.coroutines.withContext(Dispatchers.IO) {
            val normalizedBase = normalizeBaseUrl(baseUrl)
            val cleanEndpoint = endpoint.trim().removePrefix("/")
            val fullUrl = "$normalizedBase$cleanEndpoint"

            val client = OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = when {
                method.equals("GET", ignoreCase = true) || method.equals("HEAD", ignoreCase = true) -> null
                requestBodyString.isNullOrBlank() -> "".toRequestBody(mediaType)
                else -> requestBodyString.toRequestBody(mediaType)
            }

            val requestBuilder = Request.Builder()
                .url(fullUrl)
                .method(method.uppercase(), requestBody)

            if (apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }
            headers.forEach { (k, v) ->
                if (k.isNotBlank() && v.isNotBlank()) {
                    requestBuilder.header(k, v)
                }
            }

            // Generate cURL command for developer convenience
            val curlSb = StringBuilder("curl -X ${method.uppercase()} \"$fullUrl\"")
            if (apiKey.isNotBlank()) {
                curlSb.append(" \\\n  -H \"Authorization: Bearer [REDACTED]\"")
            }
            headers.forEach { (k, v) ->
                curlSb.append(" \\\n  -H \"$k: $v\"")
            }
            if (!requestBodyString.isNullOrBlank() && method.uppercase() !in listOf("GET", "HEAD")) {
                val escaped = requestBodyString.replace("\"", "\\\"")
                curlSb.append(" \\\n  -d \"$escaped\"")
            }
            val curlCommand = curlSb.toString()

            val startTime = System.currentTimeMillis()
            try {
                val response: Response = client.newCall(requestBuilder.build()).execute()
                val duration = System.currentTimeMillis() - startTime
                val respBody = response.body?.string() ?: ""

                val responseHeaders = mutableMapOf<String, String>()
                for (i in 0 until response.headers.size) {
                    responseHeaders[response.headers.name(i)] = response.headers.value(i)
                }

                RawApiResponse(
                    statusCode = response.code,
                    statusMessage = response.message,
                    durationMs = duration,
                    headers = responseHeaders,
                    body = respBody,
                    isSuccessful = response.isSuccessful,
                    curlCommand = curlCommand
                )
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                RawApiResponse(
                    statusCode = 0,
                    statusMessage = e.javaClass.simpleName,
                    durationMs = duration,
                    headers = emptyMap(),
                    body = e.message ?: "Network error or timeout connecting to $fullUrl",
                    isSuccessful = false,
                    curlCommand = curlCommand
                )
            }
        }

        fun streamMessage(
            baseUrl: String,
            apiKey: String,
            agentId: String,
            messageText: String
        ): Flow<StreamTokenEvent> = flow {
            val normalizedBase = normalizeBaseUrl(baseUrl)
            val fullUrl = "${normalizedBase}v1/agents/$agentId/messages"
            val client = buildOkHttpClient(apiKey)

            val jsonPayload = JSONObject().apply {
                val messagesArray = org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", messageText)
                    })
                }
                put("messages", messagesArray)
                put("streaming", true)
            }.toString()

            val request = Request.Builder()
                .url(fullUrl)
                .header("Accept", "text/event-stream")
                .header("Content-Type", "application/json")
                .post(jsonPayload.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()

            val accumulatedText = StringBuilder()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: "HTTP ${response.code}"
                    emit(StreamTokenEvent.Error("Stream failed: ${response.code} - $errBody"))
                    return@flow
                }

                val body = response.body ?: run {
                    emit(StreamTokenEvent.Error("Empty response body from stream"))
                    return@flow
                }

                val reader = BufferedReader(InputStreamReader(body.byteStream()))
                var line: String? = reader.readLine()
                while (line != null) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("data:")) {
                        val dataContent = trimmed.removePrefix("data:").trim()
                        if (dataContent == "[DONE]") {
                            break
                        }
                        if (dataContent.isNotEmpty()) {
                            try {
                                val json = JSONObject(dataContent)
                                val msgType = json.optString("message_type")
                                val role = json.optString("role")

                                when {
                                    msgType == "reasoning_message" || msgType == "internal_monologue" -> {
                                        val text = json.optString("reasoning")
                                            .ifEmpty { json.optString("text") }
                                        if (text.isNotEmpty()) {
                                            emit(StreamTokenEvent.Reasoning(text))
                                        }
                                    }
                                    msgType == "tool_call_message" -> {
                                        val toolCall = json.optJSONObject("tool_call")
                                        val name = toolCall?.optString("name") ?: json.optString("tool_name", "tool")
                                        val args = toolCall?.optString("arguments") ?: json.optString("arguments", "")
                                        emit(StreamTokenEvent.ToolCall(name, args))
                                    }
                                    msgType == "assistant_message" || role == "assistant" -> {
                                        val text = json.optString("text")
                                            .ifEmpty { json.optString("content") }
                                        if (text.isNotEmpty()) {
                                            accumulatedText.append(text)
                                            emit(StreamTokenEvent.Token(text))
                                        }
                                    }
                                    else -> {
                                        // Generic token extraction
                                        val text = json.optString("text")
                                            .ifEmpty { json.optString("content") }
                                        if (text.isNotEmpty()) {
                                            accumulatedText.append(text)
                                            emit(StreamTokenEvent.Token(text))
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Raw string chunk
                                accumulatedText.append(dataContent)
                                emit(StreamTokenEvent.Token(dataContent))
                            }
                        }
                    }
                    line = reader.readLine()
                }
                emit(StreamTokenEvent.Done(accumulatedText.toString()))
            } catch (e: Exception) {
                emit(StreamTokenEvent.Error(e.message ?: "Stream network error"))
            }
        }.flowOn(Dispatchers.IO)
    }
}
