package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GeminiStylePlusMenu(
    visible: Boolean,
    aiDisplayName: String = "Suraiya AI",
    onPickGallery: () -> Unit,
    onOpenCamera: () -> Unit,
    onSelectMode: (StudioMode) -> Unit,
    onOpenSettings: () -> Unit
) {
    AnimatedVisibility(visible = visible) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E1E30), Color(0xFF151522))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✨ $aiDisplayName ক্রিয়েটিভ স্টুডিও (Gemini টুলস)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "⚙️ সেটিংস বক্স",
                    color = Color(0xFFF472B6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Row 1: Gallery Pro Remake & Camera
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlusOptionCard(
                    modifier = Modifier
                        .weight(1.25f)
                        .testTag("plus_menu_gallery"),
                    icon = "🖼️",
                    title = "গ্যালারি প্রো রিমেক",
                    subtitle = "ছবি আপলোড ও প্রফেশনাল এডিট",
                    gradientColors = listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
                    onClick = onPickGallery
                )
                PlusOptionCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("plus_menu_camera"),
                    icon = "📷",
                    title = "ক্যামেরা",
                    subtitle = "লাইভ ছবি তুলুন",
                    gradientColors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                    onClick = onOpenCamera
                )
            }

            // Row 2: AI Image, AI Video (Veo), Song & Music
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlusOptionCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("plus_menu_image_gen"),
                    icon = "🎨",
                    title = "ইমেজ তৈরি",
                    subtitle = "AI Art Studio",
                    gradientColors = listOf(Color(0xFF9333EA), Color(0xFF6D28D9)),
                    onClick = { onSelectMode(StudioMode.IMAGE_GEN) }
                )
                PlusOptionCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("plus_menu_video_gen"),
                    icon = "🎬",
                    title = "ভিডিও তৈরি",
                    subtitle = "Veo Cinema",
                    gradientColors = listOf(Color(0xFF06B6D4), Color(0xFF0E7490)),
                    onClick = { onSelectMode(StudioMode.VIDEO_GEN) }
                )
                PlusOptionCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("plus_menu_music_gen"),
                    icon = "🎵",
                    title = "গান ও মিউজিক",
                    subtitle = "AI Song Studio",
                    gradientColors = listOf(Color(0xFF10B981), Color(0xFF047857)),
                    onClick = { onSelectMode(StudioMode.MUSIC_GEN) }
                )
            }
        }
    }
}

