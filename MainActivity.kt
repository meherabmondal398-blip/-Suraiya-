package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()
    private var neuralVoiceEngine: NeuralVoiceEngine? = null
    private val melodySynthesizer = MelodySynthesizer()
    private var voiceAssistant: SuraiyaVoiceAssistant? = null

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceAssistant?.onPermissionGranted()
        } else {
            Toast.makeText(this, "মাইক্রোফোন অনুমতি ছাড়া '${viewModel.settings.aiDisplayName}' ভয়েস অ্যাসিস্ট্যান্ট কাজ করতে পারবে না।", Toast.LENGTH_LONG).show()
        }
    }

    private val speechRecognizerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (spokenText.isNotBlank()) {
                voiceAssistant?.onSpokenText(spokenText)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        neuralVoiceEngine = NeuralVoiceEngine(this, viewModel.settings)

        voiceAssistant = SuraiyaVoiceAssistant(
            context = this,
            settings = viewModel.settings,
            scope = lifecycleScope,
            neuralVoiceEngine = { neuralVoiceEngine },
            onExecuteAiMessage = { query -> viewModel.sendMessage(query) },
            onRequestPermission = {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onLaunchFallbackRecognizer = { intent ->
                speechRecognizerLauncher.launch(intent)
            }
        )

        // Check audio permission and request if master switch is ON
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAudioPermission && viewModel.settings.voiceAssistantMasterEnabled) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            val assistantState by (voiceAssistant?.assistantState ?: MutableStateFlow(AssistantState.Idle)).collectAsStateWithLifecycle()
            val assistantTranscript by (voiceAssistant?.liveTranscript ?: MutableStateFlow("")).collectAsStateWithLifecycle()

            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ChatScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel,
                        melodySynthesizer = melodySynthesizer,
                        assistantState = assistantState,
                        assistantTranscript = assistantTranscript,
                        onToggleVoiceAssistant = { isEnabled ->
                            voiceAssistant?.onMasterSwitchChanged(isEnabled)
                        },
                        onCancelAssistant = {
                            voiceAssistant?.cancelInteraction()
                        },
                        onAssistantSuggestion = { suggestion ->
                            voiceAssistant?.processUserCommand(suggestion)
                        },
                        onMicClick = { onMicButtonClicked() },
                        onSpeakText = { text -> speakOut(text) },
                        onStopSpeech = {
                            neuralVoiceEngine?.stop()
                            melodySynthesizer.stop()
                        }
                    )
                }
            }
        }
    }

    private fun onMicButtonClicked() {
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasAudioPermission) {
            voiceAssistant?.manualActivate()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun speakOut(text: String) {
        neuralVoiceEngine?.speak(
            scope = lifecycleScope,
            rawText = text
        )
    }

    override fun onResume() {
        super.onResume()
        voiceAssistant?.onResume()
    }

    override fun onPause() {
        voiceAssistant?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        voiceAssistant?.onDestroy()
        melodySynthesizer.stop()
        neuralVoiceEngine?.shutdown()
        super.onDestroy()
    }
}

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel,
    melodySynthesizer: MelodySynthesizer,
    assistantState: AssistantState = AssistantState.Idle,
    assistantTranscript: String = "",
    onToggleVoiceAssistant: (Boolean) -> Unit = {},
    onCancelAssistant: () -> Unit = {},
    onAssistantSuggestion: (String) -> Unit = {},
    onMicClick: () -> Unit,
    onSpeakText: (String) -> Unit,
    onStopSpeech: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()
    val pendingImage by viewModel.pendingImage.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val galleryItems by viewModel.galleryItems.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val aiDisplayName by viewModel.aiDisplayName.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showPlusMenu by remember { mutableStateOf(false) }
    var showBanglaKeyboard by remember { mutableStateOf(false) }
    var showLiveStudioScreen by remember { mutableStateOf(false) }
    var selectedBottomTab by remember { mutableIntStateOf(0) } // 0: চ্যাট, 1: স্মৃতি, 2: গ্যালারি
    var playingSongMessageId by remember { mutableStateOf<String?>(null) }
    var inspectedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val isListening = assistantState is AssistantState.ListeningCommand
    val liveTranscript = assistantTranscript

    // Voice Assistant Glowing Animation & Command Overlay
    SuraiyaVoiceAssistantOverlay(
        state = assistantState,
        liveTranscript = assistantTranscript,
        aiDisplayName = aiDisplayName,
        onCancel = onCancelAssistant,
        onSuggestionClick = onAssistantSuggestion
    )

    if (showLiveStudioScreen) {
        SuraiyaLiveStudioScreen(
            aiDisplayName = aiDisplayName,
            isListening = isListening,
            liveTranscript = liveTranscript,
            onMicToggle = onMicClick,
            onClose = { showLiveStudioScreen = false },
            onSendMessage = { prompt ->
                showLiveStudioScreen = false
                viewModel.sendMessage(prompt)
            }
        )
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Back handler when Bangla keyboard, plus menu, or secondary tab is open
    BackHandler(enabled = showBanglaKeyboard || selectedBottomTab != 0 || showPlusMenu) {
        when {
            showBanglaKeyboard -> showBanglaKeyboard = false
            showPlusMenu -> showPlusMenu = false
            else -> selectedBottomTab = 0
        }
    }

    // Zero-permission Android Photo Picker for Gallery Image Upload & Pro Remake
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = StudioMediaHelper.loadBitmapFromUri(context, uri)
            if (bitmap != null) {
                viewModel.attachImage(bitmap, switchToProRemake = true)
                selectedBottomTab = 0
                showPlusMenu = false
                Toast.makeText(context, "✨ ছবি যুক্ত হয়েছে! প্রফেশনাল রিমেক রেডি", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.attachImage(bitmap, switchToProRemake = true)
            selectedBottomTab = 0
            showPlusMenu = false
            Toast.makeText(context, "📷 ক্যামেরার ছবি যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
        }
    }

    var activeVoiceBadge by remember { mutableStateOf(viewModel.settings.getSelectedVoice().shortName) }

    if (showSettings) {
        PremierSettingsDialog(
            settings = viewModel.settings,
            onTestVoice = { index ->
                val chosen = SettingsManager.voicesList.getOrElse(index) { SettingsManager.voicesList[0] }
                activeVoiceBadge = chosen.shortName
                val sampleText = when (chosen.index) {
                    0 -> "হ্যালো প্রিয়! আমি তোমার $aiDisplayName বলছি। সবথেকে মিষ্টি আর মানুষের মতো জীবন্ত কণ্ঠে তোমার সাথে কথা বলতে ও গান শোনাতে আমি সবসময় আছি।"
                    1 -> "হ্যালো প্রিয়! আমি মিষ্টি রিয়া বলছি। আমার কোমল কণ্ঠে তোমার সাথে গল্প করতে খুব ভালো লাগবে।"
                    2 -> "হাই! আমি কিউট স্নেহা! আজ আমরা দারুণ মজার সব আইডিয়া আর ছবি তৈরি করবো!"
                    3 -> "আসসালামু আলাইকুম, আমি তানিয়া। শান্ত ও স্নিগ্ধভাবে তোমার সব প্রশ্নের উত্তর দিতে আমি প্রস্তুত।"
                    4 -> "হ্যালো বন্ধু! আমি প্রিয়া বলছি। বলো আজ তোমার দিনটা কেমন কাটছে?"
                    5 -> "নমস্কার, আমি স্মার্ট অ্যাসিস্ট্যান্ট পায়েল। তোমার যেকোনো প্রফেশনাল কাজে আমি সাহায্য করতে পারি।"
                    6 -> "হ্যালো ব্রো! আমি স্মার্ট রকি। বলো আজ কোন প্রজেক্ট বা আইডিয়া নিয়ে কাজ শুরু করবো?"
                    7 -> "শুভ দিন! আমি রাহাত বলছি। স্পষ্ট ও গম্ভীর কণ্ঠে তোমার যেকোনো বিষয়ে বিশ্লেষণ করে দিচ্ছি।"
                    8 -> "হ্যালো বন্ধু! আমি আবীর। একদম বন্ধুর মতো তোমার সব কথা শুনতে ও সাহায্য করতে আমি আছি।"
                    9 -> "হেই! আমি অনিক! ফুল এনার্জি নিয়ে তোমার ক্রিয়েটিভ স্টুডিওর কাজ শুরু করা যাক!"
                    else -> "হ্যালো, আমি শুভ। ঠান্ডা মাথায় খুব সহজভাবে তোমার সব সমস্যার সমাধান করে দেবো।"
                }
                Toast.makeText(context, "🔊 ${chosen.shortName} (${chosen.geminiVoiceName}) বাজানো হচ্ছে...", Toast.LENGTH_SHORT).show()
                onSpeakText(sampleText)
            },
            onOpenSmartKeyboard = {
                showSettings = false
                selectedBottomTab = 0
                keyboardController?.hide()
                focusManager.clearFocus()
                showBanglaKeyboard = true
            },
            onSwitchSession = { targetId ->
                viewModel.switchSessionById(targetId)
                Toast.makeText(context, "👤 মেমোরি সেশন পরিবর্তন হয়েছে: $targetId", Toast.LENGTH_SHORT).show()
            },
            onClearChatMemory = {
                viewModel.clearCurrentSessionMemory()
                Toast.makeText(context, "🧹 সেশন মেমোরি (ChatMessageHistory) ক্লিয়ার করা হয়েছে", Toast.LENGTH_SHORT).show()
            },
            onVerifyApiConnection = { keyToTest ->
                viewModel.verifyApiConnection(keyToTest)
            },
            onDismiss = {
                activeVoiceBadge = viewModel.settings.getSelectedVoice().shortName
                showSettings = false
            }
        )
    }

    inspectedBitmap?.let { bmp ->
        FullscreenBitmapDialog(
            bitmap = bmp,
            onDismiss = { inspectedBitmap = null }
        )
    }

    var wasLoadingReply by remember { mutableStateOf(false) }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
        val lastMessage = messages.lastOrNull()
        if (lastMessage?.isLoading == true || lastMessage?.isStreaming == true) {
            wasLoadingReply = true
        } else if (wasLoadingReply && lastMessage != null && !lastMessage.isUser) {
            wasLoadingReply = false
            if (lastMessage.mode == StudioMode.MUSIC_GEN && lastMessage.songMetadata != null) {
                playingSongMessageId = lastMessage.id
                melodySynthesizer.startMelody(coroutineScope, lastMessage.songMetadata.genre) {
                    playingSongMessageId = null
                }
                onSpeakText(lastMessage.songMetadata.vocalLyrics)
            } else if (viewModel.settings.autoSpeak) {
                onSpeakText(lastMessage.text)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF161625),
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "👑 $aiDisplayName স্টুডিও",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = {
                                viewModel.startNewChat()
                                coroutineScope.launch { drawerState.close() }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("➕ নতুন চ্যাট", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Text(
                        text = "কুইক স্টুডিও মোড",
                        color = Color(0xFFF472B6),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    StudioMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (activeMode == mode) Color(0xFFEC4899).copy(alpha = 0.22f)
                                    else Color.White.copy(alpha = 0.04f)
                                )
                                .border(
                                    1.dp,
                                    if (activeMode == mode) Color(0xFFEC4899) else Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.setStudioMode(mode)
                                    selectedBottomTab = 0
                                    coroutineScope.launch { drawerState.close() }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(mode.icon, fontSize = 16.sp)
                            Text(mode.labelBn, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Text(
                        text = "সংরক্ষিত কথোপকথন (Saved Room History)",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (sessions.isEmpty()) {
                        Text(
                            text = "তোমার সব চ্যাট অটোমেটিক লোকাল ডাটাবেসে সেভ থাকবে। অ্যাপ বন্ধ করলেও হারিয়ে যাবে না।",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.sp
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(sessions, key = { it.id }) { session ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .clickable {
                                            viewModel.loadSession(session)
                                            selectedBottomTab = 0
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("💬", fontSize = 15.sp, modifier = Modifier.padding(end = 10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(session.title, color = Color.White, fontSize = 13.sp, maxLines = 1)
                                        Text(
                                            text = "${session.messages.size}টি মেসেজ • ${session.updatedAt}",
                                            color = Color.White.copy(alpha = 0.45f),
                                            fontSize = 10.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteSession(session.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("🗑️", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (sessions.isEmpty()) {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showSettings = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF312E81)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("⚙️ সব সেটিংস বক্স খুলুন", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0B0B13))
        ) {
            // Ambient glowing background orbs
            Box(
                modifier = Modifier
                    .offset(x = (-40).dp, y = (-40).dp)
                    .size(220.dp)
                    .background(Color(0xFF831843).copy(alpha = 0.22f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 40.dp, y = 40.dp)
                    .size(220.dp)
                    .background(Color(0xFF312E81).copy(alpha = 0.22f), CircleShape)
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // ==========================================
                // TOP HEADER BAR (Original Clean Corner-to-Corner Layout)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                .testTag("drawer_menu_button")
                        ) {
                            Text("☰", fontSize = 18.sp, color = Color.White)
                        }

                        // Avatar Badge with Crown & Beautiful "SM" in the Center
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
                                        )
                                    )
                                    .border(2.dp, Color(0xFFFDE047).copy(alpha = 0.75f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "👑",
                                        fontSize = 9.sp,
                                        lineHeight = 9.sp
                                    )
                                    Text(
                                        text = viewModel.settings.getAIInitials(aiDisplayName),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                                    .border(2.dp, Color(0xFF0B0B13), CircleShape)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = aiDisplayName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFEC4899).copy(alpha = 0.25f))
                                        .border(1.dp, Color(0xFFEC4899).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO STUDIO",
                                        color = Color(0xFFF472B6),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Text(
                                text = "${activeMode.icon} ${activeMode.labelBn} • ভয়েস: $activeVoiceBadge",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4ADE80)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.startNewChat()
                                Toast.makeText(context, "নতুন চ্যাট শুরু হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                                .testTag("new_chat_button")
                        ) {
                            Text("🕒", fontSize = 17.sp)
                        }
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFEC4899).copy(alpha = 0.28f), Color(0xFF8B5CF6).copy(alpha = 0.28f))
                                    )
                                )
                                .border(1.dp, Color(0xFFF472B6).copy(alpha = 0.4f), CircleShape)
                                .testTag("open_settings_button")
                        ) {
                            Text("⚙️", fontSize = 18.sp)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.07f))
                )

                // Single Master Voice Assistant Control Bar (Purple & Blue Accents)
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    VoiceAssistantStatusBar(
                        isEnabled = viewModel.settings.voiceAssistantMasterEnabled,
                        aiDisplayName = aiDisplayName,
                        onToggle = onToggleVoiceAssistant,
                        onTapOrb = onMicClick
                    )
                }

                // ==========================================
                // MAIN CONTENT AREA (SWITCH BY BOTTOM TAB)
                // ==========================================
                when (selectedBottomTab) {
                    1 -> {
                        MemoriesTabView(
                            modifier = Modifier.weight(1f),
                            memories = memories,
                            onAddMemory = { title, content ->
                                viewModel.saveToMemories(title, content)
                                Toast.makeText(context, "❤️ স্মৃতি সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteMemory = { id -> viewModel.deleteMemory(id) }
                        )
                    }
                    2 -> {
                        GalleryTabView(
                            modifier = Modifier.weight(1f),
                            items = galleryItems,
                            onPickFromDeviceGallery = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onInspectBitmap = { bmp -> inspectedBitmap = bmp }
                        )
                    }
                    else -> {
                        // Quick Studio Mode Selector Strip at top of Chat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.02f))
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudioMode.entries.forEach { mode ->
                                val selected = activeMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (selected) {
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
                                                )
                                            } else {
                                                Brush.horizontalGradient(
                                                    listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.06f))
                                                )
                                            }
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { viewModel.setStudioMode(mode) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${mode.icon} ${mode.labelBn}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Chat Messages List (Full Screen Area)
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp),
                            reverseLayout = false
                        ) {
                            item { Spacer(modifier = Modifier.height(10.dp)) }
                            if (messages.isEmpty()) {
                                item {
                                    SuraiyaVoiceAssistantHero(
                                        isEnabled = viewModel.settings.voiceAssistantMasterEnabled,
                                        aiDisplayName = aiDisplayName,
                                        onOrbClick = onMicClick,
                                        onSuggestionClick = onAssistantSuggestion
                                    )
                                }
                            }
                            items(messages, key = { it.id }) { message ->
                                PremierMessageBubble(
                                    message = message,
                                    isPlayingSongId = playingSongMessageId,
                                    aiDisplayName = aiDisplayName,
                                    onSpeak = { text -> onSpeakText(text) },
                                    onToggleSongPlay = { msg ->
                                        if (playingSongMessageId == msg.id) {
                                            playingSongMessageId = null
                                            onStopSpeech()
                                        } else {
                                            playingSongMessageId = msg.id
                                            val genre = msg.songMetadata?.genre ?: viewModel.settings.musicGenre
                                            melodySynthesizer.startMelody(coroutineScope, genre) {
                                                playingSongMessageId = null
                                            }
                                            onSpeakText(msg.songMetadata?.vocalLyrics ?: msg.text)
                                        }
                                    },
                                    onSaveMemory = { msg ->
                                        viewModel.saveToMemories(
                                            title = msg.mode.labelBn,
                                            content = msg.text.take(160)
                                        )
                                        Toast.makeText(context, "❤️ স্মৃতিতে সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
                                    },
                                    onInspectBitmap = { bmp -> inspectedBitmap = bmp }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(12.dp)) }
                        }
                    }
                }

                // ==========================================
                // GEMINI-STYLE PLUS (+) EXPANDABLE MENU & IMAGE PREVIEW
                // ==========================================
                pendingImage?.let { bmp ->
                    AttachedImagePreviewTray(
                        bitmap = bmp,
                        activeMode = activeMode,
                        onRemove = { viewModel.clearPendingImage() },
                        onQuickSendPreset = { presetPrompt ->
                            viewModel.sendMessage(inputText, quickEnhancementPreset = presetPrompt)
                            inputText = ""
                        }
                    )
                }

                GeminiStylePlusMenu(
                    visible = showPlusMenu,
                    aiDisplayName = aiDisplayName,
                    onPickGallery = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onOpenCamera = {
                        try {
                            cameraLauncher.launch(null)
                        } catch (e: Exception) {
                            Toast.makeText(context, "ক্যামেরা খোলা যায়নি", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSelectMode = { mode ->
                        viewModel.setStudioMode(mode)
                        selectedBottomTab = 0
                        showPlusMenu = false
                        Toast.makeText(context, "${mode.icon} ${mode.labelBn} চালু হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    onOpenSettings = {
                        showPlusMenu = false
                        showSettings = true
                    }
                )

                // ==========================================
                // BUILT-IN PROFESSIONAL SMART BANGLA & ENGLISH KEYBOARD
                // ==========================================
                LaunchedEffect(showBanglaKeyboard) {
                    if (showBanglaKeyboard && messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                }

                SmartBanglaKeyboard(
                    visible = showBanglaKeyboard,
                    currentText = inputText,
                    settings = viewModel.settings,
                    isListening = isListening,
                    onTextChange = { updated -> inputText = updated },
                    onSend = {
                        if (inputText.isNotBlank() || pendingImage != null) {
                            selectedBottomTab = 0
                            showPlusMenu = false
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    onVoiceTypingClick = { onMicClick() },
                    onClose = { showBanglaKeyboard = false }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )

                // Live Speech Recognition Banner (shows when listening or when transcript arrives)
                if (isListening || liveTranscript.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isListening) Color(0xFF831843).copy(alpha = 0.55f)
                                else Color(0xFF065F46).copy(alpha = 0.45f)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isListening) "🔴" else "✅",
                                fontSize = 13.sp
                            )
                            Text(
                                text = liveTranscript.ifBlank { "🎙️ শুনছি... বাংলায় বলুন" },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                        if (isListening) {
                            Text(
                                text = "থামুন ✕",
                                color = Color(0xFFF472B6),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onMicClick() }
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // ==========================================
                // BOTTOM CHAT INPUT BAR & NAVIGATION TABS
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF12121E))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .border(
                                    1.dp,
                                    if (isListening || showPlusMenu || pendingImage != null || showBanglaKeyboard) Color(0xFFEC4899)
                                    else Color.White.copy(alpha = 0.14f),
                                    RoundedCornerShape(28.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // LEFT PLUS (+) BUTTON FOR GALLERY, CAMERA, IMAGE, VIDEO, SONG
                            IconButton(
                                onClick = { showPlusMenu = !showPlusMenu },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (showPlusMenu) {
                                            Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)))
                                        } else {
                                            Brush.linearGradient(
                                                listOf(Color(0xFFEC4899).copy(alpha = 0.25f), Color(0xFF8B5CF6).copy(alpha = 0.25f))
                                            )
                                        }
                                    )
                                    .border(1.dp, Color(0xFFF472B6).copy(alpha = 0.5f), CircleShape)
                                    .testTag("chat_plus_button")
                            ) {
                                Text(
                                    text = if (showPlusMenu) "✕" else "➕",
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            BasicTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_field"),
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 15.sp),
                                cursorBrush = SolidColor(Color(0xFFEC4899)),
                                decorationBox = { innerTextField ->
                                    if (inputText.isEmpty()) {
                                        Text(
                                            text = if (isListening) "🎙️ শুনছি... এখন বাংলায় কথা বলুন" else activeMode.hintBn,
                                            color = if (isListening) Color(0xFFF472B6) else Color.White.copy(alpha = 0.45f),
                                            fontSize = 14.sp,
                                            maxLines = 1
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            // Built-in Bangla Keyboard Icon Button
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    selectedBottomTab = 0
                                    showBanglaKeyboard = !showBanglaKeyboard
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (showBanglaKeyboard) Color(0xFF10B981).copy(alpha = 0.3f)
                                        else Color.Transparent
                                    )
                                    .testTag("toggle_bangla_keyboard_button")
                            ) {
                                Text("⌨️", fontSize = 17.sp)
                            }

                            // Direct Camera / Live Studio Quick Button + Mic Button
                            IconButton(
                                onClick = { showLiveStudioScreen = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("open_live_studio_button")
                            ) {
                                Text("📷", fontSize = 18.sp)
                            }

                            val isAssistantActive = assistantState !is AssistantState.Idle
                            IconButton(
                                onClick = onMicClick,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isAssistantActive) Color(0xFF8B5CF6).copy(alpha = 0.45f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        if (isAssistantActive) 1.dp else 0.dp,
                                        if (isAssistantActive) Color(0xFFC084FC) else Color.Transparent,
                                        CircleShape
                                    )
                                    .testTag("voice_mic_button")
                            ) {
                                Text(if (isAssistantActive) "✨" else "🎙️", fontSize = 18.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Send Button
                        IconButton(
                            onClick = {
                                selectedBottomTab = 0
                                showPlusMenu = false
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))
                                    )
                                )
                                .testTag("send_message_button")
                        ) {
                            Text("✈️", fontSize = 22.sp, modifier = Modifier.padding(start = 2.dp))
                        }
                    }

                    // Bottom Interactive Tab Switcher: চ্যাট | স্মৃতি | গ্যালারি
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomNavTabItem(
                            title = "💬 চ্যাট",
                            selected = selectedBottomTab == 0,
                            onClick = { selectedBottomTab = 0 }
                        )
                        BottomNavTabItem(
                            title = "💖 স্মৃতি (${memories.size})",
                            selected = selectedBottomTab == 1,
                            onClick = {
                                showBanglaKeyboard = false
                                selectedBottomTab = 1
                            }
                        )
                        BottomNavTabItem(
                            title = "🖼️ গ্যালারি (${galleryItems.size})",
                            selected = selectedBottomTab == 2,
                            onClick = {
                                showBanglaKeyboard = false
                                selectedBottomTab = 2
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavTabItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(if (selected) 24.dp else 6.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(if (selected) Color(0xFFEC4899) else Color.White.copy(alpha = 0.3f))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

