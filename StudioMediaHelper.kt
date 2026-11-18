package com.example

import android.content.Context
import android.graphics.*
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.sin

object StudioMediaHelper {

    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, prefix: String = "img"): String? {
        return try {
            val mediaDir = File(context.filesDir, "chat_media")
            if (!mediaDir.exists()) {
                mediaDir.mkdirs()
            }
            val file = File(mediaDir, "${prefix}_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")
            FileOutputStream(file).use { out ->
                scaleBitmap(bitmap, 1024).compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun loadBitmapFromPath(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return try {
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun loadBitmapFromUri(context: Context, uri: Uri, maxDim: Int = 1024): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return null
            scaleBitmap(original, maxDim)
        } catch (e: Exception) {
            null
        }
    }

    fun scaleBitmap(bitmap: Bitmap, maxDim: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDim
            newHeight = (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            newHeight = maxDim
            newWidth = (maxDim * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 78): String {
        val scaled = scaleBitmap(bitmap, 640)
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val bytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Applies a professional multi-pass studio color grading, lighting bloom,
     * and framing pass to an uploaded gallery/camera photo.
     */
    fun createProEnhancedBitmap(source: Bitmap, style: String): Bitmap {
        val src = scaleBitmap(source, 1024)
        val w = src.width
        val h = src.height
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val colorMatrix = ColorMatrix()
        when (style) {
            "cinematic" -> {
                // High contrast teal/warm cinematic grade
                val contrast = 1.18f
                val translate = (-0.5f * contrast + 0.5f) * 255f
                colorMatrix.set(
                    floatArrayOf(
                        contrast * 1.05f, 0f, 0f, 0f, translate + 6f,
                        0f, contrast * 1.02f, 0f, 0f, translate + 2f,
                        0f, 0f, contrast * 1.08f, 0f, translate + 10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            "anime_art" -> {
                // Vibrant saturated digital art boost
                colorMatrix.setSaturation(1.45f)
                val boost = ColorMatrix(
                    floatArrayOf(
                        1.15f, 0f, 0f, 0f, 8f,
                        0f, 1.12f, 0f, 0f, 6f,
                        0f, 0f, 1.18f, 0f, 12f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                colorMatrix.postConcat(boost)
            }
            "luxury_portrait" -> {
                // Warm golden luxury skin-tone & contrast
                colorMatrix.setSaturation(1.15f)
                val warm = ColorMatrix(
                    floatArrayOf(
                        1.14f, 0f, 0f, 0f, 10f,
                        0f, 1.09f, 0f, 0f, 5f,
                        0f, 0f, 0.98f, 0f, -4f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                colorMatrix.postConcat(warm)
            }
            else -> {
                // Studio HD Pro: crisp contrast + rich saturation
                colorMatrix.setSaturation(1.25f)
                val hd = ColorMatrix(
                    floatArrayOf(
                        1.15f, 0f, 0f, 0f, -10f,
                        0f, 1.15f, 0f, 0f, -10f,
                        0f, 0f, 1.18f, 0f, -6f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                colorMatrix.postConcat(hd)
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(src, 0f, 0f, paint)

        // Subtle studio key-light & vignette depth overlay
        val radialVignette = RadialGradient(
            w * 0.5f,
            h * 0.45f,
            maxOf(w, h) * 0.72f,
            intArrayOf(
                Color.argb(18, 255, 220, 240),
                Color.argb(0, 0, 0, 0),
                Color.argb(95, 8, 6, 18)
            ),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = radialVignette
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), vignettePaint)

        // Subtle Studio Pro badge watermark in bottom-right corner
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(155, 18, 18, 30)
            this.style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (w * 0.028f).coerceIn(18f, 32f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val label = "✨ SURAIYA PRO STUDIO"
        val textWidth = textPaint.measureText(label)
        val pad = 16f
        val rect = RectF(
            w - textWidth - pad * 2.5f,
            h - textPaint.textSize - pad * 2.2f,
            w - pad * 0.8f,
            h - pad * 0.6f
        )
        canvas.drawRoundRect(rect, 18f, 18f, badgePaint)
        canvas.drawText(label, rect.left + pad * 0.85f, rect.bottom - pad * 0.65f, textPaint)

        return result
    }

    /**
     * Generates a rich visual keyframe/concept artwork canvas with atmospheric sky, glowing sun/moon,
     * layered mountains, water reflection, and stars for instant studio previews.
     */
    fun createConceptArtworkBitmap(prompt: String, isVideo: Boolean = false): Bitmap {
        val w = if (isVideo) 960 else 800
        val h = if (isVideo) 540 else 800
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val hash = prompt.hashCode()
        val hue1 = ((hash and 0xFF) % 360).let { if (it < 0) it + 360 else it }.toFloat()
        val hue2 = (hue1 + 48f) % 360f
        val hue3 = (hue1 + 290f) % 360f

        val c1 = Color.HSVToColor(floatArrayOf(hue1, 0.80f, 0.22f))
        val c2 = Color.HSVToColor(floatArrayOf(hue2, 0.85f, 0.58f))
        val c3 = Color.HSVToColor(floatArrayOf(hue3, 0.75f, 0.82f))
        val c4 = Color.HSVToColor(floatArrayOf(hue1, 0.88f, 0.14f))

        val horizonY = h * 0.62f

        // 1. Rich multi-stop sky & water gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(c1, c2, c3, c2, c4),
                floatArrayOf(0f, 0.36f, 0.60f, 0.78f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // 2. Glowing Celestial Sun/Moon with multi-ring corona
        val sunX = w * (0.32f + ((hash and 0x3F) / 180f))
        val sunY = horizonY - h * 0.14f
        val sunRadius = minOf(w, h) * 0.15f

        val coronaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                sunX, sunY, sunRadius * 2.8f,
                intArrayOf(
                    Color.argb(235, 255, 248, 220),
                    Color.argb(145, 244, 114, 182),
                    Color.argb(0, 236, 72, 153)
                ),
                floatArrayOf(0f, 0.38f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(sunX, sunY, sunRadius * 2.8f, coronaPaint)

        val sunCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(245, 255, 250, 235)
        }
        canvas.drawCircle(sunX, sunY, sunRadius * 0.62f, sunCorePaint)

        // 3. Starfield & glowing fireflies in upper sky
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0..55) {
            val sx = ((i * 137 + kotlin.math.abs(hash)) % w).toFloat()
            val sy = ((i * 73 + kotlin.math.abs(hash shr 3)) % (horizonY.toInt().coerceAtLeast(1))).toFloat()
            val alpha = 110 + (i * 29) % 145
            starPaint.color = Color.argb(alpha, 255, 255, 255)
            val sr = (i % 3) + 1.2f
            canvas.drawCircle(sx, sy, sr, starPaint)
        }

        // 4. Layered Atmospheric Mountains / Silhouette Ridge
        val mountainPathFar = Path().apply {
            moveTo(0f, horizonY)
            var x = 0f
            val step = w / 6f
            var idx = 0
            while (x <= w + step) {
                val peakOffset = ((kotlin.math.abs(hash shr (idx + 1)) % 95) + 45).toFloat()
                lineTo(x, horizonY - peakOffset)
                x += step
                idx++
            }
            lineTo(w.toFloat(), horizonY)
            close()
        }
        val mountainFarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(140, 24, 18, 48)
            style = Paint.Style.FILL
        }
        canvas.drawPath(mountainPathFar, mountainFarPaint)

        val mountainPathNear = Path().apply {
            moveTo(0f, horizonY + 10f)
            var x = 0f
            val step = w / 5f
            var idx = 0
            while (x <= w + step) {
                val peakOffset = ((kotlin.math.abs(hash shr (idx + 4)) % 65) + 22).toFloat()
                lineTo(x, horizonY - peakOffset)
                x += step
                idx++
            }
            lineTo(w.toFloat(), horizonY + 10f)
            close()
        }
        val mountainNearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 12, 10, 28)
            style = Paint.Style.FILL
        }
        canvas.drawPath(mountainPathNear, mountainNearPaint)

        // 5. Shimmering Water Ripples below horizon
        val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(115, 255, 230, 210)
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
        }
        for (r in 1..14) {
            val ry = horizonY + r * ((h - horizonY) / 18f)
            val halfSpan = (sunRadius * (1.3f - r * 0.05f)).coerceAtLeast(18f)
            val jitter = ((r * 19 + hash) % 18).toFloat()
            canvas.drawLine(sunX - halfSpan + jitter, ry, sunX + halfSpan + jitter, ry, ripplePaint)
        }

        // 6. Badge & Title overlay
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(185, 13, 13, 24)
        }
        canvas.drawRoundRect(RectF(28f, h - 132f, w - 28f, h - 26f), 24f, 24f, badgePaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(244, 114, 182)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val modeLabel = if (isVideo) "🎬 VEO AI VIDEO KEYFRAME • 1080P" else "🎨 SURAIYA TURBO AI ART"
        canvas.drawText(modeLabel, 52f, h - 86f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
        }
        val cleanPrompt = prompt.take(42) + if (prompt.length > 42) "..." else ""
        canvas.drawText(cleanPrompt, 52f, h - 48f, subPaint)

        return bmp
    }

    /**
     * Creates a studio musical album cover artwork in <100ms, optionally blending the user's
     * uploaded photo (for "ছবির গান" / Photo-to-Song) with glowing soundwaves.
     */
    fun createSongCoverArtBitmap(prompt: String, genre: String, sourceBitmap: Bitmap? = null): Bitmap {
        val base = if (sourceBitmap != null) {
            createProEnhancedBitmap(scaleBitmap(sourceBitmap, 720), "luxury_portrait")
        } else {
            createConceptArtworkBitmap("$prompt $genre", isVideo = false)
        }
        val w = base.width
        val h = base.height
        val result = base.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        // Draw glowing equalizer soundwave bars across the lower-middle of the album cover
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, h * 0.55f, w.toFloat(), h * 0.75f,
                intArrayOf(
                    Color.argb(210, 236, 72, 153),
                    Color.argb(210, 139, 92, 246),
                    Color.argb(210, 16, 185, 129)
                ),
                null,
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        val barCount = 28
        val totalWidth = w * 0.76f
        val startX = (w - totalWidth) / 2f
        val barSpacing = totalWidth / barCount
        val centerY = h * 0.70f
        for (i in 0 until barCount) {
            val phase = sin(i * 0.45 + prompt.hashCode() * 0.1).toFloat()
            val barHeight = (18f + kotlin.math.abs(phase) * 56f)
            val left = startX + i * barSpacing
            val rect = RectF(left, centerY - barHeight / 2f, left + barSpacing * 0.58f, centerY + barHeight / 2f)
            canvas.drawRoundRect(rect, 8f, 8f, wavePaint)
        }
        return result
    }
}

/**
 * Real-time polyphonic ambient melody synthesizer so AI-generated songs have playable musical backing.
 */
class MelodySynthesizer {
    private var playJob: Job? = null
    private var audioTrack: AudioTrack? = null

    fun startMelody(scope: CoroutineScope, genre: String, onComplete: () -> Unit = {}) {
        stop()
        playJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 22050
            // Choose scale frequencies based on genre
            val chordProgression: List<FloatArray> = when {
                genre.contains("রোমান্টিক") || genre.contains("Romantic", ignoreCase = true) -> listOf(
                    floatArrayOf(261.63f, 329.63f, 392.00f, 523.25f), // C major
                    floatArrayOf(220.00f, 261.63f, 329.63f, 440.00f), // A minor
                    floatArrayOf(174.61f, 220.00f, 261.63f, 349.23f), // F major
                    floatArrayOf(196.00f, 246.94f, 293.66f, 392.00f)  // G major
                )
                genre.contains("লো-ফাই") || genre.contains("Lo-Fi", ignoreCase = true) -> listOf(
                    floatArrayOf(293.66f, 349.23f, 440.00f, 523.25f), // Dm7
                    floatArrayOf(196.00f, 246.94f, 293.66f, 349.23f), // G7
                    floatArrayOf(261.63f, 329.63f, 392.00f, 493.88f), // Cmaj7
                    floatArrayOf(220.00f, 261.63f, 329.63f, 392.00f)  // Am7
                )
                else -> listOf(
                    floatArrayOf(220.00f, 277.18f, 329.63f, 440.00f),
                    floatArrayOf(185.00f, 220.00f, 277.18f, 370.00f),
                    floatArrayOf(146.83f, 185.00f, 220.00f, 293.66f),
                    floatArrayOf(164.81f, 207.65f, 246.94f, 329.66f)
                )
            }

            val durationSec = 12
            val totalSamples = sampleRate * durationSec
            val buffer = ShortArray(sampleRate / 2)

            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuf, buffer.size * 2))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                var sampleIndex = 0
                while (isActive && sampleIndex < totalSamples) {
                    for (i in buffer.indices) {
                        val globalSample = sampleIndex + i
                        val timeSec = globalSample.toDouble() / sampleRate
                        val chordIdx = ((timeSec / 1.5).toInt()) % chordProgression.size
                        val chord = chordProgression[chordIdx]
                        val arpIdx = ((timeSec * 4.0).toInt()) % chord.size
                        val leadFreq = chord[arpIdx] * 1.5

                        val notePhase = (timeSec * 4.0) % 1.0
                        val envelope = (1.0 - notePhase).coerceIn(0.1, 1.0)

                        var signal = 0.0
                        for (f in chord) {
                            signal += 0.14 * sin(2.0 * PI * f * timeSec)
                        }
                        signal += 0.22 * envelope * sin(2.0 * PI * leadFreq * timeSec)

                        buffer[i] = (signal * Short.MAX_VALUE * 0.45).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    track.write(buffer, 0, buffer.size)
                    sampleIndex += buffer.size
                }
            } catch (_: Exception) {
            } finally {
                stopTrack()
                onComplete()
            }
        }
    }

    private fun stopTrack() {
        try {
            audioTrack?.stop()
        } catch (_: Exception) {}
        try {
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        stopTrack()
    }
}
