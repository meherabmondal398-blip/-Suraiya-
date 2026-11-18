package com.example

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import java.io.IOException

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null,
    val responseModalities: List<String>? = null,
    val imageConfig: ImageConfig? = null,
    val speechConfig: SpeechConfig? = null
)

@JsonClass(generateAdapter = true)
data class ImageConfig(
    val aspectRatio: String? = null
)

@JsonClass(generateAdapter = true)
data class SpeechConfig(
    val voiceConfig: VoiceConfig? = null
)

@JsonClass(generateAdapter = true)
data class VoiceConfig(
    val prebuiltVoiceConfig: PrebuiltVoiceConfig? = null
)

@JsonClass(generateAdapter = true)
data class PrebuiltVoiceConfig(
    val voiceName: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

@JsonClass(generateAdapter = true)
data class VeoInstance(
    val prompt: String
)

@JsonClass(generateAdapter = true)
data class GenerateVideosRequest(
    val instances: List<VeoInstance>
) {
    constructor(prompt: String) : this(instances = listOf(VeoInstance(prompt = prompt)))
}

@JsonClass(generateAdapter = true)
data class GenerateVideosResponse(
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class ModelInfo(
    val name: String? = null,
    val supportedGenerationMethods: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ListModelsResponse(
    val models: List<ModelInfo>? = null
)

class GeminiApiException(
    val httpCode: Int,
    val errorBody: String,
    message: String = "HTTP $httpCode: $errorBody"
) : IOException(message)

interface GeminiApiService {
    @GET("v1beta/models")
    suspend fun listModels(
        @Query("key") apiKey: String
    ): ListModelsResponse

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @Streaming
    @POST("v1beta/models/{model}:streamGenerateContent")
    suspend fun streamGenerateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Query("alt") alt: String = "sse",
        @Body request: GenerateContentRequest
    ): Response<ResponseBody>

    @POST("v1beta/models/{model}:predictLongRunning")
    suspend fun generateVideos(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateVideosRequest
    ): GenerateVideosResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val responseAdapter by lazy {
        moshi.adapter(GenerateContentResponse::class.java)
    }

    val okHttpClient: okhttp3.OkHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }

    fun isOpenAiCompatibleKey(apiKey: String): Boolean {
        val k = apiKey.trim()
        return k.startsWith("sk-") || k.startsWith("gsk_") || k.startsWith("sk-or-")
    }

    /**
     * Supports OpenAI, Groq (gsk_...), OpenRouter (sk-or-...), and DeepSeek (sk-...) keys
     * in case the user enters a non-Gemini API key into one of the 4 boxes.
     */
    fun callOpenAiCompatibleChat(
        apiKey: String,
        request: GenerateContentRequest
    ): GenerateContentResponse {
        val cleanKey = apiKey.trim()
        val (endpointUrl, modelName) = when {
            cleanKey.startsWith("gsk_") ->
                "https://api.groq.com/openai/v1/chat/completions" to "llama-3.3-70b-versatile"
            cleanKey.startsWith("sk-or-") ->
                "https://openrouter.ai/api/v1/chat/completions" to "google/gemini-2.0-flash-001"
            else ->
                "https://api.openai.com/v1/chat/completions" to "gpt-4o-mini"
        }

        val messagesArray = JSONArray()
        val sysText = request.systemInstruction?.parts?.mapNotNull { it.text }?.joinToString("\n").orEmpty()
        if (sysText.isNotBlank()) {
            messagesArray.put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", sysText)
                }
            )
        }
        for (content in request.contents) {
            val role = if (content.role == "model") "assistant" else "user"
            val text = content.parts.mapNotNull { it.text }.joinToString("\n")
            if (text.isNotBlank()) {
                messagesArray.put(
                    JSONObject().apply {
                        put("role", role)
                        put("content", text)
                    }
                )
            }
        }

        val payload = JSONObject().apply {
            put("model", modelName)
            put("messages", messagesArray)
            request.generationConfig?.temperature?.let { put("temperature", it.toDouble()) }
        }

        val httpReq = Request.Builder()
            .url(endpointUrl)
            .addHeader("Authorization", "Bearer $cleanKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(httpReq).execute().use { resp ->
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw GeminiApiException(resp.code, bodyStr)
            }
            val json = JSONObject(bodyStr)
            val replyText = json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()
            return GenerateContentResponse(
                candidates = listOf(
                    Candidate(
                        content = Content(
                            role = "model",
                            parts = listOf(Part(text = replyText))
                        )
                    )
                )
            )
        }
    }

    /**
     * Streams partial text chunks in real-time using Server-Sent Events (SSE: ?alt=sse)
     * from the Gemini `streamGenerateContent` endpoint.
     */
    fun streamContentFlow(
        model: String,
        apiKey: String,
        request: GenerateContentRequest
    ): Flow<String> = flow {
        if (isOpenAiCompatibleKey(apiKey)) {
            val resp = callOpenAiCompatibleChat(apiKey, request)
            val txt = resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text.orEmpty()
            if (txt.isNotEmpty()) emit(txt)
            return@flow
        }

        val cleanModel = model.removePrefix("models/").trim()
        val response = service.streamGenerateContent(
            model = cleanModel,
            apiKey = apiKey,
            alt = "sse",
            request = request
        )
        if (!response.isSuccessful) {
            val errStr = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("")
            throw GeminiApiException(response.code(), errStr)
        }
        val responseBody = response.body() ?: throw GeminiApiException(500, "Empty response body")
        responseBody.use { body ->
            val reader = body.charStream().buffered()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line?.trim() ?: continue
                if (trimmed.isEmpty() || trimmed.startsWith(":")) continue

                val jsonPayload = when {
                    trimmed.startsWith("data:") -> trimmed.removePrefix("data:").trim()
                    trimmed.startsWith("{") && trimmed.endsWith("}") -> trimmed
                    else -> continue
                }

                if (jsonPayload.isEmpty() || jsonPayload == "[DONE]") continue

                val parsed = runCatching { responseAdapter.fromJson(jsonPayload) }.getOrNull()
                val chunkText = parsed?.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.mapNotNull { it.text }
                    ?.joinToString("")
                    .orEmpty()

                if (chunkText.isNotEmpty()) {
                    emit(chunkText)
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
