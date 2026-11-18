package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Modern AI voice assistant interface with purple, white, and blue accents.
 * Displays the glowing AI orb/wave animation with centered emoji avatar,
 * live transcription, state indicators, and quick voice search pills.
 */
@Composable
fun SuraiyaVoiceAssistantOverlay(
    state: AssistantState,
    liveTranscript: String,
    aiDisplayName: String = "Suraiya AI",
    onCancel: () -> Unit,
    onSuggestionClick: (String) -> Unit
) {
    if (state is AssistantState.Idle) return

    val isListening = state is AssistantState.ListeningCommand
    val isSpeaking = state is AssistantState.SpeakingResponse || state is AssistantState.WakingUp

    val centerEmoji = when {
        isListening -> "🎙️"
        isSpeaking -> "🗣️"
        state is AssistantState.ProcessingCommand -> "⚡"
        state is AssistantState.ExecutingAction -> "🔍"
        else -> "🤖"
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0F0C24).copy(alpha = 0.96f),
                            Color(0xFF18103A).copy(alpha = 0.96f),
                            Color(0xFF090815).copy(alpha = 0.98f)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient glowing background orbs with purple & blue accents
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 40.dp)
                    .size(280.dp)
                    .background(Color(0xFF8B5CF6).copy(alpha = 0.18f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(240.dp)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.15f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar: Assistant Title + Dismiss Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨", fontSize = 16.sp)
                        }
                        Column {
                            Text(
                                text = "$aiDisplayName এআই ভয়েস অ্যাসিস্ট্যান্ট",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ডাকুন “হেলো $aiDisplayName” • যা বলবেন সার্চ করবে",
                                color = Color(0xFFC084FC),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("dismiss_voice_assistant_overlay")
                    ) {
                        Text("✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Center Stage: Glowing AI Orb & Wave Animation with Front Animated Emoji Avatar
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SuraiyaOrbAnimation(
                        size = 230.dp,
                        isActive = true,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        centerEmoji = centerEmoji,
                        onClick = null
                    )

                    // Assistant State Badge
                    val (badgeText, badgeColor) = when (state) {
                        is AssistantState.WakingUp -> "🌸 “হেলো সুরাইয়া” শুনে জেগে উঠেছে" to Color(0xFFC084FC)
                        is AssistantState.ListeningCommand -> "🎙️ মাইক অন • বলুন, কি সার্চ করবো?" to Color(0xFF38BDF8)
                        is AssistantState.ProcessingCommand -> "⚡ সার্চ ও প্রসেস করা হচ্ছে..." to Color(0xFFFDE047)
                        is AssistantState.ExecutingAction -> "🔍 ${state.description}" to Color(0xFF4ADE80)
                        is AssistantState.SpeakingResponse -> "🗣️ সুরাইয়া উত্তর দিচ্ছে..." to Color(0xFFA78BFA)
                        is AssistantState.Idle -> "💤 স্ট্যান্ডবাই" to Color.White
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(badgeColor.copy(alpha = 0.16f))
                            .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User's Live Transcript or Assistant Message Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val displayText = when {
                        liveTranscript.isNotBlank() -> liveTranscript
                        state is AssistantState.ListeningCommand -> "যা বলবেন তাই সার্চ করবে... বলুন:\n“YouTube-এ বাংলা নাশিদ সার্চ করো”"
                        state is AssistantState.WakingUp -> "“জি, বলুন! কি সার্চ করবো?”"
                        state is AssistantState.SpeakingResponse -> state.text
                        else -> "শুনছি..."
                    }

                    Text(
                        text = displayText,
                        color = Color.White,
                        fontSize = if (displayText.length > 50) 15.sp else 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom: Suggestions & Quick Action Pills
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "💡 মুখে বলুন অথবা ট্যাপ করুন (যা বলবেন সার্চ করবে):",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    val suggestions = listOf(
                        "YouTube-এ বাংলা নাশিদ সার্চ করো",
                        "গুগলে আজকের আবহাওয়া সার্চ করো",
                        "আজকের খবর সার্চ করো",
                        "কয়টা বাজে?",
                        "আজকের তারিখ কত?",
                        "ক্যামেরা খোলো",
                        "একটি মিষ্টি কবিতা শোনাও"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color(0xFF8B5CF6).copy(alpha = 0.28f),
                                                Color(0xFF3B82F6).copy(alpha = 0.28f)
                                            )
                                        )
                                    )
                                    .border(1.dp, Color(0xFFC084FC).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                    .clickable { onSuggestionClick(suggestion) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = suggestion,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top Status Bar displaying the single master switch status and glowing orb icon
 */
@Composable
fun VoiceAssistantStatusBar(
    isEnabled: Boolean,
    aiDisplayName: String = "Suraiya AI",
    onToggle: (Boolean) -> Unit,
    onTapOrb: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isEnabled) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF4C1D95).copy(alpha = 0.45f), Color(0xFF1E1B4B).copy(alpha = 0.65f))
                        )
                    )
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.04f))
                }
            )
            .border(
                1.dp,
                if (isEnabled) Color(0xFF8B5CF6).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onTapOrb)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Mini Glowing Orb Icon with centered Emoji
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .then(
                        if (isEnabled) {
                            Modifier.background(
                                Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6)))
                            )
                        } else {
                            Modifier.background(Color.White.copy(alpha = 0.15f))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEnabled) "🎙️" else "🔇",
                    fontSize = 16.sp
                )
            }

            Column {
                Text(
                    text = if (isEnabled) "ভয়েস অ্যাসিস্ট্যান্ট চালু • ডাকুন “হেলো $aiDisplayName”" else "ভয়েস অ্যাসিস্ট্যান্ট বন্ধ (ট্যাপ করে চালু করুন)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isEnabled) "“হেলো $aiDisplayName” বললে সাথে সাথে মাইক অন ও যা বলবেন সার্চ করবে" else "মাস্টার সুইচ বন্ধ থাকায় কোনো সাড়া দেবে না",
                    color = if (isEnabled) Color(0xFFC084FC) else Color.White.copy(alpha = 0.5f),
                    fontSize = 10.sp
                )
            }
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF8B5CF6),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF334155)
            ),
            modifier = Modifier.testTag("main_screen_voice_assistant_toggle")
        )
    }
}

