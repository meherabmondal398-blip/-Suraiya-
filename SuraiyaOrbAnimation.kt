package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modern AI glowing orb and dynamic wave animation for Suraiya Voice Assistant.
 * Built with purple, white, and blue accents with breathing glow, orbital particles,
 * and an expressive animated emoji avatar at the center.
 */
@Composable
fun SuraiyaOrbAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    isActive: Boolean = true,
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
    centerEmoji: String? = null,
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "suraiya_orb")

    // Pulse animation for core breathing
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 900 else if (isSpeaking) 1100 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Outer aura expansion
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 1200 else 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_scale"
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 1200 else 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aura_alpha"
    )

    // Rotation for orbiting glow rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 4000 else 9000, easing = LinearEasing)
        ),
        label = "orbit_rotation"
    )

    // Wave phase animation for audio waveforms
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 800 else if (isSpeaking) 950 else 2400, easing = LinearEasing)
        ),
        label = "wave_phase"
    )

    val interactionSource = remember { MutableInteractionSource() }

    val activeEmoji = centerEmoji ?: when {
        isListening -> "🎙️"
        isSpeaking -> "🗣️"
        isActive -> "🤖"
        else -> "💤"
    }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = (size.toPx() / 2f) * 0.55f

            // 1. Outer Pulsing Glow Waves (Purple & Blue Aura)
            if (isActive) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6).copy(alpha = auraAlpha * 0.45f),
                            Color(0xFF3B82F6).copy(alpha = auraAlpha * 0.25f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * auraScale
                    ),
                    center = center,
                    radius = baseRadius * auraScale
                )

                // Secondary echoing ring
                drawCircle(
                    color = Color(0xFF60A5FA).copy(alpha = auraAlpha * 0.35f),
                    center = center,
                    radius = baseRadius * (1f + (auraScale - 1f) * 0.7f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 2. Dynamic Audio Reactive Waveform Lines around the Orb
            val waveAmplitude = when {
                isListening -> baseRadius * 0.24f
                isSpeaking -> baseRadius * 0.18f
                isActive -> baseRadius * 0.08f
                else -> 0f
            }

            if (waveAmplitude > 0f) {
                val waveCount = 3
                for (w in 0 until waveCount) {
                    val path = Path()
                    val waveOffset = (w * PI / 3).toFloat()
                    val points = 72
                    for (i in 0..points) {
                        val angle = (i.toFloat() / points) * (2 * PI).toFloat()
                        val waveSin = sin(angle * 4f + wavePhase + waveOffset)
                        val r = baseRadius * pulseScale + (waveSin * waveAmplitude * (1f - (w * 0.25f)))
                        val x = center.x + r * cos(angle)
                        val y = center.y + r * sin(angle)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    path.close()

                    val waveColor = when (w) {
                        0 -> Color(0xFFC084FC).copy(alpha = 0.65f) // Purple
                        1 -> Color(0xFF60A5FA).copy(alpha = 0.55f) // Blue
                        else -> Color(0xFF38BDF8).copy(alpha = 0.45f) // Cyan
                    }
                    drawPath(
                        path = path,
                        color = waveColor,
                        style = Stroke(width = (2.2f + (waveCount - w)).dp.toPx())
                    )
                }
            }

            // 3. Core Glowing Orb Body (Purple, Blue, and White Core)
            val coreColors = if (isActive) {
                listOf(
                    Color.White.copy(alpha = 0.95f),
                    Color(0xFFE0E7FF), // Ice white
                    Color(0xFF818CF8), // Electric indigo
                    Color(0xFF8B5CF6), // Royal purple
                    Color(0xFF3B82F6), // Vibrant blue
                    Color(0xFF1E1B4B)  // Deep cosmic midnight
                )
            } else {
                listOf(
                    Color(0xFF94A3B8),
                    Color(0xFF475569),
                    Color(0xFF1E293B)
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = coreColors,
                    center = Offset(center.x - baseRadius * 0.15f, center.y - baseRadius * 0.15f),
                    radius = baseRadius * pulseScale
                ),
                center = center,
                radius = baseRadius * pulseScale
            )

            // 4. White Specular Highlight Core for 3D Jewel/Orb Depth
            if (isActive) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.92f),
                            Color(0xFFE9D5FF).copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        center = Offset(center.x - baseRadius * 0.22f, center.y - baseRadius * 0.22f),
                        radius = baseRadius * 0.45f * pulseScale
                    ),
                    center = Offset(center.x - baseRadius * 0.22f, center.y - baseRadius * 0.22f),
                    radius = baseRadius * 0.45f * pulseScale
                )
            }

            // 5. Floating Orbital Energy Particles (Purple, Blue & White)
            if (isActive) {
                val particleCount = 6
                for (p in 0 until particleCount) {
                    val pAngle = ((rotationAngle + (p * (360f / particleCount))) * (PI / 180f)).toFloat()
                    val pDist = baseRadius * (1.18f + 0.12f * sin(pAngle * 2f))
                    val px = center.x + pDist * cos(pAngle)
                    val py = center.y + pDist * sin(pAngle)

                    val pColor = when (p % 3) {
                        0 -> Color.White
                        1 -> Color(0xFFC084FC) // Purple
                        else -> Color(0xFF60A5FA) // Blue
                    }

                    drawCircle(
                        color = pColor.copy(alpha = 0.85f),
                        center = Offset(px, py),
                        radius = (2.2f + (p % 2)).dp.toPx()
                    )
                }
            }
        }

        // 6. Center Front-Stage Animated Emoji Avatar (e.g. 🎙️, 🤖, 🗣️, ✨)
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = activeEmoji,
                fontSize = (size.value * 0.22f).sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
