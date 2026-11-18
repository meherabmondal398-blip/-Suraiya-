package com.example

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

sealed class AssistantState {
    object Idle : AssistantState()
    object WakingUp : AssistantState()
    object ListeningCommand : AssistantState()
    object ProcessingCommand : AssistantState()
    data class ExecutingAction(val description: String) : AssistantState()
    data class SpeakingResponse(val text: String) : AssistantState()
}

/**
 * Controller for the Suraiya AI Voice Assistant.
 * Responds to the wake phrase “হেলো সুরাইয়া” / “হ্যালো সুরাইয়া” / “Hello Suraiya” / “সুরাইয়া”.
 * Immediately activates microphone and front-stage glowing animated emoji/orb,
 * then searches and answers whatever the user asks.
 */
class SuraiyaVoiceAssistant(
    private val context: Context,
    private val settings: SettingsManager,
    private val scope: CoroutineScope,
    private val neuralVoiceEngine: () -> NeuralVoiceEngine?,
    private val onExecuteAiMessage: (String) -> Unit,
    private val onRequestPermission: () -> Unit = {},
    private val onLaunchFallbackRecognizer: (Intent) -> Unit = {}
) {
    private val _assistantState = MutableStateFlow<AssistantState>(AssistantState.Idle)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _lastAssistantResponse = MutableStateFlow("")
    val lastAssistantResponse: StateFlow<String> = _lastAssistantResponse.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecognizerBusy = false
    private var isAppInForeground = true
    private var wakeListeningRestartJob: Job? = null
    private var consecutiveErrors = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    // Wake words prioritized: "হেলো সুরাইয়া" / "হ্যালো সুরাইয়া" / "Hello Suraiya"
    private val wakeWords = listOf(
        "হেলো সুরাইয়া", "হেলো সুরাইয়া", "হ্যালো সুরাইয়া", "হ্যালো সুরাইয়া",
        "হে সুরাইয়া", "হে সুরাইয়া", "হাই সুরাইয়া", "হাই সুরাইয়া",
        "hello suraiya", "hello suraya", "hey suraiya", "hey suraya",
        "hi suraiya", "hi suraya",
        "সুরাইয়া", "সুরাইয়া", "সুরইয়া", "সুরিয়া",
        "suraiya", "suraya", "suriya", "soraiya",
        "shuraiya", "shuraya"
    )

    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureRecognizerOnMainThread(): SpeechRecognizer? {
        if (speechRecognizer != null) return speechRecognizer
        return try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w("SuraiyaVoice", "Speech recognition service not available on device")
                null
            } else {
                SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(AssistantRecognitionListener())
                    speechRecognizer = this
                }
            }
        } catch (t: Throwable) {
            Log.e("SuraiyaVoice", "Error initializing recognizer: ${t.message}")
            speechRecognizer = null
            null
        }
    }

    fun onPermissionGranted() {
        consecutiveErrors = 0
        if (settings.voiceAssistantMasterEnabled && isAppInForeground) {
            scheduleWakeListeningRestart(delayMs = 400)
        }
    }

    fun onMasterSwitchChanged(isEnabled: Boolean) {
        settings.voiceAssistantMasterEnabled = isEnabled
        if (isEnabled) {
            consecutiveErrors = 0
            if (hasAudioPermission()) {
                startWakeListeningIfAllowed()
            } else {
                onRequestPermission()
            }
        } else {
            stopAllListening()
            _assistantState.value = AssistantState.Idle
            _liveTranscript.value = ""
        }
    }

    fun onResume() {
        isAppInForeground = true
        consecutiveErrors = 0
        if (settings.voiceAssistantMasterEnabled && hasAudioPermission()) {
            scheduleWakeListeningRestart(delayMs = 600)
        }
    }

    fun onPause() {
        isAppInForeground = false
        stopAllListening()
        neuralVoiceEngine()?.stop()
        if (_assistantState.value !is AssistantState.Idle) {
            _assistantState.value = AssistantState.Idle
        }
    }

    fun onDestroy() {
        isAppInForeground = false
        stopAllListening()
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
            } catch (_: Throwable) {}
            speechRecognizer = null
        }
    }

    /**
     * Manually wake or tap the orb to talk to Suraiya
     */
    fun manualActivate() {
        if (!hasAudioPermission()) {
            onRequestPermission()
            return
        }

        if (!settings.voiceAssistantMasterEnabled) {
            settings.voiceAssistantMasterEnabled = true
        }

        wakeUpAndListen(withGreeting = true)
    }

    fun cancelInteraction() {
        stopAllListening()
        neuralVoiceEngine()?.stop()
        _assistantState.value = AssistantState.Idle
        _liveTranscript.value = ""
        consecutiveErrors = 0
        if (settings.voiceAssistantMasterEnabled && hasAudioPermission()) {
            scheduleWakeListeningRestart(delayMs = 600)
        }
    }

    /**
     * Start listening for wake name “হেলো সুরাইয়া” when master switch is ON, permission granted, and state is Idle
     */
    fun startWakeListeningIfAllowed() {
        if (!settings.voiceAssistantMasterEnabled) return
        if (!isAppInForeground) return
        if (!hasAudioPermission()) return
        if (_assistantState.value !is AssistantState.Idle) return
        if (consecutiveErrors >= 5) return

        mainHandler.post {
            try {
                val recognizer = ensureRecognizerOnMainThread() ?: return@post
                if (isRecognizerBusy) return@post

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                isRecognizerBusy = true
                recognizer.startListening(intent)
            } catch (t: Throwable) {
                isRecognizerBusy = false
                Log.e("SuraiyaVoice", "Error starting wake listener: ${t.message}")
                consecutiveErrors++
                if (consecutiveErrors < 5) {
                    scheduleWakeListeningRestart(delayMs = 2000)
                }
            }
        }
    }

    private fun stopAllListening() {
        wakeListeningRestartJob?.cancel()
        wakeListeningRestartJob = null
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (_: Throwable) {}
            isRecognizerBusy = false
        }
    }

    private fun scheduleWakeListeningRestart(delayMs: Long = 1000) {
        if (!settings.voiceAssistantMasterEnabled || !isAppInForeground || !hasAudioPermission()) return
        if (_assistantState.value !is AssistantState.Idle) return
        if (consecutiveErrors >= 5) return

        wakeListeningRestartJob?.cancel()
        wakeListeningRestartJob = scope.launch(Dispatchers.Main) {
            delay(delayMs)
            if (isActive && settings.voiceAssistantMasterEnabled && isAppInForeground && hasAudioPermission() && _assistantState.value is AssistantState.Idle) {
                isRecognizerBusy = false
                startWakeListeningIfAllowed()
            }
        }
    }

    /**
     * Called when wake name “হেলো সুরাইয়া” / “সুরাইয়া” is detected
     */
    private fun onWakeWordDetected(fullSpokenText: String) {
        if (!settings.voiceAssistantMasterEnabled) return

        Log.d("SuraiyaVoice", "✨ Wake name detected: $fullSpokenText")
        stopAllListening()
        consecutiveErrors = 0

        val extractedCommand = extractCommandAfterWakeWord(fullSpokenText)

        if (extractedCommand.isNotBlank() && extractedCommand.length > 2) {
            _assistantState.value = AssistantState.ProcessingCommand
            _liveTranscript.value = extractedCommand
            processUserCommand(extractedCommand)
        } else {
            wakeUpAndListen(withGreeting = true)
        }
    }

    private fun wakeUpAndListen(withGreeting: Boolean) {
        _assistantState.value = AssistantState.WakingUp
        _liveTranscript.value = "“হেলো ${settings.aiDisplayName}”"

        if (withGreeting) {
            val greeting = "জি, বলুন! কি সার্চ করবো?"
            _lastAssistantResponse.value = greeting
            speak(greeting) {
                scope.launch(Dispatchers.Main) {
                    delay(150)
                    startListeningForCommand()
                }
            }
        } else {
            startListeningForCommand()
        }
    }

    /**
     * Turn microphone ON and listen for user's voice search command (Speech-to-Text)
     */
    private fun startListeningForCommand() {
        if (!settings.voiceAssistantMasterEnabled || !isAppInForeground || !hasAudioPermission()) {
            _assistantState.value = AssistantState.Idle
            return
        }

        stopAllListening()
        _assistantState.value = AssistantState.ListeningCommand
        _liveTranscript.value = "🎙️ মাইক অন • বলুন, কি সার্চ করবো?"

        mainHandler.post {
            try {
                val recognizer = ensureRecognizerOnMainThread()
                if (recognizer == null) {
                    launchFallbackRecognizerDialog()
                    return@post
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "বলুন, কি সার্চ করবো? (${settings.aiDisplayName} শুনছে)...")
                }
                isRecognizerBusy = true
                recognizer.startListening(intent)
            } catch (t: Throwable) {
                isRecognizerBusy = false
                Log.w("SuraiyaVoice", "startListening direct service failed, using fallback dialog: ${t.message}")
                launchFallbackRecognizerDialog()
            }
        }
    }

    private fun launchFallbackRecognizerDialog() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "বলুন, কি সার্চ করবো? (${settings.aiDisplayName} শুনছে)...")
        }
        try {
            onLaunchFallbackRecognizer(intent)
        } catch (e: Throwable) {
            _assistantState.value = AssistantState.Idle
            Log.e("SuraiyaVoice", "Fallback recognizer launch failed: ${e.message}")
        }
    }

    /**
     * Called when fallback SpeechRecognizer dialog returns text result
     */
    fun onSpokenText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            _assistantState.value = AssistantState.Idle
            return
        }

        if (isWakeWord(trimmed)) {
            onWakeWordDetected(trimmed)
        } else {
            _liveTranscript.value = trimmed
            processUserCommand(trimmed)
        }
    }

    /**
     * Process recognized voice command: whatever user speaks will be searched and answered!
     */
    fun processUserCommand(command: String) {
        val trimmed = command.trim()
        if (trimmed.isBlank()) {
            _assistantState.value = AssistantState.Idle
            scheduleWakeListeningRestart()
            return
        }

        _assistantState.value = AssistantState.ProcessingCommand
        _liveTranscript.value = trimmed

        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. YouTube Search / Music / Nasheed
        if (containsAny(lower, listOf("youtube", "ইউটিউব", "ইউটিউবে", "utube", "you tube", "নাশিদ", "গজল", "গান শোনাও", "গান বাজাও", "nasheed", "gojol", "play song"))) {
            val query = cleanYouTubeQuery(trimmed)
            _assistantState.value = AssistantState.ExecutingAction("YouTube: $query")
            val confirmation = if (query.isNotBlank()) "ইউটিউবে $query সার্চ করছি।" else "ইউটিউব ওপেন করছি।"
            _lastAssistantResponse.value = confirmation
            speak(confirmation) {
                executeYouTubeSearch(query)
                finishCommandExecution()
            }
            return
        }

        // 2. Time Query
        if (containsAny(lower, listOf("কয়টা বাজে", "কটা বাজে", "সময় কত", "টাইম কত", "time", "clock", "what time"))) {
            val timeResponse = formatCurrentTimeBengali()
            _assistantState.value = AssistantState.SpeakingResponse(timeResponse)
            _lastAssistantResponse.value = timeResponse
            speak(timeResponse) {
                finishCommandExecution()
            }
            return
        }

        // 3. Date Query
        if (containsAny(lower, listOf("তারিখ", "আজকে কি বার", "আজ কি বার", "date", "today's date", "দিন কি"))) {
            val dateResponse = formatCurrentDateBengali()
            _assistantState.value = AssistantState.SpeakingResponse(dateResponse)
            _lastAssistantResponse.value = dateResponse
            speak(dateResponse) {
                finishCommandExecution()
            }
            return
        }

        // 4. Camera
        if (containsAny(lower, listOf("ক্যামেরা", "ছবি তুলব", "ছবি তুলবো", "camera", "take photo"))) {
            _assistantState.value = AssistantState.ExecutingAction("Camera")
            val confirmation = "ক্যামেরা ওপেন করছি।"
            _lastAssistantResponse.value = confirmation
            speak(confirmation) {
                executeCamera()
                finishCommandExecution()
            }
            return
        }

        // 5. Phone / Dialer
        if (containsAny(lower, listOf("ফোন খোলো", "ডায়ালার", "ডায়ালার", "কল করো", "dialer", "open phone", "call"))) {
            _assistantState.value = AssistantState.ExecutingAction("Dialer")
            val confirmation = "ফোন ডায়ালার ওপেন করছি।"
            _lastAssistantResponse.value = confirmation
            speak(confirmation) {
                executeDialer()
                finishCommandExecution()
            }
            return
        }

        // 6. Search Everything!
        // As requested: "আর যা বলব তাই সার্চ করবে এই নামটা ধরে ডাকবো হেলো সুরাইয়া"
        val searchQuery = cleanWebSearchQuery(trimmed)
        _assistantState.value = AssistantState.ExecutingAction("সার্চ: $searchQuery")
        val confirmation = "“$searchQuery” সার্চ করছি..."
        _lastAssistantResponse.value = confirmation

        speak(confirmation) {
            executeWebSearch(searchQuery)
            // Also send query to AI so user gets both the instant web search and the AI voice answer!
            onExecuteAiMessage(searchQuery)
            finishCommandExecution()
        }
    }

    private fun finishCommandExecution() {
        scope.launch(Dispatchers.Main) {
            delay(800)
            _assistantState.value = AssistantState.Idle
            _liveTranscript.value = ""
            scheduleWakeListeningRestart()
        }
    }

    private fun executeYouTubeSearch(query: String) {
        val targetQuery = query.ifBlank { "বাংলা নাশিদ" }
        try {
            val appIntent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", targetQuery)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(appIntent)
        } catch (_: Throwable) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(targetQuery)}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try { context.startActivity(webIntent) } catch (_: Throwable) {}
        }
    }

    private fun executeWebSearch(query: String) {
        val targetQuery = query.ifBlank { "Google" }
        try {
            val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, targetQuery)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(searchIntent)
        } catch (_: Throwable) {
            val browserIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=${Uri.encode(targetQuery)}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try { context.startActivity(browserIntent) } catch (_: Throwable) {}
        }
    }

    private fun executeCamera() {
        try {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Throwable) {}
    }

    private fun executeDialer() {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Throwable) {}
    }

    private fun speak(text: String, onDone: (() -> Unit)? = null) {
        val engine = neuralVoiceEngine()
        if (engine != null) {
            engine.speak(
                scope = scope,
                rawText = text
            )
            scope.launch(Dispatchers.Main) {
                val words = text.split(" ").size
                val estimatedMs = maxOf(1400L, (words * 320L))
                delay(estimatedMs)
                onDone?.invoke()
            }
        } else {
            onDone?.invoke()
        }
    }

    private fun extractCommandAfterWakeWord(text: String): String {
        var cleaned = text
        val dynamicCandidates = listOf(
            settings.aiDisplayName,
            settings.aiDisplayName.replace("AI", "", ignoreCase = true).trim(),
            "হেলো " + settings.aiDisplayName,
            "হ্যালো " + settings.aiDisplayName,
            "hello " + settings.aiDisplayName,
            "hey " + settings.aiDisplayName
        ).filter { it.isNotBlank() }
        val allWords = (dynamicCandidates + wakeWords).distinct()

        for (w in allWords) {
            val idx = cleaned.indexOf(w, ignoreCase = true)
            if (idx != -1) {
                cleaned = cleaned.substring(idx + w.length).trim()
                break
            }
        }
        return cleaned
            .replace(Regex("^[\\s,.:;!?-]+"), "")
            .trim()
    }

    private fun isWakeWord(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT).trim()
        val customName = settings.aiDisplayName.lowercase(Locale.ROOT).trim()
        val customClean = customName.replace("ai", "").trim()
        val matchesDefault = wakeWords.any { lower.contains(it) }
        val matchesCustom = (customName.isNotBlank() && lower.contains(customName)) ||
                (customClean.length >= 3 && lower.contains(customClean))
        return matchesDefault || matchesCustom
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }

    private fun cleanYouTubeQuery(text: String): String {
        return text
            .replace(Regex("(?i)youtube-এ|youtube এ|ইউটিউবে|ইউটিউব-এ|ইউটিউব|you tube|utube"), "")
            .replace(Regex("(?i)সার্চ করো|সার্চ কর|খুঁজো|খোঁজো|খুঁজে দাও|চালাও|বাজাও|প্লে করো|search|play"), "")
            .trim()
            .ifBlank { "বাংলা নাশিদ" }
    }

    private fun cleanWebSearchQuery(text: String): String {
        return text
            .replace(Regex("(?i)গুগলে|গুগল-এ|গুগল|google-এ|google|web search"), "")
            .replace(Regex("(?i)সার্চ করো|সার্চ কর|খুঁজো|খোঁজো|খুঁজে দাও|search for|search|সম্পর্কে বলো|বলো|জানাও"), "")
            .trim()
            .ifBlank { text }
    }

    private fun formatCurrentTimeBengali(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR)
        val actualHour = if (hour == 0) 12 else hour
        val minute = calendar.get(Calendar.MINUTE)
        val amPm = if (calendar.get(Calendar.AM_PM) == Calendar.AM) "সকাল" else "দুপুর/সন্ধ্যা"

        val bnDigits = mapOf('0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪', '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯')
        val hourBn = actualHour.toString().map { bnDigits[it] ?: it }.joinToString("")
        val minuteBn = minute.toString().map { bnDigits[it] ?: it }.joinToString("")

        return "এখন সময় $amPm $hourBn টা বেজে $minuteBn মিনিট।"
    }

    private fun formatCurrentDateBengali(): String {
        val sdf = SimpleDateFormat("EEEE, d MMMM, yyyy", Locale("bn", "BD"))
        val dateStr = sdf.format(Date())
        return "আজকে $dateStr।"
    }

    private inner class AssistantRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            isRecognizerBusy = true
            if (_assistantState.value is AssistantState.ListeningCommand) {
                _liveTranscript.value = "🎙️ মাইক অন • বলুন, কি সার্চ করবো?"
            }
        }

        override fun onBeginningOfSpeech() {
            isRecognizerBusy = true
        }

        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            isRecognizerBusy = false
        }

        override fun onError(error: Int) {
            isRecognizerBusy = false

            // Normal timeouts / silence should not count as fatal errors
            val isNormalSilence = error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT

            if (!isNormalSilence) {
                consecutiveErrors++
            }

            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                _assistantState.value = AssistantState.Idle
                return
            }

            if (error == SpeechRecognizer.ERROR_CLIENT && _assistantState.value is AssistantState.ListeningCommand) {
                launchFallbackRecognizerDialog()
                return
            }

            if (consecutiveErrors >= 5) {
                Log.w("SuraiyaVoice", "Speech recognition paused after errors ($error). Ready on demand.")
                _assistantState.value = AssistantState.Idle
                return
            }

            when (_assistantState.value) {
                is AssistantState.ListeningCommand -> {
                    _assistantState.value = AssistantState.Idle
                    _liveTranscript.value = ""
                    scheduleWakeListeningRestart(delayMs = 1200)
                }
                is AssistantState.Idle -> {
                    scheduleWakeListeningRestart(delayMs = 1000)
                }
                else -> {
                    scheduleWakeListeningRestart(delayMs = 1500)
                }
            }
        }

        override fun onResults(results: Bundle?) {
            isRecognizerBusy = false
            consecutiveErrors = 0
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            val text = matches.firstOrNull().orEmpty().trim()

            if (text.isBlank()) {
                if (_assistantState.value is AssistantState.ListeningCommand) {
                    _assistantState.value = AssistantState.Idle
                }
                scheduleWakeListeningRestart()
                return
            }

            when (val state = _assistantState.value) {
                is AssistantState.Idle -> {
                    if (isWakeWord(text)) {
                        onWakeWordDetected(text)
                    } else {
                        scheduleWakeListeningRestart(delayMs = 500)
                    }
                }
                is AssistantState.ListeningCommand -> {
                    _liveTranscript.value = text
                    processUserCommand(text)
                }
                else -> {
                    scheduleWakeListeningRestart()
                }
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partialList = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            val partial = partialList.firstOrNull().orEmpty().trim()
            if (partial.isBlank()) return

            when (_assistantState.value) {
                is AssistantState.Idle -> {
                    if (isWakeWord(partial)) {
                        onWakeWordDetected(partial)
                    }
                }
                is AssistantState.ListeningCommand -> {
                    _liveTranscript.value = "বলছেন: $partial"
                }
                else -> {}
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
