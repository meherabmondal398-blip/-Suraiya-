package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SmartKeyboardMode(val label: String, val shortBadge: String) {
    BN_DIRECT("বাংলা কখগ", "বাং"),
    BN_PHONETIC("অভ্র ফোনেটিক", "অভ্র"),
    ENGLISH("English", "EN"),
    NUMBERS_SYMBOLS("১২৩ ও চিহ্ন", "123"),
    EMOJI_GIF("ইমোজি ও GIF", "😊"),
    CLIPBOARD_EDIT("ক্লিপবোর্ড ও টুলস", "📋")
}

// Suraiya Purple + White + Blue Theme Palette for the Smart Keyboard
private val KbBgTop = Color(0xFF1A1438)       // Deep Royal Purple
private val KbBgBottom = Color(0xFF0E172E)    // Deep Sapphire Blue
private val KbPurple = Color(0xFF7C3AED)      // Vibrant Purple
private val KbPurpleLight = Color(0xFFA78BFA) // Soft Lavender White-Purple
private val KbBlue = Color(0xFF2563EB)        // Suraiya Royal Blue
private val KbCyanBlue = Color(0xFF38BDF8)    // Bright Sky Blue
private val KbKeySurface = Color(0xFF27204D)  // Smooth Purple-Blue Key Surface
private val KbActionSurface = Color(0xFF1E2952) // Action Key Blue Surface
private val KbWhite = Color(0xFFFFFFFF)