@Composable
private fun PlusOptionCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    gradientColors.map { it.copy(alpha = 0.22f) }
                )
            )
            .border(1.dp, gradientColors.first().copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 16.sp)
        }
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AttachedImagePreviewTray(
    bitmap: Bitmap,
    activeMode: StudioMode,
    onRemove: () -> Unit,
    onQuickSendPreset: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1C1C2D))
            .border(1.dp, Color(0xFFEC4899).copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "আপলোড করা ছবি",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, Color(0xFFEC4899), RoundedCornerShape(12.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "✨ গ্যালারি ছবি যুক্ত হয়েছে",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "নিচের যেকোনো প্রো স্টাইলে ট্যাপ করুন অথবা নিজের মতো নির্দেশ লিখুন:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Text("✕", color = Color.White, fontSize = 13.sp)
            }
        }

        // Quick 1-Tap Pro Remake & Photo-to-Song Chips
        val presets = listOf(
            "🎵 এই ছবির গান বানাও" to "এই ছবিটির আবহ ও সৌন্দর্য নিয়ে একটি চমৎকার রোমান্টিক বাংলা ছবির গান ও সুর তৈরি করো।",
            "✨ প্রো স্টুডিও রিমেক" to "এই ছবিটিকে প্রফেশনাল স্টুডিও এইচডি পোর্ট্রেট ও আকর্ষণীয় লাইটিংয়ে নতুন করে সাজিয়ে দাও।",
            "🎬 সিনেমাটিক লুক" to "এই ছবিটিকে সিনেমাটিক মুভি গ্রেডিং ও ড্রামাটিক লাইটিংয়ে রূপান্তর করো।",
            "👑 লাক্সারি পোর্ট্রেট" to "এই ছবিটিকে লাক্সারি ম্যাগাজিন কভার স্টাইলে রিটাচ ও প্রফেশনাল করে দাও।",
            "🔍 ছবির বিশ্লেষণ" to "এই ছবিটি খুঁটিয়ে দেখে এর বিস্তারিত বর্ণনা ও সৌন্দর্য বিশ্লেষণ করো।"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (chipTitle, fullPrompt) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFEC4899).copy(alpha = 0.3f), Color(0xFF8B5CF6).copy(alpha = 0.3f))
                            )
                        )
                        .border(1.dp, Color(0xFFF472B6).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { onQuickSendPreset(fullPrompt) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = chipTitle,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun PremierMessageBubble(
    message: ChatMessage,
    isPlayingSongId: String?,
    aiDisplayName: String = "Suraiya AI",
    onSpeak: (String) -> Unit,
    onToggleSongPlay: (ChatMessage) -> Unit,
    onSaveMemory: (ChatMessage) -> Unit,
    onInspectBitmap: (Bitmap) -> Unit
) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 4.dp, bottomEnd = 20.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(shape)
                .background(
                    if (message.isUser) {
                        Brush.linearGradient(listOf(Color(0xFFDB2777), Color(0xFF9333EA)))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFF1E1E2E), Color(0xFF171724)))
                    }
                )
                .border(
                    1.dp,
                    if (message.isUser) Color(0xFFF472B6).copy(alpha = 0.45f)
                    else Color.White.copy(alpha = 0.12f),
                    shape
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mode / Style Badge if present
            if (!message.styleBadge.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = message.styleBadge,
                        color = Color(0xFFF472B6),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // User's Attached Image (if uploaded from Gallery/Camera)
            message.attachedBitmap?.let { bmp ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "আপলোড করা ছবি",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onInspectBitmap(bmp) }
                    )
                    Text(
                        text = "📸 আপলোড করা মূল ছবি",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                }
            }

            // AI's Pro Remade Image / Generated AI Image / Video Keyframe
            message.generatedBitmap?.let { bmp ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (message.videoMetadata != null) 175.dp else 230.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFFEC4899).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable { onInspectBitmap(bmp) }
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "তৈরি করা প্রফেশনাল ছবি",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("🔍 ফুলস্ক্রিন", color = Color.White, fontSize = 10.sp)
                    }
                }
            }

            // Video Studio Metadata Card
            message.videoMetadata?.let { video ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF06B6D4).copy(alpha = 0.14f))
                        .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🎬 ${video.veoModel} • ${video.resolution} (${video.aspectRatio})",
                        color = Color(0xFF67E8F9),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "স্ট্যাটাস: ${video.operationStatus}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
            }

            // Song & Music Studio Interactive Player Card
            message.songMetadata?.let { song ->
                val isPlaying = isPlayingSongId == message.id
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF065F46).copy(alpha = 0.55f), Color(0xFF1E1B4B).copy(alpha = 0.65f))
                            )
                        )
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { onToggleSongPlay(message) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPlaying) Color(0xFFEF4444) else Color(0xFF10B981)
                                )
                        ) {
                            Text(if (isPlaying) "⏹" else "▶️", fontSize = 16.sp, color = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${song.genre} • ${song.bpm}",
                                color = Color(0xFF6EE7B7),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "কর্ডস: ${song.chords}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    if (isPlaying) {
                        Text(
                            text = "🎶 মিউজিক ও ${aiDisplayName}-এর কণ্ঠস্বর বাজছে...",
                            color = Color(0xFFA7F3D0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Main Text or Gemini-style Rotating & Glowing "সুরাইয়া ভাবছে" Indicator
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            var isCopied by remember { mutableStateOf(false) }

            val copyMessageText = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("Suraiya Chat Text", message.text))
                isCopied = true
                Toast.makeText(
                    context,
                    if (message.isUser) "📋 আপনার লেখা কপি করা হয়েছে!" else "📋 ${aiDisplayName}-এর লেখা কপি করা হয়েছে!",
                    Toast.LENGTH_SHORT
                ).show()
                coroutineScope.launch {
                    delay(1800)
                    isCopied = false
                }
            }

            if (message.isLoading || (message.isStreaming && message.text.isBlank())) {
                SuraiyaGeminiThinkingIndicator(
                    statusText = if (message.text.isNotBlank()) message.text else "⚡ লাইভ স্ট্রিমিং শুরু হচ্ছে..."
                )
            } else {
                // Message text row with quick Copy Icon beside the text (for both User & AI messages)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (message.isStreaming) "${message.text} ▍" else message.text,
                            color = Color(0xFFF8FAFC),
                            fontSize = 15.sp,
                            lineHeight = 23.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Inline Copy Icon Button right next to the text (works for both User & AI text)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCopied) Color(0xFF10B981).copy(alpha = 0.35f)
                                else Color.White.copy(alpha = 0.12f)
                            )
                            .border(
                                1.dp,
                                if (isCopied) Color(0xFF34D399) else Color.White.copy(alpha = 0.22f),
                                CircleShape
                            )
                            .clickable { copyMessageText() }
                            .testTag(if (message.isUser) "copy_user_text_icon" else "copy_ai_text_icon"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isCopied) "✅" else "📋",
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Bottom Action Bar for User messages (Timestamp + Copy button)
            if (message.isUser && !message.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.timestamp,
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 10.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isCopied) Color(0xFF10B981).copy(alpha = 0.3f)
                                else Color.Black.copy(alpha = 0.22f)
                            )
                            .border(
                                1.dp,
                                if (isCopied) Color(0xFF34D399) else Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { copyMessageText() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("copy_user_message_button")
                    ) {
                        Text(
                            text = if (isCopied) "✅ কপি হয়েছে" else "📋 কপি করুন",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Bottom Action Bar for AI replies (Timestamp + Listen + Copy + Save Memory)
            if (!message.isUser && !message.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.timestamp,
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 10.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isCopied) Color(0xFF10B981).copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.08f)
                                )
                                .border(
                                    1.dp,
                                    if (isCopied) Color(0xFF34D399) else Color.White.copy(alpha = 0.14f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { copyMessageText() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("copy_ai_message_button")
                        ) {
                            Text(
                                text = if (isCopied) "✅ কপি হয়েছে" else "📋 কপি",
                                color = if (isCopied) Color(0xFF6EE7B7) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .clickable { onSpeak(message.text) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🔊 শুনুন", color = Color.White, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.07f))
                                .clickable { onSaveMemory(message) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("❤️ স্মৃতি", color = Color(0xFFF472B6), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuraiyaGeminiThinkingIndicator(
    statusText: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gemini_thinking_transition")

    // Continuous 360-degree smooth rotation like Gemini sparkle icon
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gemini_star_rotation"
    )

    // Counter-rotation for outer aura ring
    val auraRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gemini_aura_rotation"
    )

    // Breathing / pulsing scale for the Gemini star
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gemini_star_pulse"
    )

    // Dynamic color-shifting glow (Pink -> Purple -> Cyan -> Gold -> Blue like Gemini)
    val glowColor1 by infiniteTransition.animateColor(
        initialValue = Color(0xFFEC4899),
        targetValue = Color(0xFF06B6D4),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gemini_glow_color_1"
    )

    val glowColor2 by infiniteTransition.animateColor(
        initialValue = Color(0xFF8B5CF6),
        targetValue = Color(0xFFFBBF24),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gemini_glow_color_2"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        glowColor1.copy(alpha = 0.16f),
                        glowColor2.copy(alpha = 0.14f),
                        Color(0xFF1E1E32)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    listOf(glowColor1.copy(alpha = 0.7f), glowColor2.copy(alpha = 0.7f), Color(0xFF3B82F6).copy(alpha = 0.6f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("suraiya_thinking_indicator"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Rotating & Color-Glowing Gemini Sparkle Icon Box
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer glowing halo that pulses and shifts colors
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .scale(pulseScale)
                    .rotate(auraRotation)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                glowColor1.copy(alpha = 0.45f),
                                glowColor2.copy(alpha = 0.45f),
                                Color(0xFF3B82F6).copy(alpha = 0.45f),
                                Color(0xFF10B981).copy(alpha = 0.45f),
                                glowColor1.copy(alpha = 0.45f)
                            )
                        )
                    )
            )

            // Custom Canvas drawing the signature Gemini 4-Point Curved Star that rotates & glows
            Canvas(
                modifier = Modifier
                    .size(32.dp)
                    .scale(pulseScale)
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val outerR = size.minDimension * 0.48f
                val innerR = size.minDimension * 0.12f

                // Soft radial neon glow behind the star
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor1.copy(alpha = 0.65f),
                            glowColor2.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = outerR * 1.25f
                    ),
                    radius = outerR * 1.25f,
                    center = Offset(cx, cy)
                )

                // Rotate the 4-pointed Gemini sparkle star continuously
                rotate(degrees = rotationAngle, pivot = Offset(cx, cy)) {
                    val starPath = Path().apply {
                        moveTo(cx, cy - outerR)
                        quadraticTo(cx + innerR, cy - innerR, cx + outerR, cy)
                        quadraticTo(cx + innerR, cy + innerR, cx, cy + outerR)
                        quadraticTo(cx - innerR, cy + innerR, cx - outerR, cy)
                        quadraticTo(cx - innerR, cy - innerR, cx, cy - outerR)
                        close()
                    }

                    drawPath(
                        path = starPath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFDE047),
                                glowColor1,
                                glowColor2,
                                Color(0xFF38BDF8)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        )
                    )
                }

                // Bright glowing core at center of the Gemini star
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = size.minDimension * 0.08f,
                    center = Offset(cx, cy)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "সুরাইয়া ভাবছে...",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "✨",
                    fontSize = 13.sp,
                    modifier = Modifier.scale(pulseScale)
                )
            }
            if (statusText.isNotBlank() && statusText != "সুরাইয়া ভাবছে...") {
                Text(
                    text = statusText.replace("সুরাইয়া ভাবছে...", "").trim(),
                    color = glowColor1,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Text(
                    text = "জেমিনি এআই তোমার জন্য সুন্দর উত্তর সাজাচ্ছে...",
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun MemoriesTabView(
    modifier: Modifier = Modifier,
    memories: List<MemoryItem>,
    onAddMemory: (String, String) -> Unit,
    onDeleteMemory: (String) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var contentInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "💖 সুরাইয়ার স্মৃতি ডায়েরি (AI Memory Hub)",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "এখানে যা সংরক্ষণ করবেন সুরাইয়া চ্যাটের সময় তা সবসময় মনে রাখবে।",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )

        // Add New Memory Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color(0xFFEC4899).copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                placeholder = { Text("স্মৃতির শিরোনাম (যেমন: আমার প্রিয় শখ)", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFEC4899),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = contentInput,
                onValueChange = { contentInput = it },
                placeholder = { Text("বিস্তারিত লিখুন...", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFFEC4899),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (contentInput.isNotBlank()) {
                        onAddMemory(titleInput, contentInput)
                        titleInput = ""
                        contentInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("➕ স্মৃতি যুক্ত করুন", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(memories, key = { it.id }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1A2A))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.title, color = Color(0xFFF472B6), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(item.content, color = Color.White, fontSize = 13.sp)
                        Text(item.date, color = Color.White.copy(alpha = 0.45f), fontSize = 10.sp)
                    }
                    IconButton(onClick = { onDeleteMemory(item.id) }) {
                        Text("🗑️", fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun GalleryTabView(
    modifier: Modifier = Modifier,
    items: List<GalleryItem>,
    onPickFromDeviceGallery: () -> Unit,
    onInspectBitmap: (Bitmap) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🖼️ প্রো স্টুডিও গ্যালারি (Media Hub)",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "তোমার তৈরি করা প্রফেশনাল ছবি, ভিডিও ও গান",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = onPickFromDeviceGallery,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("📤 ছবি আপলোড", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("✨🖼️", fontSize = 40.sp)
                    Text(
                        text = "এখনও কোনো স্টুডিও মিডিয়া তৈরি হয়নি",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "চ্যাট বক্সের বাম পাশের ➕ আইকনে ট্যাপ করে তোমার গ্যালারি থেকে ছবি আপলোড করো অথবা নতুন এআই ইমেজ, ভিডিও বা গান তৈরি করো!",
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                    Button(
                        onClick = onPickFromDeviceGallery,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
                    ) {
                        Text("🖼️ গ্যালারি থেকে ছবি প্রফেশনাল করুন", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.id }) { media ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF1B1B2B))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = media.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = media.timestamp,
                                color = Color(0xFFF472B6),
                                fontSize = 11.sp
                            )
                        }
                        media.bitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = media.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onInspectBitmap(bmp) }
                            )
                        }
                        Text(
                            text = media.description,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullscreenBitmapDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF12121E))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "প্রফেশনাল স্টুডিও প্রিভিউ",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 440.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899))
            ) {
                Text("বন্ধ করুন (Close)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
