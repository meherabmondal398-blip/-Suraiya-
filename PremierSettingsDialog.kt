package com.example

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@Composable
fun PremierSettingsDialog(
    settings: SettingsManager,
    onTestVoice: (Int) -> Unit = {},
    onOpenSmartKeyboard: () -> Unit = {},
    onSwitchSession: (String) -> Unit = {},
    onClearChatMemory: () -> Unit = {},
    onVerifyApiConnection: suspend (String) -> Pair<Boolean, String> = { Pair(false, "") },
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 0. Voice Assistant Master Control (Single Master Switch)
    var voiceAssistantMasterEnabled by remember { mutableStateOf(settings.voiceAssistantMasterEnabled) }

    // Dynamic AI Assistant Name states
    var aiNameInput by remember { mutableStateOf(settings.aiDisplayName) }
    var currentAiNamePreview by remember { mutableStateOf(settings.aiDisplayName) }
    var aiNameValidationMsg by remember { mutableStateOf<String?>(null) }
    var aiNameSuccessMsg by remember { mutableStateOf<String?>(null) }

    // 1. Chat & Core AI Box states
    var geminiKey by remember { mutableStateOf(settings.geminiKey.ifBlank { settings.resolveChatKey() }) }
    var chatModel by remember { mutableStateOf(settings.chatModel) }
    var aiPersona by remember { mutableStateOf(settings.aiPersona) }
    var temperature by remember { mutableFloatStateOf(settings.temperature) }
    var conversationalMemoryEnabled by remember { mutableStateOf(settings.conversationalMemoryEnabled) }
    var activeSessionId by remember { mutableStateOf(settings.activeSessionId) }

    var isTestingBox1 by remember { mutableStateOf(false) }
    var box1TestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // 2. Image Studio & Pro Remake Box states
    var imageApiKey by remember { mutableStateOf(settings.imageApiKey) }
    var imageModel by remember { mutableStateOf(settings.imageModel) }
    var imageAspectRatio by remember { mutableStateOf(settings.imageAspectRatio) }
    var imageEnhancementStyle by remember { mutableStateOf(settings.imageEnhancementStyle) }
    var autoEnhanceGallery by remember { mutableStateOf(settings.autoEnhanceGallery) }

    var isTestingBox2 by remember { mutableStateOf(false) }
    var box2TestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // 3. Video Studio (Veo) Box states
    var videoApiKey by remember { mutableStateOf(settings.videoApiKey) }
    var videoModel by remember { mutableStateOf(settings.videoModel) }
    var videoResolution by remember { mutableStateOf(settings.videoResolution) }
    var videoAspectRatio by remember { mutableStateOf(settings.videoAspectRatio) }

    var isTestingBox3 by remember { mutableStateOf(false) }
    var box3TestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // 4. Music, Song & Voice Box states
    var voiceKey by remember { mutableStateOf(settings.voiceKey) }
    var musicModel by remember { mutableStateOf(settings.musicModel) }
    var musicGenre by remember { mutableStateOf(settings.musicGenre) }
    var language by remember { mutableStateOf(settings.language) }
    var selectedVoiceIndex by remember { mutableIntStateOf(settings.selectedVoiceIndex) }
    var voiceDropdownExpanded by remember { mutableStateOf(false) }
    var autoSpeak by remember { mutableStateOf(settings.autoSpeak) }

    var isTestingBox4 by remember { mutableStateOf(false) }
    var box4TestResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Universal 4-Box Test State
    var isTestingAll4 by remember { mutableStateOf(false) }
    var all4SummaryBanner by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // 5. Smart AI Profile & Professional Smart Keyboard Box states
    var userName by remember { mutableStateOf(settings.userName) }
    var smartKeyboardEnabled by remember { mutableStateOf(settings.smartKeyboardEnabled) }
    var smartKeyboardDefaultLang by remember { mutableStateOf(settings.smartKeyboardDefaultLang) }
    var banglaAutoCorrect by remember { mutableStateOf(settings.banglaAutoCorrect) }
    var wordPredictionEnabled by remember { mutableStateOf(settings.wordPredictionEnabled) }
    var keyboardHapticEnabled by remember { mutableStateOf(settings.keyboardHapticEnabled) }

    var showKeysPlain by remember { mutableStateOf(false) }

    fun refreshLocalKeyStatesFromSettings() {
        if (geminiKey.isBlank() && settings.geminiKey.isNotBlank()) geminiKey = settings.geminiKey
        if (imageApiKey.isBlank() && settings.imageApiKey.isNotBlank()) imageApiKey = settings.imageApiKey
        if (videoApiKey.isBlank() && settings.videoApiKey.isNotBlank()) videoApiKey = settings.videoApiKey
        if (voiceKey.isBlank() && settings.voiceKey.isNotBlank()) voiceKey = settings.voiceKey
    }

    fun readClipboardKey(): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipText = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        if (clipText.isBlank()) return ""
        return settings.extractApiKeyFromText(clipText) ?: settings.cleanRawKeyInput(clipText)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 20.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF10101A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF831843).copy(alpha = 0.45f), Color(0xFF312E81).copy(alpha = 0.45f))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "⚙️ প্রিমিয়ার স্টুডিও সেটিংস",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "চ্যাট, ইমেজ, ভিডিও ও গানের ৪টি আলাদা API কনফিগারেশন বক্স",
                            color = Color(0xFFF472B6),
                            fontSize = 11.sp
                        )
                    }
                    TextButton(
                        onClick = { showKeysPlain = !showKeysPlain },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Text(
                            text = if (showKeysPlain) "🙈 Key লুকান" else "👁️ Key দেখুন",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }

                // Scrollable Settings Boxes
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ==========================================
                    // SINGLE MASTER SWITCH: VOICE ASSISTANT
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        if (voiceAssistantMasterEnabled) Color(0xFF4C1D95).copy(alpha = 0.55f) else Color(0xFF1E293B).copy(alpha = 0.55f),
                                        if (voiceAssistantMasterEnabled) Color(0xFF1E1B4B).copy(alpha = 0.85f) else Color(0xFF0F172A).copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .border(
                                1.5.dp,
                                if (voiceAssistantMasterEnabled) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (voiceAssistantMasterEnabled)
                                                Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6)))
                                            else
                                                Brush.linearGradient(listOf(Color(0xFF64748B), Color(0xFF334155)))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (voiceAssistantMasterEnabled) "🎙️" else "🔇",
                                        fontSize = 22.sp
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Voice Assistant",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (voiceAssistantMasterEnabled)
                                            "🟢 ON — “হেলো সুরাইয়া” ডাকলে মাইক অন হবে ও যা বলবেন সার্চ করবে"
                                        else
                                            "🔴 OFF — অ্যাসিস্ট্যান্ট সম্পূর্ণ নিষ্ক্রিয় থাকবে",
                                        color = if (voiceAssistantMasterEnabled) Color(0xFFC084FC) else Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Switch(
                                checked = voiceAssistantMasterEnabled,
                                onCheckedChange = {
                                    voiceAssistantMasterEnabled = it
                                    settings.voiceAssistantMasterEnabled = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF8B5CF6),
                                    uncheckedThumbColor = Color(0xFF94A3B8),
                                    uncheckedTrackColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.testTag("voice_assistant_master_switch")
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "• ON → “হেলো $currentAiNamePreview” ডাকলেই মাইক অন হবে, ইমোজি ও অ্যানিমেশন সামনে আসবে এবং যা বলবেন তাই সার্চ করবে।",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "• OFF → অ্যাসিস্ট্যান্ট সম্পূর্ণ বন্ধ থাকবে এবং কোনো ডাকে সাড়া দেবে না।",
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // ==========================================
                    // 1. AI Assistant Name (Dynamic Branding System)
                    // ==========================================
                    SettingsBoxCard(
                        icon = "🤖",
                        title = "AI Assistant Name",
                        subtitle = "কাস্টম নাম সেট করুন • পুরো অ্যাপে স্বয়ংক্রিয়ভাবে দেখাবে",
                        accentColor = Color(0xFF8B5CF6)
                    ) {
                        // বর্তমান AI নাম দেখানোর একটি Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF2E1065).copy(alpha = 0.7f), Color(0xFF1E1B4B).copy(alpha = 0.7f))
                                    )
                                )
                                .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))
                                                )
                                            )
                                            .border(1.5.dp, Color(0xFFFDE047).copy(alpha = 0.7f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text("👑", fontSize = 9.sp, lineHeight = 9.sp)
                                            Text(
                                                text = settings.getAIInitials(currentAiNamePreview),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                lineHeight = 11.sp
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "বর্তমান AI নাম (Active Preview):",
                                            color = Color(0xFFC084FC),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = currentAiNamePreview,
                                            color = Color.White,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                        .border(1.dp, Color(0xFF34D399).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "✨ সক্রিয়",
                                        color = Color(0xFF6EE7B7),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Text Input Box (Placeholder: Enter Your AI Name)
                        OutlinedTextField(
                            value = aiNameInput,
                            onValueChange = {
                                aiNameInput = it
                                aiNameValidationMsg = null
                                aiNameSuccessMsg = null
                            },
                            label = { Text("AI Assistant Name (বাংলা বা English)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp) },
                            placeholder = { Text("Enter Your AI Name", color = Color.White.copy(alpha = 0.35f), fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF8B5CF6),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.25f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_name_input_field"),
                            supportingText = {
                                Text(
                                    text = "উদাহরণ: সুরাইয়া, জান্নাত AI, মেহেরাব AI, Nova AI, My Personal AI",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp
                                )
                            }
                        )

                        // Validation Message Banner
                        aiNameValidationMsg?.let { error ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF7F1D1D).copy(alpha = 0.45f))
                                    .border(1.dp, Color(0xFFF87171), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = error,
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Success Message Banner: "AI-এর নাম সফলভাবে পরিবর্তন করা হয়েছে!"
                        aiNameSuccessMsg?.let { success ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF065F46).copy(alpha = 0.45f))
                                    .border(1.dp, Color(0xFF34D399), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "✅ $success",
                                    color = Color(0xFF6EE7B7),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons: Save & Reset to Default
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val clean = aiNameInput.trim()
                                    if (clean.isBlank()) {
                                        aiNameValidationMsg = "⚠️ অনুগ্রহ করে একটি সঠিক AI নাম লিখুন (নাম খালি রাখা যাবে না)"
                                        aiNameSuccessMsg = null
                                    } else {
                                        settings.saveAIName(clean)
                                        aiNameInput = clean
                                        currentAiNamePreview = clean
                                        aiNameValidationMsg = null
                                        aiNameSuccessMsg = "AI-এর নাম সফলভাবে পরিবর্তন করা হয়েছে!"
                                        Toast.makeText(context, "AI-এর নাম সফলভাবে পরিবর্তন করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_name_save_button")
                            ) {
                                Text(
                                    text = "💾 Save",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    settings.resetAIName()
                                    aiNameInput = SettingsManager.DEFAULT_AI_NAME
                                    currentAiNamePreview = SettingsManager.DEFAULT_AI_NAME
                                    aiNameValidationMsg = null
                                    aiNameSuccessMsg = "ডিফল্ট নাম 'Suraiya AI' সফলভাবে সেট করা হয়েছে!"
                                    Toast.makeText(context, "ডিফল্ট নাম 'Suraiya AI' সফলভাবে সেট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("ai_name_reset_button")
                            ) {
                                Text(
                                    text = "↺ Reset to Default",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // ==========================================
                    // 4-BOX UNIVERSAL API HUB & 1-TAP SYNC CARD
                    // ==========================================
                    val anyActiveKey = listOf(geminiKey, imageApiKey, videoApiKey, voiceKey, settings.resolveChatKey())
                        .map { settings.cleanRawKeyInput(it) }
                        .firstOrNull { it.length >= 20 }
                        .orEmpty()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E1B4B), Color(0xFF31102F))
                                )
                            )
                            .border(1.dp, Color(0xFFF472B6).copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🔗 ৪টি এপিআই বক্সের মাস্টার কানেকশন হাব",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "১টি বা ৪টি আলাদা API Key দিন — যেকোনো ১টি সচল থাকলেই সবগুলো বক্স কাজ করবে",
                                    color = Color(0xFFFBCFE8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Status pills for all 4 boxes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val boxesStatus = listOf(
                                Triple("১. চ্যাট", geminiKey, anyActiveKey),
                                Triple("২. ইমেজ", imageApiKey, anyActiveKey),
                                Triple("৩. ভিডিও", videoApiKey, anyActiveKey),
                                Triple("৪. গান/ভয়েস", voiceKey, anyActiveKey)
                            )
                            boxesStatus.forEach { (name, ownKey, fallbackKey) ->
                                val cleanOwn = settings.cleanRawKeyInput(ownKey)
                                val isReady = cleanOwn.length >= 20 || fallbackKey.isNotEmpty()
                                val badgeText = when {
                                    cleanOwn.length >= 20 -> "🟢 $name (${settings.maskKey(cleanOwn)})"
                                    fallbackKey.isNotEmpty() -> "🟢 $name (অটো-সিঙ্ক)"
                                    else -> "🔴 $name (খালি)"
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isReady) Color(0xFF065F46).copy(alpha = 0.55f)
                                            else Color(0xFF7F1D1D).copy(alpha = 0.5f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isReady) Color(0xFF34D399) else Color(0xFFF87171),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Sync 1 key or Clipboard across all 4 boxes
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
                                    .clickable {
                                        val keyToSync = anyActiveKey.ifEmpty { readClipboardKey() }
                                        if (keyToSync.length >= 20) {
                                            settings.syncKeyToAllBoxes(keyToSync)
                                            geminiKey = keyToSync
                                            imageApiKey = keyToSync
                                            videoApiKey = keyToSync
                                            voiceKey = keyToSync
                                            val masked = settings.maskKey(keyToSync)
                                            all4SummaryBanner = Pair(
                                                true,
                                                "🔄 API Key ($masked) চ্যাট, ইমেজ, ভিডিও ও গানের ৪টি বক্সেই একসাথে বসানো ও সেভ করা হয়েছে!"
                                            )
                                        } else {
                                            all4SummaryBanner = Pair(
                                                false,
                                                "প্রথমে যেকোনো ১টি বক্সে বা ক্লিপবোর্ডে সঠিক API Key (AIzaSy...) কপি করুন।"
                                            )
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp)
                                    .testTag("sync_all_4_keys_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🔄 ১টি Key ৪টি বক্সেই বসান",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Test all 4 boxes simultaneously
                            Box(
                                modifier = Modifier
                                    .weight(1.15f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF10B981), Color(0xFF06B6D4))
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .clickable(enabled = !isTestingAll4) {
                                        settings.geminiKey = geminiKey
                                        settings.imageApiKey = imageApiKey
                                        settings.videoApiKey = videoApiKey
                                        settings.voiceKey = voiceKey
                                        isTestingAll4 = true
                                        all4SummaryBanner = null
                                        scope.launch {
                                            val preferred = geminiKey.ifBlank { imageApiKey.ifBlank { videoApiKey.ifBlank { voiceKey } } }
                                            val res = onVerifyApiConnection(preferred)
                                            refreshLocalKeyStatesFromSettings()
                                            box1TestResult = res
                                            box2TestResult = res
                                            box3TestResult = res
                                            box4TestResult = res
                                            all4SummaryBanner = if (res.first) {
                                                Pair(
                                                    true,
                                                    "✅ ৪টি বক্সেরই (১. চ্যাট, ২. ইমেজ, ৩. ভিডিও, ৪. গান ও ভয়েস) এপিআই সংযোগ সচল ও প্রস্তুত! ${res.second}"
                                                )
                                            } else {
                                                res
                                            }
                                            isTestingAll4 = false
                                        }
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp)
                                    .testTag("verify_all_4_boxes_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isTestingAll4) "⏳ ৪টি বক্স যাচাই হচ্ছে..." else "⚡ ৪টি বক্স একসাথে চেক করুন",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        all4SummaryBanner?.let { (ok, msg) ->
                            ApiTestResultBanner(ok = ok, message = msg)
                        }
                    }

                    // ==========================================
                    // BOX 1: CHAT & CORE AI SETTINGS BOX
                    // ==========================================
                    SettingsBoxCard(
                        icon = "💬",
                        title = "১. চ্যাট ও মূল এআই সেটিংস বক্স",
                        subtitle = "কথোপকথন, মডেল ও ব্যক্তিত্ব নিয়ন্ত্রণ",
                        accentColor = Color(0xFFEC4899)
                    ) {
                        ApiKeyField(
                            value = geminiKey,
                            onValueChange = {
                                val extracted = settings.extractApiKeyFromText(it) ?: settings.cleanRawKeyInput(it)
                                geminiKey = extracted
                                settings.geminiKey = extracted
                                box1TestResult = null
                            },
                            label = "১ নম্বর বক্স: Gemini Chat API Key (মূল চাবি)",
                            placeholder = "AIzaSy... (এখানে দিলে সব বক্সে কাজ করবে)",
                            showPlain = showKeysPlain,
                            accentColor = Color(0xFFEC4899),
                            testTag = "settings_gemini_key_input"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        BoxApiKeyControlBar(
                            ownKey = geminiKey,
                            fallbackKey = anyActiveKey,
                            accentColor = Color(0xFFEC4899),
                            isTesting = isTestingBox1,
                            testResult = box1TestResult,
                            pasteButtonTag = "paste_api_key_button",
                            testButtonTag = "test_api_connection_button",
                            onClear = {
                                geminiKey = ""
                                settings.geminiKey = ""
                                box1TestResult = null
                            },
                            onPaste = {
                                val clean = readClipboardKey()
                                if (clean.isNotBlank()) {
                                    geminiKey = clean
                                    settings.geminiKey = clean
                                    settings.autoFillEmptyBoxesFromAvailableKey()
                                    refreshLocalKeyStatesFromSettings()
                                    box1TestResult = Pair(true, "📋 ক্লিপবোর্ড থেকে API Key বসানো হয়েছে! এখন 'সংযোগ পরীক্ষা' করুন।")
                                } else {
                                    box1TestResult = Pair(false, "ক্লিপবোর্ডে কোনো টেক্সট বা API Key পাওয়া যায়নি।")
                                }
                            },
                            onTest = {
                                settings.geminiKey = geminiKey
                                settings.chatModel = chatModel
                                isTestingBox1 = true
                                box1TestResult = null
                                scope.launch {
                                    box1TestResult = onVerifyApiConnection(geminiKey)
                                    refreshLocalKeyStatesFromSettings()
                                    isTestingBox1 = false
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        SectionLabel("চ্যাট এআই মডেল (Chat Model)")
                        ChipSelectorRow(
                            options = listOf(
                                "gemini-3.5-flash" to "🚀 Gemini 3.5 Flash (সুপার ফাস্ট ও আধুনিক)",
                                "gemini-flash-latest" to "⚡ Flash Latest (অটো-আপডেট)",
                                "gemini-3.1-pro-preview" to "💎 Gemini 3.1 Pro (প্রফেশনাল)"
                            ),
                            selected = chatModel,
                            accentColor = Color(0xFFEC4899),
                            onSelect = {
                                chatModel = it
                                settings.chatModel = it
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        SectionLabel("সুরাইয়ার ব্যক্তিত্ব (AI Persona)")
                        ChipSelectorRow(
                            options = listOf(
                                "loving" to "❤️ মায়াবী সঙ্গী",
                                "professional" to "💼 প্রফেশনাল এক্সপার্ট",
                                "creative" to "🎨 ক্রিয়েটিভ ডিরেক্টর",
                                "mentor" to "🎓 স্মার্ট মেন্টর"
                            ),
                            selected = aiPersona,
                            accentColor = Color(0xFFEC4899),
                            onSelect = {
                                aiPersona = it
                                settings.aiPersona = it
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        SectionLabel("তাপমাত্রা ও সৃজনশীলতা (Temperature: $temperature)")
                        ChipSelectorRow(
                            options = listOf(
                                "0.7" to "⚖️ 0.7 (Smart Balanced)",
                                "0.4" to "🎯 0.4 (Precise)",
                                "0.9" to "✨ 0.9 (Creative)"
                            ),
                            selected = String.format(java.util.Locale.US, "%.1f", temperature),
                            accentColor = Color(0xFFEC4899),
                            onSelect = { selectedTemp ->
                                val parsed = selectedTemp.toFloatOrNull() ?: 0.7f
                                temperature = parsed
                                settings.temperature = parsed
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        SectionLabel("মেমোরি সেশন আইডি (Session ID — ChatMessageHistory)")
                        ChipSelectorRow(
                            options = listOf(
                                "user_1" to "👤 user_1 (ডিফল্ট)",
                                "user_2" to "👥 user_2 (আলাদা সেশন)",
                                "vip_studio" to "👑 vip_studio"
                            ),
                            selected = activeSessionId,
                            accentColor = Color(0xFFEC4899),
                            onSelect = { chosenSession ->
                                activeSessionId = chosenSession
                                settings.activeSessionId = chosenSession
                                onSwitchSession(chosenSession)
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "কনভারসেশনাল মেমোরি চেইন (RunnableWithMessageHistory)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "আগের সব কথা মনে রেখে ধারাবাহিকভাবে বাংলায় উত্তর দেবে (সেশন: $activeSessionId)",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = conversationalMemoryEnabled,
                                onCheckedChange = {
                                    conversationalMemoryEnabled = it
                                    settings.conversationalMemoryEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFEC4899))
                            )
                        }
                    }

                    // ==========================================
                    // BOX 2: GALLERY UPLOAD PRO REMAKE & IMAGE BOX
                    // ==========================================
                    SettingsBoxCard(
                        icon = "🖼️",
                        title = "২. প্রো ইমেজ ও গ্যালারি এডিট বক্স",
                        subtitle = "গ্যালারির ছবি প্রফেশনাল রিমেক ও এআই ছবি তৈরি",
                        accentColor = Color(0xFF8B5CF6)
                    ) {
                        ApiKeyField(
                            value = imageApiKey,
                            onValueChange = {
                                val extracted = settings.extractApiKeyFromText(it) ?: settings.cleanRawKeyInput(it)
                                imageApiKey = extracted
                                settings.imageApiKey = extracted
                                box2TestResult = null
                            },
                            label = "২ নম্বর বক্স: Image Studio API Key (ইমেজ কি)",
                            placeholder = "AIzaSy... (খালি রাখলে ১ নম্বর বক্সের চাবি কাজ করবে)",
                            showPlain = showKeysPlain,
                            accentColor = Color(0xFF8B5CF6),
                            testTag = "settings_image_key_input"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        BoxApiKeyControlBar(
                            ownKey = imageApiKey,
                            fallbackKey = anyActiveKey,
                            accentColor = Color(0xFF8B5CF6),
                            isTesting = isTestingBox2,
                            testResult = box2TestResult,
                            pasteButtonTag = "paste_image_key_button",
                            testButtonTag = "test_image_key_button",
                            onClear = {
                                imageApiKey = ""
                                settings.imageApiKey = ""
                                box2TestResult = null
                            },
                            onPaste = {
                                val clean = readClipboardKey()
                                if (clean.isNotBlank()) {
                                    imageApiKey = clean
                                    settings.imageApiKey = clean
                                    settings.autoFillEmptyBoxesFromAvailableKey()
                                    refreshLocalKeyStatesFromSettings()
                                    box2TestResult = Pair(true, "📋 ২ নম্বর (ইমেজ) বক্সে API Key বসানো হয়েছে! এখন 'সংযোগ পরীক্ষা' করুন।")
                                } else {
                                    box2TestResult = Pair(false, "ক্লিপবোর্ডে কোনো API Key পাওয়া যায়নি।")
                                }
                            },
                            onTest = {
                                settings.imageApiKey = imageApiKey
                                isTestingBox2 = true
                                box2TestResult = null
                                scope.launch {
                                    box2TestResult = onVerifyApiConnection(imageApiKey.ifBlank { anyActiveKey })
                                    refreshLocalKeyStatesFromSettings()
                                    isTestingBox2 = false
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        SectionLabel("ইমেজ জেনারেশন মডেল (Image Model)")
                        ChipSelectorRow(
                            options = listOf(
                                "gemini-2.5-flash-image" to "🍌 Nano Banana (Fast)",
                                "gemini-3.1-flash-image-preview" to "✨ Nano Banana 2 (HD)",
                                "gemini-3-pro-image-preview" to "👑 Pro Image Studio"
                            ),
                            selected = imageModel,
                            accentColor = Color(0xFF8B5CF6),
                            onSelect = {
                                imageModel = it
                                settings.imageModel = it
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        SectionLabel("গ্যালারি ছবি প্রফেশনাল স্টাইল (Pro Remake Preset)")
                        ChipSelectorRow(
                            options = listOf(
                                "studio_hd" to "📸 স্টুডিও এইচডি প্রো",
                                "cinematic" to "🎬 সিনেমাটিক HDR",
                                "luxury_portrait" to "✨ লাক্সারি পোর্ট্রেট",
                                "anime_art" to "🎨 ডিজিটাল আর্ট"
                            ),
                            selected = imageEnhancementStyle,
                            accentColor = Color(0xFF8B5CF6),
                            onSelect = {
                                imageEnhancementStyle = it
                                settings.imageEnhancementStyle = it
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        SectionLabel("ছবির সাইজ ও অনুপাত (Aspect Ratio)")
                        ChipSelectorRow(
                            options = listOf(
                                "1:1" to "1:1 বর্গাকার",
                                "16:9" to "16:9 ওয়াইড",
                                "9:16" to "9:16 পোর্ট্রেট",
                                "4:3" to "4:3 স্ট্যান্ডার্ড"
                            ),
                            selected = imageAspectRatio,
                            accentColor = Color(0xFF8B5CF6),
                            onSelect = {
                                imageAspectRatio = it
                                settings.imageAspectRatio = it
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "গ্যালারি ছবি অটো-প্রফেশনাল মোড",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "গ্যালারি থেকে ছবি নিলেই প্রফেশনাল রিমেক মোড চালু হবে",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = autoEnhanceGallery,
                                onCheckedChange = {
                                    autoEnhanceGallery = it
                                    settings.autoEnhanceGallery = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF8B5CF6))
                            )
                        }
                    }

                    // ==========================================
                    // BOX 3: VIDEO STUDIO (VEO) SETTINGS BOX
                    // ==========================================
                    SettingsBoxCard(
                        icon = "🎬",
                        title = "৩. এআই ভিডিও জেনারেশন বক্স (Veo)",
                        subtitle = "সিনেমাটিক ভিডিও ও ডিরেক্টর স্টোরিবোর্ড",
                        accentColor = Color(0xFF06B6D4)
                    ) {
                        ApiKeyField(
                            value = videoApiKey,
                            onValueChange = {
                                val extracted = settings.extractApiKeyFromText(it) ?: settings.cleanRawKeyInput(it)
                                videoApiKey = extracted
                                settings.videoApiKey = extracted
                                box3TestResult = null
                            },
                            label = "৩ নম্বর বক্স: Video (Veo) API Key (ভিডিও কি)",
                            placeholder = "AIzaSy... (খালি রাখলে ১ নম্বর বক্সের চাবি কাজ করবে)",
                            showPlain = showKeysPlain,
                            accentColor = Color(0xFF06B6D4),
                            testTag = "settings_video_key_input"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        BoxApiKeyControlBar(
                            ownKey = videoApiKey,
                            fallbackKey = anyActiveKey,
                            accentColor = Color(0xFF06B6D4),
                            isTesting = isTestingBox3,
                            testResult = box3TestResult,
                            pasteButtonTag = "paste_video_key_button",
                            testButtonTag = "test_video_key_button",
                            onClear = {
                                videoApiKey = ""
                                settings.videoApiKey = ""
                                box3TestResult = null
                            },
                            onPaste = {
                                val clean = readClipboardKey()
                                if (clean.isNotBlank()) {
                                    videoApiKey = clean
                                    settings.videoApiKey = clean
                                    settings.autoFillEmptyBoxesFromAvailableKey()
                                    refreshLocalKeyStatesFromSettings()
                                    box3TestResult = Pair(true, "📋 ৩ নম্বর (ভিডিও) বক্সে API Key বসানো হয়েছে! এখন 'সংযোগ পরীক্ষা' করুন।")
                                } else {
                                    box3TestResult = Pair(false, "ক্লিপবোর্ডে কোনো API Key পাওয়া যায়নি।")
                                }
                            },
                            onTest = {
                                settings.videoApiKey = videoApiKey
                                isTestingBox3 = true
                                box3TestResult = null
                                scope.launch {
                                    box3TestResult = onVerifyApiConnection(videoApiKey.ifBlank { anyActiveKey })
                                    refreshLocalKeyStatesFromSettings()
                                    isTestingBox3 = false
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        SectionLabel("ভিডিও মডেল (Veo Model)")
                        ChipSelectorRow(
                            options = listOf(
                                "veo-3.1-fast-generate-preview" to "⚡ Veo 3.1 Fast",
                                "veo-3.1-generate-preview" to "🎥 Veo 3.1 Cinema Pro"
                            ),
                            selected = videoModel,
                            accentColor = Color(0xFF06B6D4),
                            onSelect = {
                                videoModel = it
                                settings.videoModel = it
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        SectionLabel("ভিডিও রেজোলিউশন ও রেশিও")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ChipSelectorRow(
                                options = listOf("1080p" to "Full HD 1080p", "720p" to "HD 720p"),
                                selected = videoResolution,
                                accentColor = Color(0xFF06B6D4),
                                onSelect = {
                                    videoResolution = it
                                    settings.videoResolution = it
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        ChipSelectorRow(
                            options = listOf("16:9" to "16:9 সিনেমা", "9:16" to "9:16 রিলস/শর্টস"),
                            selected = videoAspectRatio,
                            accentColor = Color(0xFF06B6D4),
                            onSelect = {
                                videoAspectRatio = it
                                settings.videoAspectRatio = it
                            }
                        )
                    }

                    // ==========================================
                    // BOX 4: MUSIC, SONG & VOICE SETTINGS BOX
                    // ==========================================
                    SettingsBoxCard(
                        icon = "🎵",
                        title = "৪. গান, মিউজিক ও ভয়েস স্টুডিও বক্স",
                        subtitle = "এআই গান তৈরি, মিউজিক টিউন ও সুরাইয়ার কণ্ঠস্বর",
                        accentColor = Color(0xFF10B981)
                    ) {
                        ApiKeyField(
                            value = voiceKey,
                            onValueChange = {
                                val extracted = settings.extractApiKeyFromText(it) ?: settings.cleanRawKeyInput(it)
                                voiceKey = extracted
                                settings.voiceKey = extracted
                                box4TestResult = null
                            },
                            label = "৪ নম্বর বক্স: Music & Voice API Key (গান ও ভয়েস কি)",
                            placeholder = "AIzaSy... (খালি রাখলে ১ নম্বর বক্সের চাবি কাজ করবে)",
                            showPlain = showKeysPlain,
                            accentColor = Color(0xFF10B981),
                            testTag = "settings_voice_key_input"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        BoxApiKeyControlBar(
                            ownKey = voiceKey,
                            fallbackKey = anyActiveKey,
                            accentColor = Color(0xFF10B981),
                            isTesting = isTestingBox4,
                            testResult = box4TestResult,
                            pasteButtonTag = "paste_voice_key_button",
                            testButtonTag = "test_voice_key_button",
                            onClear = {
                                voiceKey = ""
                                settings.voiceKey = ""
                                box4TestResult = null
                            },
                            onPaste = {
                                val clean = readClipboardKey()
                                if (clean.isNotBlank()) {
                                    voiceKey = clean
                                    settings.voiceKey = clean
                                    settings.autoFillEmptyBoxesFromAvailableKey()
                                    refreshLocalKeyStatesFromSettings()
                                    box4TestResult = Pair(true, "📋 ৪ নম্বর (গান ও ভয়েস) বক্সে API Key বসানো হয়েছে! এখন 'সংযোগ পরীক্ষা' করুন।")
                                } else {
                                    box4TestResult = Pair(false, "ক্লিপবোর্ডে কোনো API Key পাওয়া যায়নি।")
                                }
                            },
                            onTest = {
                                settings.voiceKey = voiceKey
                                isTestingBox4 = true
                                box4TestResult = null
                                scope.launch {
                                    box4TestResult = onVerifyApiConnection(voiceKey.ifBlank { anyActiveKey })
                                    refreshLocalKeyStatesFromSettings()
                                    isTestingBox4 = false
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        SectionLabel("গানের ধরন ও মিউজিক স্টাইল (Music Genre)")
                        ChipSelectorRow(
                            options = listOf(
                                "রোমান্টিক মেলোডি" to "💖 রোমান্টিক মেলোডি",
                                "লো-ফাই চিল" to "🌙 লো-ফাই চিল",
                                "সিনেমাটিক অর্কেস্ট্রা" to "🎻 সিনেমাটিক",
                                "বাংলা আধুনিক গান" to "🪕 বাংলা আধুনিক"
                            ),
                            selected = musicGenre,
                            accentColor = Color(0xFF10B981),
                            onSelect = {
                                musicGenre = it
                                settings.musicGenre = it
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        SectionLabel("অডিও ও মিউজিক মডেল (Audio Model)")
                        ChipSelectorRow(
                            options = listOf(
                                "gemini-2.5-flash-native-audio-preview-12-2025" to "🎧 Native Audio 2.5",
                                "gemini-2.5-flash-preview-tts" to "🗣️ Gemini TTS Studio"
                            ),
                            selected = musicModel,
                            accentColor = Color(0xFF10B981),
                            onSelect = {
                                musicModel = it
                                settings.musicModel = it
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        SectionLabel("ভাষা (Language)")
                        ChipSelectorRow(
                            options = listOf(
                                "bn" to "🇧🇩 বাংলা",
                                "en" to "🇺🇸 English",
                                "hi" to "🇮🇳 हिंदी"
                            ),
                            selected = language,
                            accentColor = Color(0xFF10B981),
                            onSelect = {
                                language = it
                                settings.language = it
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        SectionLabel("ভয়েস সিলেক্ট করুন (👑 সুরাইয়া স্পেশাল + মেয়ে ৫টি + ছেলে ৫টি):")

                        val currentVoice = SettingsManager.voicesList.getOrElse(selectedVoiceIndex) {
                            SettingsManager.voicesList[0]
                        }

                        // Dropdown Selector Box
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black.copy(alpha = 0.35f))
                                    .border(
                                        1.5.dp,
                                        if (voiceDropdownExpanded) Color(0xFF10B981) else Color(0xFF10B981).copy(alpha = 0.5f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { voiceDropdownExpanded = true }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .testTag("voice_select_dropdown"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when {
                                            currentVoice.index == 0 -> currentVoice.name
                                            currentVoice.gender == "female" -> "🌸 ${currentVoice.name}"
                                            else -> "👦 ${currentVoice.name}"
                                        },
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Neural Voice: ${currentVoice.geminiVoiceName} • Lang: ${currentVoice.lang} • ${currentVoice.gender.uppercase()}",
                                        color = Color(0xFF34D399),
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = if (voiceDropdownExpanded) "▲" else "▼",
                                    color = Color(0xFF10B981),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = voiceDropdownExpanded,
                                onDismissRequest = { voiceDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.86f)
                                    .heightIn(max = 380.dp)
                                    .background(Color(0xFF181828))
                                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            ) {
                                // Special Signature Suraiya Voice Header
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFFEC4899).copy(alpha = 0.28f), Color(0xFF8B5CF6).copy(alpha = 0.28f))
                                            )
                                        )
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "👑 স্পেশাল সিগনেচার ভয়েস (সবথেকে মিষ্টি ও রিয়েলিস্টিক)",
                                        color = Color(0xFFFDE047),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                SettingsManager.voicesList.firstOrNull { it.index == 0 }?.let { suraiyaVoice ->
                                    val isSelected = selectedVoiceIndex == suraiyaVoice.index
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = suraiyaVoice.name,
                                                    color = if (isSelected) Color(0xFFFDE047) else Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "✨ সবথেকে মিষ্টি ও মানুষের মতো রিয়েলিস্টিক কণ্ঠ (${suraiyaVoice.lang})",
                                                    color = Color(0xFFF472B6),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            settings.geminiKey = geminiKey
                                            settings.voiceKey = voiceKey
                                            selectedVoiceIndex = suraiyaVoice.index
                                            settings.selectedVoiceIndex = suraiyaVoice.index
                                            voiceDropdownExpanded = false
                                            onTestVoice(suraiyaVoice.index)
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Text("👑", fontSize = 14.sp)
                                            }
                                        }
                                    )
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                                // Female section header
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFEC4899).copy(alpha = 0.15f))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "🌸 মেয়েদের ভয়েস (Female Voices - 5 Distinct)",
                                        color = Color(0xFFF472B6),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                SettingsManager.voicesList.filter { it.gender == "female" && it.index > 0 }.forEach { voice ->
                                    val isSelected = selectedVoiceIndex == voice.index
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = voice.name,
                                                    color = if (isSelected) Color(0xFF34D399) else Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = "Neural: ${voice.geminiVoiceName} • ${voice.lang} • মেয়ে কণ্ঠ",
                                                    color = Color.White.copy(alpha = 0.55f),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            settings.geminiKey = geminiKey
                                            settings.voiceKey = voiceKey
                                            selectedVoiceIndex = voice.index
                                            settings.selectedVoiceIndex = voice.index
                                            voiceDropdownExpanded = false
                                            onTestVoice(voice.index)
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Text("✅", fontSize = 13.sp)
                                            }
                                        }
                                    )
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                                // Male section header
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF06B6D4).copy(alpha = 0.15f))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "👦 ছেলেদের ভয়েস (Male Voices - Deep Natural Pitch)",
                                        color = Color(0xFF22D3EE),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                SettingsManager.voicesList.filter { it.gender == "male" }.forEach { voice ->
                                    val isSelected = selectedVoiceIndex == voice.index
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = voice.name,
                                                    color = if (isSelected) Color(0xFF34D399) else Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    text = "Neural: ${voice.geminiVoiceName} • ${voice.lang} • ছেলে কণ্ঠ",
                                                    color = Color.White.copy(alpha = 0.55f),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            settings.geminiKey = geminiKey
                                            settings.voiceKey = voiceKey
                                            selectedVoiceIndex = voice.index
                                            settings.selectedVoiceIndex = voice.index
                                            voiceDropdownExpanded = false
                                            onTestVoice(voice.index)
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Text("✅", fontSize = 13.sp)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Voice Preview Button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.16f))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    settings.geminiKey = geminiKey
                                    settings.voiceKey = voiceKey
                                    settings.selectedVoiceIndex = selectedVoiceIndex
                                    onTestVoice(selectedVoiceIndex)
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("test_selected_voice_button"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔊 সিলেক্ট করা ভয়েস বাজিয়ে শুনুন (${currentVoice.shortName})",
                                color = Color(0xFF34D399),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ভয়েস রেসপন্স ও অ্যাসিস্ট্যান্ট স্ট্যাটাস",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "শীর্ষের একমাত্র 'Voice Assistant' মাস্টার সুইচ দিয়ে সম্পূর্ণ নিয়ন্ত্রিত",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = if (voiceAssistantMasterEnabled) "🟢 সক্রিয় (Active)" else "🔴 নিষ্ক্রিয় (OFF)",
                                color = if (voiceAssistantMasterEnabled) Color(0xFF34D399) else Color(0xFFF87171),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ==========================================
                    // BOX 5: PROFESSIONAL SMART KEYBOARD & USER PROFILE BOX
                    // ==========================================
                    SettingsBoxCard(
                        icon = "⌨️",
                        title = "৫. প্রফেশনাল স্মার্ট বাংলা কিবোর্ড ও প্রোফাইল বক্স",
                        subtitle = "বাংলা + English কিবোর্ড, অটো-কারেকশন, সাজেশন, GIF ও ক্লিপবোর্ড",
                        accentColor = Color(0xFF3B82F6)
                    ) {
                        OutlinedTextField(
                            value = userName,
                            onValueChange = {
                                userName = it
                                settings.userName = it
                            },
                            label = { Text("ব্যবহারকারীর নাম (User Profile Name)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp) },
                            placeholder = { Text("মেহেরাব", color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color.Black.copy(alpha = 0.25f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_user_name_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        SectionLabel("কিবোর্ডের ডিফল্ট ভাষা মোড (Default Keyboard Language)")
                        ChipSelectorRow(
                            options = listOf(
                                "bn_direct" to "🇧🇩 বাংলা (কখগ)",
                                "bn_phonetic" to "✨ অভ্র ফোনেটিক",
                                "en" to "🇺🇸 English (QWERTY)"
                            ),
                            selected = smartKeyboardDefaultLang,
                            accentColor = Color(0xFF7C3AED),
                            onSelect = {
                                smartKeyboardDefaultLang = it
                                settings.smartKeyboardDefaultLang = it
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "বাংলা অটো-কারেকশন (Auto-Correction)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "বানান ভুল হলে স্বয়ংক্রিয়ভাবে সঠিক বাংলা শব্দ বসাবে",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = banglaAutoCorrect,
                                onCheckedChange = {
                                    banglaAutoCorrect = it
                                    settings.banglaAutoCorrect = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF7C3AED))
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "স্মার্ট শব্দ সাজেশন ও প্রেডিকশন (Word Prediction)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "টাইপ করার সময় পরবর্তী বাংলা ও English শব্দ সাজেশন দেখাবে",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = wordPredictionEnabled,
                                onCheckedChange = {
                                    wordPredictionEnabled = it
                                    settings.wordPredictionEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF3B82F6))
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "কিবোর্ড হ্যাপটিক ভাইব্রেশন (Smooth Key Feel)",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = keyboardHapticEnabled,
                                onCheckedChange = {
                                    keyboardHapticEnabled = it
                                    settings.keyboardHapticEnabled = it
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF3B82F6))
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFF2563EB)))
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        settings.userName = userName
                                        settings.smartKeyboardEnabled = smartKeyboardEnabled
                                        settings.smartKeyboardDefaultLang = smartKeyboardDefaultLang
                                        settings.banglaAutoCorrect = banglaAutoCorrect
                                        settings.wordPredictionEnabled = wordPredictionEnabled
                                        settings.keyboardHapticEnabled = keyboardHapticEnabled
                                        onOpenSmartKeyboard()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("open_smart_keyboard_from_settings"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⌨️ স্মার্ট বাংলা কিবোর্ড খুলুন",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                    .clickable { onClearChatMemory() }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("clear_suraiya_memory_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "🧹 মেমোরি ক্লিয়ার",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Bottom Save / Cancel Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF181826))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("settings_cancel_button")
                    ) {
                        Text("বাতিল (Cancel)", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            settings.voiceAssistantMasterEnabled = voiceAssistantMasterEnabled
                            settings.autoSpeak = voiceAssistantMasterEnabled
                            if (aiNameInput.isNotBlank()) {
                                settings.saveAIName(aiNameInput.trim())
                            }
                            settings.geminiKey = geminiKey
                            settings.chatModel = chatModel
                            settings.aiPersona = aiPersona
                            settings.temperature = temperature
                            settings.conversationalMemoryEnabled = conversationalMemoryEnabled
                            if (settings.activeSessionId != activeSessionId) {
                                settings.activeSessionId = activeSessionId
                                onSwitchSession(activeSessionId)
                            }

                            settings.imageApiKey = imageApiKey
                            settings.imageModel = imageModel
                            settings.imageAspectRatio = imageAspectRatio
                            settings.imageEnhancementStyle = imageEnhancementStyle
                            settings.autoEnhanceGallery = autoEnhanceGallery

                            settings.videoApiKey = videoApiKey
                            settings.videoModel = videoModel
                            settings.videoResolution = videoResolution
                            settings.videoAspectRatio = videoAspectRatio

                            settings.voiceKey = voiceKey
                            settings.musicModel = musicModel
                            settings.musicGenre = musicGenre
                            settings.language = language
                            settings.selectedVoiceIndex = selectedVoiceIndex
                            settings.autoSpeak = autoSpeak

                            settings.autoFillEmptyBoxesFromAvailableKey()

                            settings.userName = userName
                            settings.smartKeyboardEnabled = smartKeyboardEnabled
                            settings.smartKeyboardDefaultLang = smartKeyboardDefaultLang
                            settings.banglaAutoCorrect = banglaAutoCorrect
                            settings.wordPredictionEnabled = wordPredictionEnabled
                            settings.keyboardHapticEnabled = keyboardHapticEnabled
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("settings_save_button")
                    ) {
                        Text(
                            text = "✅ সেভ করুন (Save All)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxApiKeyControlBar(
    ownKey: String,
    fallbackKey: String,
    accentColor: Color,
    isTesting: Boolean,
    testResult: Pair<Boolean, String>?,
    pasteButtonTag: String,
    testButtonTag: String,
    onClear: () -> Unit,
    onPaste: () -> Unit,
    onTest: () -> Unit
) {
    val cleanOwn = ownKey.trim()
    val hasOwnKey = cleanOwn.length >= 20
    val hasFallbackKey = fallbackKey.length >= 20

    val statusText = when {
        hasOwnKey -> "🟢 নিজস্ব API Key সংযুক্ত (${cleanOwn.take(6)}...${cleanOwn.takeLast(4)})"
        hasFallbackKey -> "🟢 অটো-সিঙ্ক সক্রিয় (${fallbackKey.take(6)}...${fallbackKey.takeLast(4)})"
        else -> "🔴 কোনো API Key দেওয়া নেই"
    }
    val statusColor = if (hasOwnKey || hasFallbackKey) Color(0xFF4ADE80) else Color(0xFFF87171)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = statusText,
            color = statusColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        if (ownKey.isNotEmpty()) {
            Text(
                text = "🗑️ মুছুন",
                color = Color(0xFFFCA5A5),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onClear() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .clickable { onPaste() }
                .padding(vertical = 10.dp, horizontal = 8.dp)
                .testTag(pasteButtonTag),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📋 Key পেস্ট করুন",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .weight(1.2f)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(accentColor, Color(0xFF6366F1))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .clickable(enabled = !isTesting) { onTest() }
                .padding(vertical = 10.dp, horizontal = 8.dp)
                .testTag(testButtonTag),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isTesting) "⏳ যাচাই হচ্ছে..." else "🔌 সংযোগ পরীক্ষা করুন",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    testResult?.let { (ok, message) ->
        Spacer(modifier = Modifier.height(8.dp))
        ApiTestResultBanner(ok = ok, message = message)
    }
}

@Composable
private fun ApiTestResultBanner(ok: Boolean, message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (ok) Color(0xFF065F46).copy(alpha = 0.4f)
                else Color(0xFF7F1D1D).copy(alpha = 0.45f)
            )
            .border(
                1.dp,
                if (ok) Color(0xFF34D399) else Color(0xFFF87171),
                RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        Text(
            text = if (ok && !message.startsWith("✅") && !message.startsWith("📋") && !message.startsWith("🔄")) {
                "✅ সংযোগ সফল: $message"
            } else {
                message
            },
            color = Color.White,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SettingsBoxCard(
    icon: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A28))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 18.sp)
            }
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = accentColor,
                    fontSize = 11.sp
                )
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 12.dp))
        content()
    }
}

@Composable
private fun ApiKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    showPlain: Boolean,
    accentColor: Color,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp) },
        placeholder = { Text(placeholder, color = Color.White.copy(alpha = 0.35f), fontSize = 12.sp) },
        singleLine = true,
        visualTransformation = if (showPlain) VisualTransformation.None else PasswordVisualTransformation(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = accentColor,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedContainerColor = Color.Black.copy(alpha = 0.25f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.85f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun ChipSelectorRow(
    options: List<Pair<String, String>>,
    selected: String,
    accentColor: Color,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) ->
            val isSelected = key == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) accentColor else Color.White.copy(alpha = 0.06f))
                    .border(
                        1.dp,
                        if (isSelected) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(key) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