@Composable
fun SmartBanglaKeyboard(
    visible: Boolean,
    currentText: String,
    settings: SettingsManager,
    isListening: Boolean = false,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoiceTypingClick: () -> Unit = {},
    onClose: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val view = LocalView.current

    val initialMode = remember(settings.smartKeyboardDefaultLang) {
        when (settings.smartKeyboardDefaultLang) {
            "bn_phonetic" -> SmartKeyboardMode.BN_PHONETIC
            "en" -> SmartKeyboardMode.ENGLISH
            else -> SmartKeyboardMode.BN_DIRECT
        }
    }

    var activeMode by remember { mutableStateOf(initialMode) }
    var lastLanguageMode by remember {
        mutableStateOf(
            if (initialMode == SmartKeyboardMode.ENGLISH) SmartKeyboardMode.ENGLISH
            else SmartKeyboardMode.BN_DIRECT
        )
    }
    var showSworJuktoSubLayer by remember { mutableStateOf(false) }
    var isEnglishUppercase by remember { mutableStateOf(false) }
    var isBanglaNumbersInSym by remember { mutableStateOf(true) }
    var emojiOrGifTab by remember { mutableIntStateOf(0) } // 0 = Emoji, 1 = GIF Cards
    var phoneticBuffer by remember { mutableStateOf("") }
    var isAllSelected by remember { mutableStateOf(false) }
    var clipboardItems by remember { mutableStateOf(settings.getClipboardHistory()) }

    fun triggerKeyHaptic() {
        if (settings.keyboardHapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    // Commit any pending phonetic buffer into Bengali
    fun flushPhoneticBuffer(): String {
        if (phoneticBuffer.isBlank()) return currentText
        val rawConverted = AvroPhoneticEngine.transliterateWord(phoneticBuffer)
        val finalWord = if (settings.banglaAutoCorrect) {
            BanglaAutoCorrectEngine.autoCorrectWord(rawConverted)
        } else {
            rawConverted
        }
        val base = if (isAllSelected) "" else currentText
        isAllSelected = false
        val updated = if (base.isEmpty() || base.endsWith(" ")) {
            base + finalWord
        } else {
            "$base $finalWord"
        }
        phoneticBuffer = ""
        onTextChange(updated)
        return updated
    }

    fun appendDirectText(str: String, addSpaceAfter: Boolean = false) {
        triggerKeyHaptic()
        val baseAfterPhonetic = if (phoneticBuffer.isNotBlank()) flushPhoneticBuffer() else currentText
        val base = if (isAllSelected) "" else baseAfterPhonetic
        isAllSelected = false
        val next = if (addSpaceAfter) {
            if (base.isEmpty() || base.endsWith(" ")) "$base$str " else "$base $str "
        } else {
            base + str
        }
        onTextChange(next)
    }

    fun replaceLastWordWithSuggestion(suggestion: String) {
        triggerKeyHaptic()
        if (phoneticBuffer.isNotBlank()) {
            val base = if (isAllSelected) "" else currentText
            isAllSelected = false
            val updated = if (base.isEmpty() || base.endsWith(" ")) {
                "$base$suggestion "
            } else {
                "$base $suggestion "
            }
            phoneticBuffer = ""
            onTextChange(updated)
            return
        }

        if (isAllSelected) {
            isAllSelected = false
            onTextChange("$suggestion ")
            return
        }

        val trimmed = currentText
        if (trimmed.isEmpty() || trimmed.endsWith(" ")) {
            onTextChange("$trimmed$suggestion ")
        } else {
            val lastSpaceIdx = trimmed.lastIndexOf(' ')
            val prefix = if (lastSpaceIdx >= 0) trimmed.substring(0, lastSpaceIdx + 1) else ""
            onTextChange("$prefix$suggestion ")
        }
    }

    fun handleSpaceKey() {
        triggerKeyHaptic()
        if (phoneticBuffer.isNotBlank()) {
            val updated = flushPhoneticBuffer()
            onTextChange("$updated ")
            return
        }
        if (isAllSelected) {
            isAllSelected = false
            onTextChange(" ")
            return
        }
        if (settings.banglaAutoCorrect && currentText.isNotBlank() && !currentText.endsWith(" ")) {
            val correctedText = BanglaAutoCorrectEngine.autoCorrectLastWordInSentence(currentText)
            onTextChange("$correctedText ")
        } else {
            onTextChange("$currentText ")
        }
    }

    fun handleBackspace() {
        triggerKeyHaptic()
        if (isAllSelected) {
            isAllSelected = false
            phoneticBuffer = ""
            onTextChange("")
            return
        }
        if (phoneticBuffer.isNotEmpty()) {
            phoneticBuffer = phoneticBuffer.dropLast(1)
            return
        }
        if (currentText.isNotEmpty()) {
            val lastCodePoint = Character.codePointBefore(currentText, currentText.length)
            val charCount = Character.charCount(lastCodePoint)
            onTextChange(currentText.substring(0, currentText.length - charCount))
        }
    }

    fun copyToClipboard(cutAfterCopy: Boolean = false) {
        triggerKeyHaptic()
        val full = flushPhoneticBuffer()
        if (full.isBlank()) {
            Toast.makeText(context, "কপি করার জন্য কোনো লেখা নেই", Toast.LENGTH_SHORT).show()
            return
        }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Suraiya Smart Keyboard", full))
        settings.addClipboardItem(full)
        clipboardItems = settings.getClipboardHistory()
        if (cutAfterCopy) {
            onTextChange("")
            isAllSelected = false
            Toast.makeText(context, "✂️ কাট ও ক্লিপবোর্ডে সেভ হয়েছে", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "📋 ক্লিপবোর্ডে কপি হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    fun pasteFromClipboard() {
        triggerKeyHaptic()
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val systemClip = cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
        val clipToPaste = systemClip.ifBlank { clipboardItems.firstOrNull().orEmpty() }
        if (clipToPaste.isBlank()) {
            Toast.makeText(context, "ক্লিপবোর্ড খালি", Toast.LENGTH_SHORT).show()
            return
        }
        settings.addClipboardItem(clipToPaste)
        clipboardItems = settings.getClipboardHistory()
        appendDirectText(clipToPaste)
        Toast.makeText(context, "📥 পেস্ট করা হয়েছে", Toast.LENGTH_SHORT).show()
    }

    fun toggleQuickLanguage() {
        triggerKeyHaptic()
        flushPhoneticBuffer()
        activeMode = when (activeMode) {
            SmartKeyboardMode.BN_DIRECT -> {
                lastLanguageMode = SmartKeyboardMode.BN_PHONETIC
                SmartKeyboardMode.BN_PHONETIC
            }
            SmartKeyboardMode.BN_PHONETIC -> {
                lastLanguageMode = SmartKeyboardMode.ENGLISH
                SmartKeyboardMode.ENGLISH
            }
            SmartKeyboardMode.ENGLISH -> {
                lastLanguageMode = SmartKeyboardMode.BN_DIRECT
                SmartKeyboardMode.BN_DIRECT
            }
            else -> lastLanguageMode
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(KbBgTop, KbBgBottom)
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        KbPurple.copy(alpha = 0.85f),
                        KbWhite.copy(alpha = 0.55f),
                        KbCyanBlue.copy(alpha = 0.85f)
                    )
                ),
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            )
            .padding(horizontal = 6.dp, vertical = 7.dp)
            .testTag("smart_bangla_keyboard"),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // =====================================================
        // 1. TOP SMART TOOLBAR: LANGUAGE SWITCH, VOICE, CLIPBOARD, EDIT, HIDE
        // =====================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmartKeyboardMode.entries.forEach { mode ->
                    val isSelected = activeMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(listOf(KbPurple, KbBlue))
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(KbKeySurface.copy(alpha = 0.85f), KbActionSurface.copy(alpha = 0.85f))
                                    )
                                }
                            )
                            .border(
                                1.dp,
                                if (isSelected) KbWhite.copy(alpha = 0.8f) else KbPurpleLight.copy(alpha = 0.22f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                triggerKeyHaptic()
                                flushPhoneticBuffer()
                                if (mode == SmartKeyboardMode.BN_DIRECT ||
                                    mode == SmartKeyboardMode.BN_PHONETIC ||
                                    mode == SmartKeyboardMode.ENGLISH
                                ) {
                                    lastLanguageMode = mode
                                }
                                activeMode = mode
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = mode.label,
                            color = if (isSelected) KbWhite else KbWhite.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Quick Voice Typing + Hide Keyboard
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (isListening) Color(0xFFEF4444) else KbBlue.copy(alpha = 0.35f)
                        )
                        .border(1.dp, KbCyanBlue.copy(alpha = 0.6f), CircleShape)
                        .clickable {
                            triggerKeyHaptic()
                            flushPhoneticBuffer()
                            onVoiceTypingClick()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("keyboard_voice_typing_button")
                ) {
                    Text(
                        text = if (isListening) "🔴 শুনছি" else "🎙️ ভয়েস",
                        color = KbWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(KbKeySurface)
                        .border(1.dp, KbPurpleLight.copy(alpha = 0.35f), CircleShape)
                        .clickable {
                            triggerKeyHaptic()
                            flushPhoneticBuffer()
                            onClose()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("close_bangla_keyboard_button")
                ) {
                    Text(
                        text = "⬇",
                        color = KbCyanBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // =====================================================
        // 2. WORD SUGGESTION, PREDICTION & QUICK EDIT BAR
        // =====================================================
        val suggestions = remember(phoneticBuffer, currentText, activeMode, settings.wordPredictionEnabled) {
            if (!settings.wordPredictionEnabled) {
                emptyList()
            } else if (phoneticBuffer.isNotBlank()) {
                AvroPhoneticEngine.getSuggestions(phoneticBuffer)
            } else {
                BanglaAutoCorrectEngine.getPredictionsAndCompletions(
                    text = currentText,
                    isEnglishMode = activeMode == SmartKeyboardMode.ENGLISH
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF100C24).copy(alpha = 0.85f))
                .border(1.dp, KbPurpleLight.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick Select All / Copy / Paste mini pills on left
            QuickEditMiniPill(
                label = if (isAllSelected) "✅ সব সিলেক্টেড" else "☑️ সব",
                active = isAllSelected,
                onClick = {
                    triggerKeyHaptic()
                    flushPhoneticBuffer()
                    if (currentText.isNotEmpty()) {
                        isAllSelected = !isAllSelected
                    }
                }
            )
            QuickEditMiniPill(
                label = "📋 কপি",
                active = false,
                onClick = { copyToClipboard(cutAfterCopy = false) }
            )
            QuickEditMiniPill(
                label = "📥 পেস্ট",
                active = false,
                onClick = { pasteFromClipboard() }
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(18.dp)
                    .background(KbWhite.copy(alpha = 0.2f))
            )

            if (phoneticBuffer.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(KbBlue.copy(alpha = 0.35f))
                        .border(1.dp, KbCyanBlue, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$phoneticBuffer ➔",
                        color = KbWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            suggestions.forEachIndexed { idx, word ->
                val isPrimary = idx == 0 && (phoneticBuffer.isNotBlank() || settings.banglaAutoCorrect)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isPrimary) {
                                Brush.horizontalGradient(listOf(KbPurple, KbBlue))
                            } else {
                                Brush.horizontalGradient(
                                    listOf(KbKeySurface, KbActionSurface)
                                )
                            }
                        )
                        .border(
                            1.dp,
                            if (isPrimary) KbCyanBlue else KbWhite.copy(alpha = 0.18f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            replaceLastWordWithSuggestion(word)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = word,
                        color = KbWhite,
                        fontSize = 12.sp,
                        fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // =====================================================
        // 3. ACTIVE KEYBOARD LAYOUT BODY
        // =====================================================
        when (activeMode) {
            SmartKeyboardMode.BN_DIRECT -> {
                // Top Pinned Kar-Matra + Toggle for Sworborno & Juktoborno
                val karList = remember {
                    listOf("া", "ি", "ী", "ু", "ূ", "ৃ", "ে", "ৈ", "ো", "ৌ", "্", "্র", "্য", "ঁ", "ং")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle between কখগ (Byanjon) and অআই/যুক্তবর্ণ
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (showSworJuktoSubLayer) Brush.horizontalGradient(listOf(KbBlue, KbPurple))
                                else Brush.horizontalGradient(listOf(KbPurple, KbBlue))
                            )
                            .border(1.dp, KbWhite.copy(alpha = 0.6f), RoundedCornerShape(9.dp))
                            .clickable {
                                triggerKeyHaptic()
                                showSworJuktoSubLayer = !showSworJuktoSubLayer
                            }
                            .padding(horizontal = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showSworJuktoSubLayer) "কখগ ব্যঞ্জন" else "অআই/যুক্ত",
                            color = KbWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    karList.forEach { kar ->
                        Box(
                            modifier = Modifier
                                .widthIn(min = 34.dp)
                                .height(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(KbPurple.copy(alpha = 0.32f))
                                .border(1.dp, KbCyanBlue.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
                                .clickable { appendDirectText(kar) }
                                .padding(horizontal = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = kar,
                                color = KbWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                val rows = remember(showSworJuktoSubLayer) {
                    if (showSworJuktoSubLayer) {
                        listOf(
                            listOf("অ", "আ", "ই", "ঈ", "উ", "ঊ", "ঋ", "এ", "ঐ", "ও", "ঔ"),
                            listOf("ক্ষ", "জ্ঞ", "প্র", "ত্র", "ক্ত", "ন্ত", "ন্দ", "স্থ", "ষ্ট", "শ্চ"),
                            listOf("ম্প", "ল্ল", "ঙ্গ", "দ্ধ", "ত্ত", "শ্ন", "স্ম", "ক্ল", "দ্র", "ব্র"),
                            listOf("০", "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯")
                        )
                    } else {
                        listOf(
                            listOf("ক", "খ", "গ", "ঘ", "ঙ", "চ", "ছ", "জ", "ঝ"),
                            listOf("ট", "ঠ", "ড", "ঢ", "ণ", "ত", "থ", "দ", "ধ", "ন"),
                            listOf("প", "ফ", "ব", "ভ", "ম", "য", "র", "ল", "শ"),
                            listOf("ষ", "স", "হ", "ড়", "ঢ়", "য়", "ৎ", "ঃ", "ঞ")
                        )
                    }
                }

                rows.forEach { rowKeys ->
                    KeyboardKeyRow(
                        keys = rowKeys,
                        onKeyClick = { key -> appendDirectText(key) }
                    )
                }
            }

            SmartKeyboardMode.BN_PHONETIC -> {
                val phoneticRows = remember {
                    listOf(
                        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                        listOf("z", "x", "c", "v", "b", "n", "m")
                    )
                }

                val quickKarRow = remember {
                    listOf("া", "ি", "ী", "ু", "ে", "ো", "্", "্র", "ঁ", "ং", "আমি", "তুমি", "সুরাইয়া")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickKarRow.forEach { item ->
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KbActionSurface)
                                .border(1.dp, KbCyanBlue.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .clickable {
                                    flushPhoneticBuffer()
                                    val isKar = item.length <= 2 && item[0].code in 0x0981..0x09CD && item !in listOf("আমি", "তুমি", "সুরাইয়া")
                                    appendDirectText(item, addSpaceAfter = !isKar)
                                }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                color = KbWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                phoneticRows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowKeys.forEach { k ->
                            val hintBn = AvroPhoneticEngine.singleKeyHint(k)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(41.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(KbKeySurface)
                                    .border(1.dp, KbPurpleLight.copy(alpha = 0.25f), RoundedCornerShape(9.dp))
                                    .clickable {
                                        triggerKeyHaptic()
                                        phoneticBuffer += k
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = hintBn,
                                    color = KbCyanBlue,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = k.uppercase(),
                                    color = KbWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            SmartKeyboardMode.ENGLISH -> {
                val numberRow = remember { listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0") }
                val qwertyRows = remember(isEnglishUppercase) {
                    val base = listOf(
                        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                        listOf("⇧", "z", "x", "c", "v", "b", "n", "m", ".")
                    )
                    if (isEnglishUppercase) {
                        base.map { row -> row.map { if (it == "⇧" || it == ".") it else it.uppercase() } }
                    } else {
                        base
                    }
                }

                KeyboardKeyRow(
                    keys = numberRow,
                    keyHeight = 34.dp,
                    onKeyClick = { key -> appendDirectText(key) }
                )

                qwertyRows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowKeys.forEach { key ->
                            val isShift = key == "⇧"
                            Box(
                                modifier = Modifier
                                    .weight(if (isShift) 1.3f else 1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        if (isShift && isEnglishUppercase) Brush.horizontalGradient(listOf(KbPurple, KbBlue))
                                        else Brush.horizontalGradient(listOf(KbKeySurface, KbKeySurface))
                                    )
                                    .border(
                                        1.dp,
                                        if (isShift && isEnglishUppercase) KbCyanBlue else KbPurpleLight.copy(alpha = 0.25f),
                                        RoundedCornerShape(9.dp)
                                    )
                                    .clickable {
                                        if (isShift) {
                                            triggerKeyHaptic()
                                            isEnglishUppercase = !isEnglishUppercase
                                        } else {
                                            appendDirectText(key)
                                            if (isEnglishUppercase) isEnglishUppercase = false
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    color = KbWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            SmartKeyboardMode.NUMBERS_SYMBOLS -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isBanglaNumbersInSym) KbPurple else KbActionSurface)
                            .border(1.dp, KbWhite.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable {
                                triggerKeyHaptic()
                                isBanglaNumbersInSym = true
                            }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text("বাংলা সংখ্যা (০-৯)", color = KbWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!isBanglaNumbersInSym) KbBlue else KbActionSurface)
                            .border(1.dp, KbWhite.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable {
                                triggerKeyHaptic()
                                isBanglaNumbersInSym = false
                            }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text("English Digits (0-9)", color = KbWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                val numAndSymRows = remember(isBanglaNumbersInSym) {
                    val digits = if (isBanglaNumbersInSym) {
                        listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০")
                    } else {
                        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                    }
                    listOf(
                        digits,
                        listOf("@", "#", "৳", "$", "%", "&", "-", "+", "(", ")"),
                        listOf("*", "\"", "'", ":", ";", "!", "?", "/", "=", "~"),
                        listOf("।", ",", ".", "_", "<", ">", "[", "]", "{", "}")
                    )
                }

                numAndSymRows.forEach { rowKeys ->
                    KeyboardKeyRow(
                        keys = rowKeys,
                        onKeyClick = { key -> appendDirectText(key) }
                    )
                }
            }

            SmartKeyboardMode.EMOJI_GIF -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (emojiOrGifTab == 0) KbPurple else KbActionSurface)
                            .border(1.dp, KbWhite.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable {
                                triggerKeyHaptic()
                                emojiOrGifTab = 0
                            }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text("😊 প্রিমিয়াম ইমোজি", color = KbWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (emojiOrGifTab == 1) KbBlue else KbActionSurface)
                            .border(1.dp, KbWhite.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable {
                                triggerKeyHaptic()
                                emojiOrGifTab = 1
                            }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text("🎬 এক্সপ্রেসিভ GIF কার্ড", color = KbWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (emojiOrGifTab == 0) {
                    val emojiRows = remember {
                        listOf(
                            listOf("🌸", "❤️", "✨", "👑", "😍", "🥰", "😊", "😂", "🤣", "😭"),
                            listOf("🙏", "🔥", "💖", "🎉", "🎵", "🎨", "🎬", "🌹", "🌟", "💯"),
                            listOf("😘", "🤗", "🤔", "😎", "🤩", "🥳", "😴", "🥺", "👍", "👏"),
                            listOf("🇧🇩", "🦋", "🌙", "☀️", "🌧️", "☕", "🍫", "🎁", "💎", "🚀")
                        )
                    }
                    emojiRows.forEach { rowKeys ->
                        KeyboardKeyRow(
                            keys = rowKeys,
                            onKeyClick = { emoji -> appendDirectText(emoji) }
                        )
                    }
                } else {
                    val gifCards = remember {
                        listOf(
                            "🌸✨ [GIF: মিষ্টি হাসি ও ফুলের শুভেচ্ছা] ✨🌸" to "🌸 মিষ্টি শুভেচ্ছা GIF",
                            "💖👑 [GIF: ভালোবাসা ও রাজকীয় হার্ট অ্যানিমেশন] 👑💖" to "💖 রয়্যাল লাভ GIF",
                            "🎉🎊 [GIF: অসংখ্য অভিনন্দন ও সেলিব্রেশন] 🎊🎉" to "🎉 সেলিব্রেশন GIF",
                            "🎵🎶 [GIF: মিউজিক ও গানের সুরের ঢেউ] 🎶🎵" to "🎵 মিউজিক ভাইব GIF",
                            "🔥🚀 [GIF: সুপার স্মার্ট এআই পাওয়ার] 🚀🔥" to "🚀 সুপার এআই GIF",
                            "☕🌧️ [GIF: বৃষ্টির দিনে গরম কফির আড্ডা] 🌧️☕" to "☕ বৃষ্টির আড্ডা GIF",
                            "🙏✨ [GIF: আন্তরিক ধন্যবাদ ও কৃতজ্ঞতা] ✨🙏" to "🙏 ধন্যবাদ GIF",
                            "🌙⭐ [GIF: শুভ রাত্রি ও মিষ্টি স্বপ্নের তারা] ⭐🌙" to "🌙 শুভ রাত্রি GIF"
                        )
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        gifCards.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                pair.forEach { (payload, label) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                Brush.horizontalGradient(listOf(KbPurple.copy(alpha = 0.55f), KbBlue.copy(alpha = 0.55f)))
                                            )
                                            .border(1.dp, KbCyanBlue.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                            .clickable {
                                                appendDirectText(payload, addSpaceAfter = true)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = KbWhite,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            SmartKeyboardMode.CLIPBOARD_EDIT -> {
                // Full Edit Action Row: Select All, Copy, Paste, Cut, Clear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    EditActionButton(
                        icon = "☑️",
                        title = if (isAllSelected) "সিলেক্টেড" else "সব সিলেক্ট",
                        modifier = Modifier.weight(1f),
                        active = isAllSelected,
                        onClick = {
                            triggerKeyHaptic()
                            flushPhoneticBuffer()
                            if (currentText.isNotEmpty()) {
                                isAllSelected = !isAllSelected
                                Toast.makeText(
                                    context,
                                    if (isAllSelected) "সব লেখা সিলেক্ট করা হয়েছে" else "সিলেকশন সরানো হয়েছে",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                    EditActionButton(
                        icon = "📋",
                        title = "কপি (Copy)",
                        modifier = Modifier.weight(1f),
                        onClick = { copyToClipboard(cutAfterCopy = false) }
                    )
                    EditActionButton(
                        icon = "📥",
                        title = "পেস্ট (Paste)",
                        modifier = Modifier.weight(1f),
                        onClick = { pasteFromClipboard() }
                    )
                    EditActionButton(
                        icon = "✂️",
                        title = "কাট (Cut)",
                        modifier = Modifier.weight(1f),
                        onClick = { copyToClipboard(cutAfterCopy = true) }
                    )
                }

                // Saved Clipboard History Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📌 ক্লিপবোর্ড ও রেডি মেসেজ (ট্যাপ করলেই পেস্ট হবে):",
                        color = KbCyanBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "সব মুছুন",
                        color = Color(0xFFFCA5A5),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                settings.clearClipboardHistory()
                                clipboardItems = settings.getClipboardHistory()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    clipboardItems.take(6).chunked(2).forEach { rowClips ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowClips.forEach { clipText ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(KbKeySurface)
                                        .border(1.dp, KbPurpleLight.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            appendDirectText(clipText, addSpaceAfter = true)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = clipText,
                                        color = KbWhite,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================
        // 4. BOTTOM MODERN KEYBOARD BAR: 123, GLOBE SWITCH, SPACE, DARI, BACKSPACE, SEND
        // =====================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 123 / ABC Toggle
            ActionKeyButton(
                label = if (activeMode == SmartKeyboardMode.NUMBERS_SYMBOLS) lastLanguageMode.shortBadge else "১২৩",
                modifier = Modifier.width(46.dp),
                bgColor = KbActionSurface,
                onClick = {
                    triggerKeyHaptic()
                    flushPhoneticBuffer()
                    activeMode = if (activeMode == SmartKeyboardMode.NUMBERS_SYMBOLS) {
                        lastLanguageMode
                    } else {
                        SmartKeyboardMode.NUMBERS_SYMBOLS
                    }
                }
            )

            // Fast Language Switch Key (🌐 বাং / অভ্র / EN)
            ActionKeyButton(
                label = "🌐 ${activeMode.shortBadge}",
                modifier = Modifier.width(58.dp),
                bgColor = KbPurple.copy(alpha = 0.65f),
                onClick = { toggleQuickLanguage() }
            )

            // Emoji Quick Key
            ActionKeyButton(
                label = "😊",
                modifier = Modifier.width(36.dp),
                bgColor = KbActionSurface,
                onClick = {
                    triggerKeyHaptic()
                    flushPhoneticBuffer()
                    activeMode = if (activeMode == SmartKeyboardMode.EMOJI_GIF) {
                        lastLanguageMode
                    } else {
                        SmartKeyboardMode.EMOJI_GIF
                    }
                }
            )

            // Spacebar with live phonetic preview & language label
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(KbKeySurface, KbActionSurface)
                        )
                    )
                    .border(1.dp, KbCyanBlue.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .clickable { handleSpaceKey() }
                    .testTag("bangla_keyboard_space"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        phoneticBuffer.isNotBlank() ->
                            "␣ '${AvroPhoneticEngine.transliterateWord(phoneticBuffer)}'"
                        activeMode == SmartKeyboardMode.ENGLISH ->
                            "␣ ${settings.aiDisplayName} English"
                        activeMode == SmartKeyboardMode.BN_PHONETIC ->
                            "␣ বাংলা ফোনেটিক স্পেস"
                        else ->
                            "␣ ${settings.aiDisplayName} স্মার্ট বাংলা"
                    },
                    color = KbWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            // Punctuation (। in Bangla, . in English)
            val puncChar = if (activeMode == SmartKeyboardMode.ENGLISH) "." else "।"
            ActionKeyButton(
                label = puncChar,
                modifier = Modifier.width(34.dp),
                bgColor = KbActionSurface,
                onClick = {
                    flushPhoneticBuffer()
                    appendDirectText("$puncChar ")
                }
            )

            // Backspace (⌫)
            ActionKeyButton(
                label = "⌫",
                modifier = Modifier.width(44.dp),
                bgColor = Color(0xFF881337).copy(alpha = 0.75f),
                onClick = { handleBackspace() }
            )

            // Direct Send to Suraiya AI (পাঠান ✈️)
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(KbPurple, KbBlue)
                        )
                    )
                    .border(1.dp, KbWhite.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                    .clickable {
                        triggerKeyHaptic()
                        val finalMsg = flushPhoneticBuffer()
                        if (finalMsg.isNotBlank()) {
                            if (settings.banglaAutoCorrect) {
                                val autoFixed = BanglaAutoCorrectEngine.autoCorrectLastWordInSentence(finalMsg)
                                onTextChange(autoFixed)
                            }
                            onSend()
                        }
                    }
                    .testTag("bangla_keyboard_send"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "পাঠান ✈️",
                    color = KbWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun QuickEditMiniPill(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (active) KbBlue else KbActionSurface.copy(alpha = 0.9f))
            .border(
                1.dp,
                if (active) KbWhite else KbCyanBlue.copy(alpha = 0.4f),
                RoundedCornerShape(7.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = KbWhite,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EditActionButton(
    icon: String,
    title: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (active) Brush.horizontalGradient(listOf(KbPurple, KbBlue))
                else Brush.horizontalGradient(listOf(KbKeySurface, KbActionSurface))
            )
            .border(1.dp, KbCyanBlue.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(icon, fontSize = 14.sp)
        Text(
            text = title,
            color = KbWhite,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun KeyboardKeyRow(
    keys: List<String>,
    keyHeight: androidx.compose.ui.unit.Dp = 39.dp,
    onKeyClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keys.forEach { key ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(keyHeight)
                    .clip(RoundedCornerShape(9.dp))
                    .background(KbKeySurface)
                    .border(1.dp, KbPurpleLight.copy(alpha = 0.25f), RoundedCornerShape(9.dp))
                    .clickable { onKeyClick(key) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = key,
                    color = KbWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ActionKeyButton(
    label: String,
    modifier: Modifier = Modifier,
    bgColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, KbWhite.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = KbWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Smart Bengali & English Auto-Correction, Word Completion & Next-Word Prediction Engine
 */
object BanglaAutoCorrectEngine {
    private val banglaAutoCorrections = mapOf(
        "সুরাইয়া" to "সুরাইয়া",
        "ভালবাসি" to "ভালোবাসি",
        "ভাল" to "ভালো",
        "কিভাবে" to "কীভাবে",
        "কিরকম" to "কীরকম",
        "ধন্যবদ" to "ধন্যবাদ",
        "কেমনআছো" to "কেমন আছো",
        "আছ" to "আছো",
        "করছ" to "করছো",
        "বল" to "বলো",
        "কেনো" to "কেন",
        "যদিওবা" to "যদিও",
        "মনভালো" to "মন ভালো",
        "গুডমর্নিং" to "শুভ সকাল",
        // English common auto-corrections
        "teh" to "the",
        "dont" to "don't",
        "cant" to "can't",
        "im" to "I'm",
        "u" to "you",
        "ur" to "your",
        "plz" to "please",
        "thx" to "thanks"
    )

    private val nextWordPredictionsBn = mapOf(
        "আমি" to listOf("তোমাকে", "ভালো আছি", "জানতে চাই", "একটা", "কীভাবে"),
        "তুমি" to listOf("কেমন আছো?", "কী করছো?", "অনেক মিষ্টি", "আমাকে বলো"),
        "সুরাইয়া" to listOf("কেমন আছো?", "একটা গান শোনাও", "আমাকে সাহায্য করো", "তুমি দারুণ"),
        "কেমন" to listOf("আছো?", "আছেন?", "লাগছে?", "চলছে?"),
        "আমার" to listOf("জন্য", "মন ভালো নেই", "নাম মেহেরাব", "একটা প্রশ্ন আছে"),
        "একটা" to listOf("সুন্দর গান", "রোমান্টিক কবিতা", "প্রফেশনাল ছবি", "সিনেমাটিক ভিডিও"),
        "আমাকে" to listOf("বুঝিয়ে বলো", "ধাপে ধাপে বলো", "একটা গল্প বলো", "সাহায্য করো"),
        "ভালো" to listOf("আছি", "লাগছে", "থাকো", "কথা বলো"),
        "শুভ" to listOf("সকাল 🌸", "রাত্রি 🌙", "সন্ধ্যা ✨", "জন্মদিন 🎂")
    )

    private val commonBanglaWords = listOf(
        "সুরাইয়া", "আমি", "তুমি", "কেমন আছো?", "ভালোবাসি", "ধন্যবাদ",
        "আমাকে", "তোমার", "ভালো আছি", "কী করছো?", "গল্প বলো", "গান শোনাও",
        "কবিতা লেখো", "ছবি বানিয়ে দাও", "ভিডিও তৈরি করো", "সহজ বাংলায় বলো",
        "মেহেরাব", "বন্ধু", "সাহায্য করো", "স্বাভাবিক", "প্রফেশনাল"
    )

    private val commonEnglishWords = listOf(
        "Suraiya", "Hello", "How are you?", "Please explain", "Step by step",
        "Thank you", "Generate an image", "Create a song", "Help me", "Smart AI"
    )

    fun autoCorrectWord(word: String): String {
        return banglaAutoCorrections[word] ?: banglaAutoCorrections[word.lowercase()] ?: word
    }

    fun autoCorrectLastWordInSentence(text: String): String {
        val trimmed = text.trimEnd()
        if (trimmed.isEmpty()) return text
        val lastSpace = trimmed.lastIndexOf(' ')
        return if (lastSpace < 0) {
            autoCorrectWord(trimmed)
        } else {
            val prefix = trimmed.substring(0, lastSpace + 1)
            val lastWord = trimmed.substring(lastSpace + 1)
            prefix + autoCorrectWord(lastWord)
        }
    }

    fun getPredictionsAndCompletions(text: String, isEnglishMode: Boolean): List<String> {
        if (isEnglishMode) {
            val trimmed = text.trimEnd()
            if (trimmed.isEmpty()) return commonEnglishWords
            val lastWord = trimmed.substringAfterLast(' ').lowercase()
            if (lastWord.isEmpty()) return commonEnglishWords
            val matches = commonEnglishWords.filter { it.lowercase().startsWith(lastWord) }
            val corrected = banglaAutoCorrections[lastWord]
            return (listOfNotNull(corrected) + matches + commonEnglishWords).distinct().take(8)
        }

        if (text.isBlank()) {
            return commonBanglaWords.take(8)
        }

        val endsWithSpace = text.endsWith(" ")
        val words = text.trim().split(Regex("\\s+"))
        val lastWord = words.lastOrNull().orEmpty()

        if (endsWithSpace) {
            val nextPreds = nextWordPredictionsBn[lastWord].orEmpty()
            return (nextPreds + commonBanglaWords).distinct().take(8)
        }

        val autoFixed = banglaAutoCorrections[lastWord]
        val prefixCompletions = commonBanglaWords.filter { it.startsWith(lastWord) && it != lastWord }
        val nextPreds = nextWordPredictionsBn[lastWord].orEmpty()

        return (listOfNotNull(autoFixed) + prefixCompletions + nextPreds + commonBanglaWords)
            .distinct()
            .take(8)
    }
}

/**
 * Built-in Smart Avro-style Phonetic Engine (English to Bengali Transliteration + Dictionary)
 */
object AvroPhoneticEngine {
    private val dictionary = mapOf(
        "ami" to listOf("আমি", "আমাদের", "আমাকে"),
        "amake" to listOf("আমাকে", "আমার"),
        "amar" to listOf("আমার", "আমাদের"),
        "tumi" to listOf("তুমি", "তোমার", "তোমাকে"),
        "tomar" to listOf("তোমার", "তোমাদের"),
        "tomake" to listOf("তোমাকে", "তোমার"),
        "apni" to listOf("আপনি", "আপনার", "আপনাকে"),
        "apnar" to listOf("আপনার", "আপনি"),
        "suraiya" to listOf("সুরাইয়া", "সুরাইয়া", "সুরাইয়া আপু"),
        "meherab" to listOf("মেহেরাব", "মেহেরাব ভাই"),
        "kemon" to listOf("কেমন", "কেমন আছো?"),
        "acho" to listOf("আছো", "আছো?", "আছেন"),
        "achi" to listOf("আছি", "ভালো আছি"),
        "bhalo" to listOf("ভালো", "ভালোবাসি", "ভালো আছি"),
        "valo" to listOf("ভালো", "ভালোবাসি", "ভালো আছি"),
        "valobashi" to listOf("ভালোবাসি", "অনেক ভালোবাসি"),
        "bhalobashi" to listOf("ভালোবাসি", "অনেক ভালোবাসি"),
        "ki" to listOf("কী", "কি", "কী করছো?"),
        "kivabe" to listOf("কীভাবে", "কীভাবে করব?"),
        "korcho" to listOf("করছো", "করছো?"),
        "koro" to listOf("করো", "করুন"),
        "bolo" to listOf("বলো", "বলুন"),
        "bolun" to listOf("বলুন", "বলো"),
        "gan" to listOf("গান", "গান শোনাও"),
        "gaan" to listOf("গান", "গান শোনাও"),
        "kobita" to listOf("কবিতা", "কবিতা লেখো"),
        "chobi" to listOf("ছবি", "ছবি তৈরি করো"),
        "video" to listOf("ভিডিও", "সিনেমাটিক ভিডিও"),
        "golpo" to listOf("গল্প", "গল্প বলো"),
        "mon" to listOf("মন", "মনের কথা"),
        "sundor" to listOf("সুন্দর", "খুব সুন্দর"),
        "dhonnobad" to listOf("ধন্যবাদ", "অনেক ধন্যবাদ"),
        "priyo" to listOf("প্রিয়", "প্রিয়তমা"),
        "bondhu" to listOf("বন্ধু", "প্রিয় বন্ধু"),
        "aj" to listOf("আজ", "আজকের"),
        "ekta" to listOf("একটা", "একটি"),
        "dao" to listOf("দাও", "করে দাও"),
        "likho" to listOf("লেখো", "লিখে দাও"),
        "shonao" to listOf("শোনাও", "গেয়ে শোনাও"),
        "hello" to listOf("হ্যালো", "হ্যালো সুরাইয়া"),
        "hi" to listOf("হাই", "হ্যালো"),
        "kotha" to listOf("কথা", "কথা বলো"),
        "help" to listOf("সাহায্য", "সাহায্য করো"),
        "nam" to listOf("নাম", "তোমার নাম কী?")
    )

    fun singleKeyHint(key: String): String = when (key.lowercase()) {
        "a" -> "আ/া"
        "b" -> "ব"
        "c" -> "চ"
        "d" -> "দ/ড"
        "e" -> "এ/ে"
        "f" -> "ফ"
        "g" -> "গ"
        "h" -> "হ"
        "i" -> "ই/ি"
        "j" -> "জ"
        "k" -> "ক"
        "l" -> "ল"
        "m" -> "ম"
        "n" -> "ন"
        "o" -> "অ/ো"
        "p" -> "প"
        "q" -> "ক"
        "r" -> "র"
        "s" -> "স/শ"
        "t" -> "ত/ট"
        "u" -> "উ/ু"
        "v" -> "ভ"
        "w" -> "ও"
        "x" -> "ক্স"
        "y" -> "য়"
        "z" -> "য"
        else -> ""
    }

    fun getSuggestions(rawInput: String): List<String> {
        val clean = rawInput.trim().lowercase()
        if (clean.isEmpty()) return emptyList()

        val exactMatches = dictionary[clean].orEmpty()
        val prefixMatches = dictionary.entries
            .filter { it.key.startsWith(clean) && it.key != clean }
            .flatMap { it.value }
            .distinct()
            .take(4)

        val ruleConverted = transliterateWord(clean)
        return (exactMatches + listOf(ruleConverted) + prefixMatches).distinct().take(6)
    }

    fun transliterateWord(input: String): String {
        val clean = input.trim().lowercase()
        if (clean.isEmpty()) return ""
        dictionary[clean]?.firstOrNull()?.let { return it }

        val out = StringBuilder()
        var i = 0
        var prevWasConsonant = false

        fun isVowelChar(c: Char): Boolean = c in "aeiou"

        while (i < clean.length) {
            val rem = clean.substring(i)

            val consonantMatch: Pair<Int, String>? = when {
                rem.startsWith("ksh") -> 3 to "ক্ষ"
                rem.startsWith("chh") -> 3 to "ছ"
                rem.startsWith("ng") -> 2 to "ং"
                rem.startsWith("kh") -> 2 to "খ"
                rem.startsWith("gh") -> 2 to "ঘ"
                rem.startsWith("ch") -> 2 to "চ"
                rem.startsWith("jh") -> 2 to "ঝ"
                rem.startsWith("th") -> 2 to "থ"
                rem.startsWith("dh") -> 2 to "ধ"
                rem.startsWith("ph") -> 2 to "ফ"
                rem.startsWith("bh") -> 2 to "ভ"
                rem.startsWith("sh") -> 2 to "শ"
                else -> null
            }

            if (consonantMatch != null) {
                out.append(consonantMatch.second)
                i += consonantMatch.first
                prevWasConsonant = consonantMatch.second != "ং"
                continue
            }

            val multiVowel: Pair<Int, Pair<String, String>>? = when {
                rem.startsWith("oi") -> 2 to ("ঐ" to "ৈ")
                rem.startsWith("ou") -> 2 to ("ঔ" to "ৌ")
                rem.startsWith("ee") -> 2 to ("ঈ" to "ী")
                rem.startsWith("oo") -> 2 to ("উ" to "ু")
                else -> null
            }

            if (multiVowel != null) {
                val (standalone, kar) = multiVowel.second
                out.append(if (prevWasConsonant) kar else standalone)
                i += multiVowel.first
                prevWasConsonant = false
                continue
            }

            val ch = clean[i]
            if (isVowelChar(ch)) {
                if (prevWasConsonant) {
                    val kar = when (ch) {
                        'a' -> "া"
                        'i' -> "ি"
                        'u' -> "ু"
                        'e' -> "ে"
                        'o' -> "ো"
                        else -> ""
                    }
                    out.append(kar)
                } else {
                    val v = when (ch) {
                        'a' -> "আ"
                        'i' -> "ই"
                        'u' -> "উ"
                        'e' -> "এ"
                        'o' -> "অ"
                        else -> ""
                    }
                    out.append(v)
                }
                prevWasConsonant = false
                i++
            } else {
                val cStr = when (ch) {
                    'b' -> "ব"
                    'c' -> "চ"
                    'd' -> "দ"
                    'f' -> "ফ"
                    'g' -> "গ"
                    'h' -> "হ"
                    'j' -> "জ"
                    'k' -> "ক"
                    'l' -> "ল"
                    'm' -> "ম"
                    'n' -> "ন"
                    'p' -> "প"
                    'q' -> "ক"
                    'r' -> "র"
                    's' -> "স"
                    't' -> "ত"
                    'v' -> "ভ"
                    'w' -> "ও"
                    'x' -> "ক্স"
                    'y' -> "য়"
                    'z' -> "য"
                    else -> ch.toString()
                }
                out.append(cStr)
                prevWasConsonant = ch in 'a'..'z' && ch != 'w'
                i++
            }
        }
        return out.toString()
    }
}
