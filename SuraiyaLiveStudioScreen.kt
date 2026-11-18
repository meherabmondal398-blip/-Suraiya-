package com.example

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SuraiyaLiveStudioScreen(
    aiDisplayName: String,
    isListening: Boolean,
    liveTranscript: String,
    onMicToggle: () -> Unit,
    onClose: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isCameraShareActive by remember { mutableStateOf(false) }
    var isBackgroundRunning by remember { mutableStateOf(SuraiyaLiveService.isRunning) }
    var userSpokenInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F0C24),
                        Color(0xFF1E103A),
                        Color(0xFF090815)
                    )
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glowing circles
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 60.dp)
                .size(300.dp)
                .background(Color(0xFF8B5CF6).copy(alpha = 0.2f), CircleShape)
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                    )
                    Text(
                        text = "🔴 $aiDisplayName লাইভ স্টুডিও",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Background Mode Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isBackgroundRunning) Color(0xFF10B981).copy(alpha = 0.3f)
                                else Color.White.copy(alpha = 0.1f)
                            )
                            .border(
                                1.dp,
                                if (isBackgroundRunning) Color(0xFF34D399) else Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                isBackgroundRunning = !isBackgroundRunning
                                val intent = Intent(context, SuraiyaLiveService::class.java).apply {
                                    putExtra("ai_name", aiDisplayName)
                                }
                                if (isBackgroundRunning) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                    Toast.makeText(context, "✅ লাইভ সেশন ব্যাকগ্রাউন্ডে চালু করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                } else {
                                    context.stopService(intent)
                                    Toast.makeText(context, "🛑 ব্যাকগ্রাউন্ড লাইভ বন্ধ করা হয়েছে।", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isBackgroundRunning) "🛰️ ব্যাকগ্রাউন্ড সচল" else "📡 ব্যাকগ্রাউন্ড",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Close / Minimize
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Text("✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Center Stage: Glowing Orb & Live Transcript
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SuraiyaOrbAnimation(
                    size = 210.dp,
                    isActive = true,
                    isListening = isListening,
                    isSpeaking = false,
                    centerEmoji = if (isCameraShareActive) "📷" else "🎙️",
                    onClick = onMicToggle
                )

                // Live status text card
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF161625).copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isListening) "🎙️ শুনছি... বলুন কি সাহায্য করবো?" else "✨ $aiDisplayName লাইভ শুনছে ও দেখছে",
                            color = Color(0xFFC084FC),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = liveTranscript.ifBlank { "মাইক বাটনে ট্যাপ করে কথা বলুন অথবা স্ক্রিন/ক্যামেরা শেয়ার করুন।" },
                            color = Color.White,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Screen Share / Camera Preview Indicator Box
                if (isCameraShareActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(110.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF06B6D4), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📹 লাইভ ক্যামেরা ও স্ক্রিন শেয়ার সক্রিয়", color = Color(0xFF67E8F9), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("অন্য অ্যাপের স্ক্রিন ও ক্যামেরা এআই দেখতে ও বিশ্লেষণ করতে পারছে।", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        }
                    }
                }
            }

            // Bottom Control Toolbar (Mic, Camera Share, Quick Prompts)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Quick Voice Prompts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LiveQuickChip("✨ স্ক্রিন বিশ্লেষণ করো", onClick = { onSendMessage("আমার বর্তমান স্ক্রিন বা ক্যামেরা ফিড বিশ্লেষণ করে বুঝিয়ে বলো।") })
                    LiveQuickChip("🎶 একটা গান শোনাও", onClick = { onSendMessage("একটি সুন্দর বাংলা গান বা নাশিদ শোনাও।") })
                    LiveQuickChip("💡 আজকের আবহাওয়া", onClick = { onSendMessage("আজকের আবহাওয়া কেমন তা জানাও।") })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Camera / Screen Share Toggle Button
                    Button(
                        onClick = {
                            isCameraShareActive = !isCameraShareActive
                            Toast.makeText(context, if (isCameraShareActive) "📹 লাইভ স্ক্রিন ও ক্যামেরা শেয়ার চালু হয়েছে!" else "⏹️ স্ক্রিন শেয়ার বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCameraShareActive) Color(0xFF06B6D4) else Color.White.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (isCameraShareActive) "📹 স্ক্রিন শেয়ার ON" else "📷 স্ক্রিন/ক্যামেরা শেয়ার",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Main Mic Talk Button
                    Button(
                        onClick = onMicToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListening) Color(0xFFEF4444) else Color(0xFF8B5CF6)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (isListening) "🛑 কথা বলা থামুন" else "🎙️ লাইভ কথা বলুন",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveQuickChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
