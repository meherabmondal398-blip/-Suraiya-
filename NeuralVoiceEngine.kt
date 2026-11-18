package com.example

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class NeuralVoiceEngine(
    private val context: Context,
    private val settings: SettingsManager
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var activeAudioTrack: AudioTrack? = null
    private var speechJob: Job? = null

    // Cache recent neural TTS audio bytes by (voiceName + text) to save latency & quota
    private val pcmCache = ConcurrentHashMap<String, Pair<Int, ByteArray>>()

    init {
        tts = try {
            TextToSpeech(context.applicationContext, this)
        } catch (e: Throwable) {
            Log.e("NeuralVoiceEngine", "Failed to initialize TextToSpeech: ${e.message}")
            null
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            try {
                val chosen = settings.getSelectedVoice()
                configureAndroidTtsVoice(tts, chosen)
            } catch (e: Throwable) {
                Log.w("NeuralVoiceEngine", "Failed to configure TTS voice: ${e.message}")
            }
        }
    }

    fun stop() {
        speechJob?.cancel()
        speechJob = null
        try {
            activeAudioTrack?.pause()
            activeAudioTrack?.flush()
            activeAudioTrack?.release()
        } catch (_: Exception) {}
        activeAudioTrack = null
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
    }

    /**
     * Cleans markdown, bullets, and emojis so TTS speaks only pure natural human sentences
     * without reading emoji names or symbols robotically.
     */
    private fun cleanTextForSpeech(rawText: String): String {
        val withoutMarkdown = rawText
            .replace("*", "")
            .replace("#", "")
            .replace("`", "")
            .replace("_", " ")
            .replace("•", " ")
            .replace(Regex("https?://\\S+"), "")

        val sb = StringBuilder(withoutMarkdown.length)
        var i = 0
        while (i < withoutMarkdown.length) {
            val codePoint = withoutMarkdown.codePointAt(i)
            val charCount = Character.charCount(codePoint)
            val type = Character.getType(codePoint)
            val isEmojiOrSymbol =
                (codePoint in 0x1F000..0x1FAFF) ||
                (codePoint in 0x2600..0x27BF) ||
                (codePoint in 0xFE00..0xFE0F) ||
                (codePoint == 0x200D) ||
                (type == Character.OTHER_SYMBOL.toInt() && codePoint > 0x09FF)

            if (!isEmojiOrSymbol) {
                sb.appendCodePoint(codePoint)
            }
            i += charCount
        }

        return sb.toString()
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun speak(
        scope: CoroutineScope,
        rawText: String,
        voiceOverrideIndex: Int? = null,
        onStatusMessage: (String) -> Unit = {}
    ) {
        val cleanText = cleanTextForSpeech(rawText)
        if (cleanText.isBlank()) return

        stop()

        val voiceIndex = voiceOverrideIndex ?: settings.selectedVoiceIndex
        val chosenVoice = SettingsManager.voicesList.getOrElse(voiceIndex) {
            SettingsManager.voicesList[0]
        }

        speechJob = scope.launch(Dispatchers.IO) {
            val apiKey = settings.resolveMusicKey()

            // 1. Primary Path: Pure Bit-Perfect Gemini 2.5 Neural TTS API (11 Distinct Human Voices)
            // Played at native 1:1 24kHz PCM with zero WSOLA/PlaybackParams resampling distortion
            if (apiKey.isNotEmpty()) {
                val cacheKey = "${chosenVoice.geminiVoiceName}_${cleanText.take(260)}"
                val cached = pcmCache[cacheKey]
                if (cached != null) {
                    playPureNeuralPcmAudio(
                        pcmBytes = cached.second,
                        sampleRate = cached.first
                    )
                    return@launch
                }

                val neuralAudio = kotlinx.coroutines.withTimeoutOrNull(3800L) {
                    fetchGeminiNeuralTtsPcm(
                        apiKey = apiKey,
                        text = cleanText.take(360),
                        chosenVoice = chosenVoice
                    )
                }
                if (neuralAudio != null && neuralAudio.second.size > 200) {
                    if (pcmCache.size > 24) pcmCache.clear()
                    pcmCache[cacheKey] = neuralAudio
                    playPureNeuralPcmAudio(
                        pcmBytes = neuralAudio.second,
                        sampleRate = neuralAudio.first
                    )
                    return@launch
                }
            }

            // 2. Fallback Path: Direct High-Quality Android Neural TTS (No WAV double-pitching or low-pass muffling)
            withContext(Dispatchers.Main) {
                speakDirectAndroidTts(cleanText, chosenVoice)
            }
        }
    }

    private suspend fun fetchGeminiNeuralTtsPcm(
        apiKey: String,
        text: String,
        chosenVoice: VoiceOption
    ): Pair<Int, ByteArray>? {
        val candidateKeys = settings.getAllCandidateKeys(preferredKey = apiKey)
            .filter { !RetrofitClient.isOpenAiCompatibleKey(it) }
        if (candidateKeys.isEmpty()) return null

        val ttsModels = listOf(
            "gemini-2.5-flash-preview-tts",
            settings.musicModel
        ).filter { it.contains("tts", ignoreCase = true) }
            .ifEmpty { listOf("gemini-2.5-flash-preview-tts") }
            .distinct()

        for (currentKey in candidateKeys) {
            for (modelName in ttsModels) {
                try {
                    // Send clean Bengali/conversational text directly so the phoneme engine
                    // stays 100% in natural Bengali prosody without English prompt contamination.
                    val request = GenerateContentRequest(
                        contents = listOf(
                            Content(parts = listOf(Part(text = text)))
                        ),
                        generationConfig = GenerationConfig(
                            responseModalities = listOf("AUDIO"),
                            speechConfig = SpeechConfig(
                                voiceConfig = VoiceConfig(
                                    prebuiltVoiceConfig = PrebuiltVoiceConfig(
                                        voiceName = chosenVoice.geminiVoiceName
                                    )
                                )
                            )
                        )
                    )

                    val response = RetrofitClient.service.generateContent(
                        model = modelName,
                        apiKey = currentKey,
                        request = request
                    )

                    val inlineData = response.candidates
                        ?.firstOrNull()
                        ?.content
                        ?.parts
                        ?.firstOrNull { it.inlineData != null }
                        ?.inlineData

                    if (inlineData != null && inlineData.data.isNotBlank()) {
                        settings.lastVerifiedWorkingKey = currentKey
                        val rawBytes = Base64.decode(inlineData.data, Base64.DEFAULT)
                        val wavRate = readWavSampleRate(rawBytes)
                        val mimeRate = parseSampleRateFromMime(inlineData.mimeType)
                        val sampleRate = wavRate ?: mimeRate ?: 24000
                        val pcmBytes = stripWavHeaderIfPresent(rawBytes)
                        val cleanPcm = applyMicroFadeEdges(pcmBytes)
                        return Pair(sampleRate, cleanPcm)
                    }
                } catch (e: Exception) {
                    Log.w("NeuralVoiceEngine", "Gemini TTS ($modelName / ${chosenVoice.geminiVoiceName}) failed: ${e.message}")
                }
            }
        }
        return null
    }

    private fun parseSampleRateFromMime(mimeType: String?): Int? {
        if (mimeType.isNullOrBlank()) return null
        val match = Regex("rate=(\\d+)", RegexOption.IGNORE_CASE).find(mimeType)
        val rate = match?.groupValues?.getOrNull(1)?.toIntOrNull()
        return if (rate != null && rate in 8000..48000) rate else null
    }

    private fun speakDirectAndroidTts(text: String, chosenVoice: VoiceOption) {
        val engine = tts ?: return
        if (!isTtsReady) return
        configureAndroidTtsVoice(engine, chosenVoice)
        // Subtle, natural human pitch & rate — never extreme robotic pitch shifts
        engine.setPitch(chosenVoice.pitch.coerceIn(0.85f, 1.12f))
        engine.setSpeechRate(chosenVoice.rate.coerceIn(0.90f, 1.05f))
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "direct_${System.currentTimeMillis()}")
    }

    private fun configureAndroidTtsVoice(engine: TextToSpeech?, chosenVoice: VoiceOption) {
        if (engine == null) return
        val targetLocale = Locale.forLanguageTag(chosenVoice.lang)
        val langRes = engine.setLanguage(targetLocale)
        if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.forLanguageTag("bn-BD"))
        }

        try {
            val allVoices = engine.voices?.toList().orEmpty()
            val bengaliVoices = allVoices.filter {
                it.locale.language.equals("bn", ignoreCase = true)
            }
            if (bengaliVoices.isNotEmpty()) {
                // Sort by highest quality first so Android chooses natural neural voices over legacy robotic ones
                val sortedByQuality = bengaliVoices.sortedWith(
                    compareByDescending<Voice> { it.quality }
                        .thenBy { it.isNetworkConnectionRequired }
                        .thenBy { it.name }
                )
                val genderMatched = sortedByQuality.filter { v ->
                    val n = v.name.lowercase()
                    if (chosenVoice.gender == "male") {
                        (n.contains("male") && !n.contains("female")) ||
                            n.contains("bn-in-x-bin") ||
                            n.contains("bn-bd-x-ban") ||
                            n.contains("bn-in-x-bng") ||
                            n.contains("#male")
                    } else {
                        n.contains("female") ||
                            n.contains("bn-in-x-bnf") ||
                            n.contains("bn-in-x-bnx") ||
                            n.contains("bn-bd-x-baf") ||
                            n.contains("sfs")
                    }
                }
                val pool = if (genderMatched.isNotEmpty()) genderMatched else sortedByQuality
                val picked = pool[chosenVoice.index % pool.size]
                engine.voice = picked
            }
        } catch (_: Exception) {}
    }

    /**
     * Applies only a tiny 6ms fade-in and fade-out at the very start and end of the PCM buffer
     * to prevent hardware click/pop without altering the natural human frequencies at all.
     */
    private fun applyMicroFadeEdges(pcm: ByteArray): ByteArray {
        val totalSamples = pcm.size / 2
        if (totalSamples < 64) return pcm
        val out = pcm.copyOf()
        val fadeSamples = minOf(144, totalSamples / 8) // ~6ms at 24kHz

        for (s in 0 until fadeSamples) {
            val gain = s.toFloat() / fadeSamples.toFloat()
            // Fade-in sample
            val inIdx = s * 2
            val inLo = out[inIdx].toInt() and 0xFF
            val inHi = out[inIdx + 1].toInt()
            val inSample = (((inHi shl 8) or inLo).toShort() * gain).toInt().toShort()
            out[inIdx] = (inSample.toInt() and 0xFF).toByte()
            out[inIdx + 1] = ((inSample.toInt() shr 8) and 0xFF).toByte()

            // Fade-out sample
            val outIdx = (totalSamples - 1 - s) * 2
            val outLo = out[outIdx].toInt() and 0xFF
            val outHi = out[outIdx + 1].toInt()
            val outSample = (((outHi shl 8) or outLo).toShort() * gain).toInt().toShort()
            out[outIdx] = (outSample.toInt() and 0xFF).toByte()
            out[outIdx + 1] = ((outSample.toInt() shr 8) and 0xFF).toByte()
        }
        return out
    }

    private fun stripWavHeaderIfPresent(bytes: ByteArray): ByteArray {
        if (bytes.size > 44 &&
            bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte()
        ) {
            return bytes.copyOfRange(44, bytes.size)
        }
        return bytes
    }

    private fun readWavSampleRate(bytes: ByteArray): Int? {
        if (bytes.size < 28) return null
        if (bytes[0] != 'R'.code.toByte() || bytes[1] != 'I'.code.toByte()) return null
        val b0 = bytes[24].toInt() and 0xFF
        val b1 = bytes[25].toInt() and 0xFF
        val b2 = bytes[26].toInt() and 0xFF
        val b3 = bytes[27].toInt() and 0xFF
        val rate = b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
        return if (rate in 8000..48000) rate else null
    }

    /**
     * Plays Gemini 2.5 Neural TTS PCM at exact 1:1 hardware sample rate without PlaybackParams
     * time-stretching or phase-vocoder resampling so the voice remains 100% human and crystal clear.
     */
    private fun playPureNeuralPcmAudio(
        pcmBytes: ByteArray,
        sampleRate: Int
    ) {
        try {
            activeAudioTrack?.release()
        } catch (_: Exception) {}

        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, pcmBytes.size))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcmBytes, 0, pcmBytes.size)
            activeAudioTrack = track
            track.play()
        } catch (e: Exception) {
            Log.e("NeuralVoiceEngine", "AudioTrack playback error: ${e.message}")
        }
    }
}
