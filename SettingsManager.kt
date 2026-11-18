package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VoiceOption(
    val index: Int,
    val name: String,
    val shortName: String,
    val lang: String,
    val gender: String, // "female" or "male"
    val geminiVoiceName: String, // Distinct Gemini Neural TTS Prebuilt Voice
    val stylePrompt: String, // Vocal acting instruction for Gemini TTS
    val neuralPitch: Float, // Subtle AudioTrack pitch for neural audio (0.88f - 1.12f)
    val neuralSpeed: Float, // Subtle AudioTrack speed for neural audio (0.92f - 1.08f)
    val pitch: Float, // Offline TTS fallback pitch
    val rate: Float // Offline TTS fallback rate
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("suraiya_settings", Context.MODE_PRIVATE)

    companion object {
        const val DEFAULT_AI_NAME = "Suraiya AI"

        // ১১টি আলাদা মিষ্টি ও ন্যাচারাল ভয়েসের তালিকা (সুরাইয়া স্পেশাল ১টি + মেয়ে ৫টি + ছেলে ৫টি)
        val voicesList: List<VoiceOption> = listOf(
            // 👑 স্পেশাল সিগনেচার ভয়েস: সুরাইয়া (Sweetest & Ultra-Realistic Human Voice)
            VoiceOption(
                index = 0,
                name = "👑 স্পেশাল: সুরাইয়া (Sweetest & Realistic Human Voice)",
                shortName = "👑 মায়াবী সুরাইয়া",
                lang = "bn-BD",
                gender = "female",
                geminiVoiceName = "Aoede",
                stylePrompt = "",
                neuralPitch = 1.00f,
                neuralSpeed = 1.00f,
                pitch = 1.02f,
                rate = 0.96f
            ),

            // 🌸 মেয়েদের ভয়েস (Female Voices - 5 Distinct Neural Voices)
            VoiceOption(
                index = 1,
                name = "মেয়ে ১: মিষ্টি মিষ্টি রিয়া (Sweet & Gentle)",
                shortName = "🌸 মিষ্টি রিয়া",
                lang = "bn-IN",
                gender = "female",
                geminiVoiceName = "Sulafat",
                stylePrompt = "",
                neuralPitch = 1.00f,
                neuralSpeed = 1.00f,
                pitch = 1.06f,
                rate = 0.95f
            ),
            VoiceOption(
                index = 2,
                name = "মেয়ে ২: কিউট কিউট স্নেহা (Cute & Energetic)",
                shortName = "🌸 কিউট স্নেহা",
                lang = "bn-IN",
                gender = "female",
                geminiVoiceName = "Kore",
                stylePrompt = "",
                neuralPitch = 1.00f,
                neuralSpeed = 1.00f,
                pitch = 1.10f,
                rate = 1.00f
            ),
            VoiceOption(
                index = 3,
                name = "মেয়ে ৩: সফট সফট তানিয়া (Soft & Calm)",
                shortName = "🌸 সফট তানিয়া",
                lang = "bn-BD",
                gender = "female",
                geminiVoiceName = "Leda",
                stylePrompt = "",
                neuralPitch = 1.00f,
                neuralSpeed = 1.00f,
                pitch = 0.98f,
                rate = 0.93f
            ),
            VoiceOption(
                index = 4,
                name = "মেয়ে ৪: ফ্রেন্ডলি প্রিয়া (Friendly & Warm)",
                shortName = "🌸 ফ্রেন্ডলি প্রিয়া",
                lang = "bn-IN",
                gender = "female",
                geminiVoiceName = "Zephyr",
                stylePrompt = "",
                neuralPitch = 1.00f,
                neuralSpeed = 1.00f,
                pitch = 1.04f,
                rate = 0.98f
            ),
            VoiceOption(
                index = 5,
                name = "মেয়ে ৫: স্মার্ট পায়েল (Smart Assistant)",
                shortName = "🌸 স্মার্ট পায়েল",
                lang = "bn-IN",
                gender = "female",
                geminiVoiceName = "Callirrhoe",
                stylePrompt = "",
                neuralPitch = 1.00f,
                neuralSpeed = 1.00f,
                pitch = 1.00f,
                rate = 0.99f
            ),

            // 👦 ছেলেদের ভয়েস (Male Voices - 5 Distinct Neural Voices)
            VoiceOption(
                index = 6,
                name = "ছেলে ১: স্মার্ট রকি (Smart & Young)",
                shortName = "👦 স্মার্ট রকি",
                lang = "bn-IN",
                gender = "male",
                geminiVoiceName = "Puck",
                stylePrompt = "",
                neuralPitch = 1.00f,
                neuralSpeed = 1.00f,
                pitch = 0.93f,
                rate = 0.98f
            ),
            VoiceOption(
                index = 7,
                name = "ছেলে ২: ডিপ রাহাত (Deep & Clear)",
                shortName = "👦 ডিপ রাহাত",
                lang = "bn-BD",
                gender = "male",
                geminiVoiceName = "Charon",
                stylePrompt = "",
                neuralPitch = 1.00f,
                neuralSpeed = 1.00f,
                pitch = 0.86f,
                rate = 0.95f
            ),
            VoiceOption(
                index = 8,
                name = "ছেলে ৩: ফ্রেন্ডলি আবীর (Friendly Boy)",
                shortName = "👦 ফ্রেন্ডলি আবীর",
                lang = "bn-IN",
                gender = "male",
                geminiVoiceName = "Fenrir",
                stylePrompt = "",
                neuralPitch = 1.00f,
                neuralSpeed = 1.00f,
                pitch = 0.90f,
                rate = 0.97f
            ),
            VoiceOption(
                index = 9,
                name = "ছেলে ৪: এনার্জেটিক অনিক (Energetic Voice)",
                shortName = "👦 এনার্জেটিক অনিক",
                lang = "bn-IN",
                gender = "male",
                geminiVoiceName = "Orus",
                stylePrompt = "",
                neuralPitch = 1.00f,
                neuralSpeed = 1.00f,
                pitch = 0.95f,
                rate = 1.01f
            ),
            VoiceOption(
                index = 10,
                name = "ছেলে ৫: কুল শুভ (Cool & Calm)",
                shortName = "👦 কুল শুভ",
                lang = "bn-BD",
                gender = "male",
                geminiVoiceName = "Iapetus",
                stylePrompt = "",
                neuralPitch = 1.00f,
                neuralSpeed = 1.00f,
                pitch = 0.88f,
                rate = 0.94f
            )
        )
    }

    // ==========================================
    // DYNAMIC BRANDING ARCHITECTURE: AI Assistant Name
    // ==========================================
    private val _aiDisplayNameFlow = MutableStateFlow(getAIName())
    val aiDisplayNameFlow: StateFlow<String> = _aiDisplayNameFlow.asStateFlow()

    var aiDisplayName: String
        get() = getAIName()
        set(value) {
            val clean = value.trim()
            val finalName = if (clean.isBlank()) DEFAULT_AI_NAME else clean
            prefs.edit().putString("ai_display_name", finalName).apply()
            _aiDisplayNameFlow.value = finalName
        }

    fun getAIName(): String {
        return prefs.getString("ai_display_name", DEFAULT_AI_NAME)?.trim()?.ifBlank { DEFAULT_AI_NAME } ?: DEFAULT_AI_NAME
    }

    fun saveAIName(name: String): Boolean {
        val clean = name.trim()
        if (clean.isBlank()) return false
        aiDisplayName = clean
        return true
    }

    fun resetAIName() {
        aiDisplayName = DEFAULT_AI_NAME
    }

    fun getAIInitials(name: String = aiDisplayName): String {
        val clean = name.trim()
        if (clean.isBlank()) return "AI"
        val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            words.size >= 2 -> "${words[0].take(1)}${words[1].take(1)}".uppercase()
            clean.length >= 2 -> clean.take(2).uppercase()
            else -> clean.uppercase()
        }
    }

    // Master Voice Assistant Control: Single Master Switch required by user
    var voiceAssistantMasterEnabled: Boolean
        get() = prefs.getBoolean("voice_assistant_master_enabled", true)
        set(value) = prefs.edit().putBoolean("voice_assistant_master_enabled", value).apply()

    // 1. Core Chat & Gemini Box
    var geminiKey: String
        get() = prefs.getString("gemini_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_key", value.cleanApiKey()).apply()

    var chatModel: String
        get() {
            val saved = prefs.getString("chat_model", "gemini-3.5-flash") ?: "gemini-3.5-flash"
            return if (saved == "gemini-2.5-flash" || saved == "gemini-2.0-flash" || saved == "gemini-1.5-flash" || saved == "gemini-pro" || saved.isBlank()) {
                "gemini-3.5-flash"
            } else {
                saved
            }
        }
        set(value) = prefs.edit().putString("chat_model", value).apply()

    var aiPersona: String
        get() = prefs.getString("ai_persona", "loving") ?: "loving"
        set(value) = prefs.edit().putString("ai_persona", value).apply()

    var temperature: Float
        get() = prefs.getFloat("temperature", 0.7f)
        set(value) = prefs.edit().putFloat("temperature", value).apply()

    var conversationalMemoryEnabled: Boolean
        get() = prefs.getBoolean("conversational_memory_enabled", true)
        set(value) = prefs.edit().putBoolean("conversational_memory_enabled", value).apply()

    // 2. Image Generation & Gallery Pro Remake Box
    var imageApiKey: String
        get() = prefs.getString("image_api_key", "") ?: ""
        set(value) = prefs.edit().putString("image_api_key", value.cleanApiKey()).apply()

    var imageModel: String
        get() = prefs.getString("image_model", "gemini-2.5-flash-image") ?: "gemini-2.5-flash-image"
        set(value) = prefs.edit().putString("image_model", value).apply()

    var imageAspectRatio: String
        get() = prefs.getString("image_aspect_ratio", "1:1") ?: "1:1"
        set(value) = prefs.edit().putString("image_aspect_ratio", value).apply()

    var imageEnhancementStyle: String
        get() = prefs.getString("image_enhancement_style", "studio_hd") ?: "studio_hd"
        set(value) = prefs.edit().putString("image_enhancement_style", value).apply()

    var autoEnhanceGallery: Boolean
        get() = prefs.getBoolean("auto_enhance_gallery", true)
        set(value) = prefs.edit().putBoolean("auto_enhance_gallery", value).apply()

    // 3. Video Generation (Veo) Box
    var videoApiKey: String
        get() = prefs.getString("video_api_key", "") ?: ""
        set(value) = prefs.edit().putString("video_api_key", value.cleanApiKey()).apply()

    var videoModel: String
        get() = prefs.getString("video_model", "veo-3.1-fast-generate-preview") ?: "veo-3.1-fast-generate-preview"
        set(value) = prefs.edit().putString("video_model", value).apply()

    var videoResolution: String
        get() = prefs.getString("video_resolution", "1080p") ?: "1080p"
        set(value) = prefs.edit().putString("video_resolution", value).apply()

    var videoAspectRatio: String
        get() = prefs.getString("video_aspect_ratio", "16:9") ?: "16:9"
        set(value) = prefs.edit().putString("video_aspect_ratio", value).apply()

    // 4. Music, Song & Voice Box
    var voiceKey: String
        get() = prefs.getString("voice_key", "") ?: ""
        set(value) = prefs.edit().putString("voice_key", value.cleanApiKey()).apply()

    var musicModel: String
        get() = prefs.getString("music_model", "gemini-2.5-flash-preview-tts") ?: "gemini-2.5-flash-preview-tts"
        set(value) = prefs.edit().putString("music_model", value).apply()

    var musicGenre: String
        get() = prefs.getString("music_genre", "রোমান্টিক মেলোডি") ?: "রোমান্টিক মেলোডি"
        set(value) = prefs.edit().putString("music_genre", value).apply()

    var language: String
        get() = prefs.getString("language", "bn") ?: "bn"
        set(value) = prefs.edit().putString("language", value).apply()

    var selectedVoiceIndex: Int
        get() = prefs.getInt("selected_voice_index", 0).coerceIn(0, voicesList.lastIndex)
        set(value) = prefs.edit().putInt("selected_voice_index", value.coerceIn(0, voicesList.lastIndex)).apply()

    var voiceTone: Int
        get() = prefs.getInt("voice_tone", 1)
        set(value) = prefs.edit().putInt("voice_tone", value).apply()

    fun getSelectedVoice(): VoiceOption {
        return voicesList.getOrElse(selectedVoiceIndex) { voicesList[0] }
    }

    var autoSpeak: Boolean
        get() = prefs.getBoolean("auto_speak", true)
        set(value) = prefs.edit().putBoolean("auto_speak", value).apply()

    var activeSessionId: String
        get() = prefs.getString("active_session_id", "user_1") ?: "user_1"
        set(value) = prefs.edit().putString("active_session_id", value.trim().ifBlank { "user_1" }).apply()

    // 5. User Profile & Professional Smart Keyboard Settings
    var userName: String
        get() = prefs.getString("user_name", "মেহেরাব") ?: "মেহেরাব"
        set(value) = prefs.edit().putString("user_name", value.trim().ifBlank { "মেহেরাব" }).apply()

    var smartKeyboardEnabled: Boolean
        get() = prefs.getBoolean("smart_keyboard_enabled", true)
        set(value) = prefs.edit().putBoolean("smart_keyboard_enabled", value).apply()

    var smartKeyboardDefaultLang: String
        get() = prefs.getString("smart_keyboard_default_lang", "bn_direct") ?: "bn_direct"
        set(value) = prefs.edit().putString("smart_keyboard_default_lang", value).apply()

    var banglaAutoCorrect: Boolean
        get() = prefs.getBoolean("bangla_auto_correct", true)
        set(value) = prefs.edit().putBoolean("bangla_auto_correct", value).apply()

    var wordPredictionEnabled: Boolean
        get() = prefs.getBoolean("word_prediction_enabled", true)
        set(value) = prefs.edit().putBoolean("word_prediction_enabled", value).apply()

    var keyboardHapticEnabled: Boolean
        get() = prefs.getBoolean("keyboard_haptic_enabled", true)
        set(value) = prefs.edit().putBoolean("keyboard_haptic_enabled", value).apply()

    fun getClipboardHistory(): List<String> {
        val raw = prefs.getString("smart_clipboard_history", "") ?: ""
        if (raw.isBlank()) {
            return listOf(
                "হ্যালো সুরাইয়া, কেমন আছো?",
                "আমাকে ধাপে ধাপে সহজ বাংলায় বুঝিয়ে বলো।",
                "একটা সুন্দর রোমান্টিক বাংলা গান তৈরি করো।"
            )
        }
        return raw.split("||CLIP||").filter { it.isNotBlank() }
    }

    fun addClipboardItem(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val updated = (listOf(clean) + getClipboardHistory().filterNot { it == clean }).take(15)
        prefs.edit().putString("smart_clipboard_history", updated.joinToString("||CLIP||")).apply()
    }

    fun clearClipboardHistory() {
        prefs.edit().remove("smart_clipboard_history").apply()
    }

    private val invalidPlaceholderKeys = setOf(
        "",
        "MY_GEMINI_API_KEY",
        "YOUR_GEMINI_API_KEY_HERE",
        "YOUR_API_KEY",
        "YOUR_API_KEY_HERE",
        "API_KEY",
        "null",
        "undefined"
    )

    var lastVerifiedWorkingKey: String
        get() = prefs.getString("last_verified_working_key", "") ?: ""
        set(value) = prefs.edit().putString("last_verified_working_key", value.cleanApiKey()).apply()

    fun cleanRawKeyInput(input: String): String = input.cleanApiKey()

    private fun String.cleanApiKey(): String {
        val stripped = this.replace(Regex("[\\u200B-\\u200D\\uFEFF\\u00A0\\s\\r\\n\"'`]+"), "").trim()
        val raw = stripped.substringAfterLast("=").trim()
        if (raw.isEmpty()) return ""

        // 1. Direct match for Google Gemini API key (AIza...) or OpenAI/Groq/OpenRouter keys
        val match = Regex("(AIza[0-9A-Za-z_\\-]{30,45}|sk-or-v1-[0-9A-Za-z_\\-]{20,}|gsk_[0-9A-Za-z_\\-]{20,}|sk-[0-9A-Za-z_\\-]{20,})").find(raw)
        if (match != null) {
            return match.value.trim()
        }

        return if (raw in invalidPlaceholderKeys) "" else raw
    }

    fun extractApiKeyFromText(text: String): String? {
        val stripped = text.replace(Regex("[\\u200B-\\u200D\\uFEFF\\u00A0\\s\\r\\n\"'`]+"), "").trim()
        val match = Regex("(AIza[0-9A-Za-z_\\-]{30,45}|sk-or-v1-[0-9A-Za-z_\\-]{20,}|gsk_[0-9A-Za-z_\\-]{20,}|sk-[0-9A-Za-z_\\-]{20,})").find(stripped)
        if (match != null) return match.value.trim()
        return null
    }

    private fun String.isValidEffectiveKey(): Boolean =
        this.isNotEmpty() && this !in invalidPlaceholderKeys && this.length >= 20

    fun getAllCandidateKeys(preferredKey: String? = null): List<String> {
        return listOfNotNull(
            preferredKey?.cleanApiKey(),
            geminiKey.cleanApiKey(),
            imageApiKey.cleanApiKey(),
            videoApiKey.cleanApiKey(),
            voiceKey.cleanApiKey(),
            lastVerifiedWorkingKey.cleanApiKey(),
            BuildConfig.GEMINI_API_KEY.cleanApiKey()
        ).filter { it.isValidEffectiveKey() }.distinct()
    }

    private fun firstAvailableKey(): String {
        return getAllCandidateKeys().firstOrNull().orEmpty()
    }

    fun hasValidApiKey(): Boolean = getAllCandidateKeys().isNotEmpty()

    fun maskKey(rawKey: String): String {
        val key = rawKey.cleanApiKey()
        if (!key.isValidEffectiveKey()) return ""
        return if (key.length > 10) "${key.take(6)}...${key.takeLast(4)}" else "সক্রিয় Key"
    }

    fun getMaskedActiveKey(): String = maskKey(resolveChatKey())

    fun syncKeyToAllBoxes(rawKey: String) {
        val clean = rawKey.cleanApiKey()
        if (!clean.isValidEffectiveKey()) return
        geminiKey = clean
        imageApiKey = clean
        videoApiKey = clean
        voiceKey = clean
        lastVerifiedWorkingKey = clean
    }

    fun autoFillEmptyBoxesFromAvailableKey() {
        val best = firstAvailableKey()
        if (!best.isValidEffectiveKey()) return
        if (!geminiKey.cleanApiKey().isValidEffectiveKey()) geminiKey = best
        if (!imageApiKey.cleanApiKey().isValidEffectiveKey()) imageApiKey = best
        if (!videoApiKey.cleanApiKey().isValidEffectiveKey()) videoApiKey = best
        if (!voiceKey.cleanApiKey().isValidEffectiveKey()) voiceKey = best
    }

    // Helper functions to resolve effective API keys with universal fallback across all 4 boxes + BuildConfig
    fun resolveChatKey(): String = geminiKey.cleanApiKey().takeIf { it.isValidEffectiveKey() } ?: firstAvailableKey()
    fun resolveImageKey(): String = imageApiKey.cleanApiKey().takeIf { it.isValidEffectiveKey() } ?: firstAvailableKey()
    fun resolveVideoKey(): String = videoApiKey.cleanApiKey().takeIf { it.isValidEffectiveKey() } ?: firstAvailableKey()
    fun resolveMusicKey(): String = voiceKey.cleanApiKey().takeIf { it.isValidEffectiveKey() } ?: firstAvailableKey()
}