/**
 * Modern Hero Card displayed when chat is empty, showcasing the glowing AI orb with centered emoji
 * and quick voice commands.
 */
@Composable
fun SuraiyaVoiceAssistantHero(
    isEnabled: Boolean,
    aiDisplayName: String = "Suraiya AI",
    onOrbClick: () -> Unit,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Glowing AI Orb with breathing glow, waves, and animated emoji
        SuraiyaOrbAnimation(
            size = 150.dp,
            isActive = isEnabled,
            isListening = false,
            centerEmoji = if (isEnabled) "🤖" else "🔇",
            onClick = onOrbClick
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$aiDisplayName এআই ভয়েস অ্যাসিস্ট্যান্ট",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isEnabled) "ডাকুন “হেলো $aiDisplayName” • যা বলবেন তাই সার্চ করবে" else "ভয়েস অ্যাসিস্ট্যান্ট বন্ধ আছে (সেটিংস থেকে চালু করুন)",
                color = if (isEnabled) Color(0xFFC084FC) else Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Example interaction card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1E1B4B).copy(alpha = 0.65f), Color(0xFF31102F).copy(alpha = 0.65f))
                    )
                )
                .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "✨ কথোপকথন ও সার্চের নিয়ম:",
                    color = Color(0xFFFDE047),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "১. ডাকুন: “হেলো $aiDisplayName”\n২. মাইক অন হবে, অ্যানিমেশন সামনে আসবে এবং $aiDisplayName বলবে: “জি, বলুন! কি সার্চ করবো?”\n৩. আপনি যা বলবেন: “YouTube-এ বাংলা নাশিদ সার্চ করো” বা যেকোনো তথ্য — $aiDisplayName সাথে সাথে সার্চ করে উত্তর দেবে!",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Sample Voice Command Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "কমান্ডের নমুনা (ট্যাপ করে শুনুন):",
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            val sampleCommands = listOf(
                "YouTube-এ বাংলা নাশিদ সার্চ করো",
                "গুগলে আজকের আবহাওয়া সার্চ করো",
                "আজকের তাজা খবর সার্চ করো",
                "কয়টা বাজে?",
                "আজকের তারিখ কত?",
                "ক্যামেরা খোলো"
            )
            sampleCommands.forEach { cmd ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .clickable { onSuggestionClick(cmd) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎙️ “$cmd”",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text("▶", color = Color(0xFFC084FC), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
