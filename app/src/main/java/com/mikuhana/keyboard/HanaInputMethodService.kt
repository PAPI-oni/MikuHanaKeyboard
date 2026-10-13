package com.mikuhana.keyboard

import android.inputmethodservice.InputMethodService
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.Intent

class HanaInputMethodService : InputMethodService() {
    private lateinit var root: LinearLayout
    private lateinit var suggestionRow: LinearLayout
    private lateinit var emojiRow1: LinearLayout
    private lateinit var emojiRow2: LinearLayout
    private var numbersMode = false

    override fun onCreateInputView(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(7), dp(7), dp(7), dp(8))
            background = rounded("#FFF0F7", "#E9B5D0", 22)
        }
        suggestionRow = row()
        emojiRow1 = row()
        emojiRow2 = row()
        root.addView(suggestionRow, rowParams(42))
        root.addView(emojiRow1, rowParams(48))
        root.addView(emojiRow2, rowParams(48))
        refreshSuggestions()
        refreshEmojis()
        if (numbersMode) addNumberKeys() else addLetterKeys()
        addBottomKeys()
        return root
    }

    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }

    private fun rowParams(h: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, dp(h)
    ).apply { bottomMargin = dp(3) }

    private fun rounded(fill: String, stroke: String, radius: Int) =
        GradientDrawable().apply {
            setColor(Color.parseColor(fill))
            setStroke(dp(1), Color.parseColor(stroke))
            cornerRadius = dp(radius).toFloat()
        }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun key(label: String, onClick: () -> Unit, weight: Float = 1f,
                    fill: String = "#FFFFFF", textColor: String = "#713A68",
                    height: Int = 48): TextView {
        val b = TextView(this).apply {
            text = label
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(textColor))
            typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
            background = rounded(fill, "#E7B4D0", 13)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            setPadding(dp(2), 0, dp(2), 0)
        }
        val lp = LinearLayout.LayoutParams(0, dp(height), weight).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }
        b.layoutParams = lp
        return b
    }

    private fun addToRow(r: LinearLayout, label: String, action: () -> Unit,
                         fill: String = "#FFFFFF", weight: Float = 1f, size: Float = 20f) {
        val t = key(label, action, weight, fill)
        t.textSize = size
        r.addView(t)
    }

    private fun refreshSuggestions() {
        suggestionRow.removeAllViews()
        val prefs = getSharedPreferences("hana", Context.MODE_PRIVATE)
        val custom = prefs.getStringSet("phrases", emptySet())?.toList()?.take(3) ?: emptyList()
        val current = currentWord()
        val learned = prefs.getStringSet("learned", emptySet())?.toList()
            ?.filter { current.isNotBlank() && it.startsWith(current) }?.take(3) ?: emptyList()
        val suggestions = (custom + learned).distinct().take(4)
        if (suggestions.isEmpty()) {
            addToRow(suggestionRow, "🎀", { openSettings() }, "#FFE0EF", size = 17f)
            addToRow(suggestionRow, "واژه‌ها +", { openSettings() }, "#FFE0EF", size = 13f)
            addToRow(suggestionRow, "🌸", { commit("🌸") }, "#FFE0EF", size = 17f)
            addToRow(suggestionRow, "⚙️", { openSettings() }, "#FFE0EF", size = 17f)
        } else {
            suggestions.forEach { word ->
                addToRow(suggestionRow, word, { commit(word) }, "#FFE0EF", size = 13f)
            }
            addToRow(suggestionRow, "⚙️", { openSettings() }, "#FFE0EF", size = 16f)
        }
    }

    private fun currentWord(): String {
        return try {
            currentInputConnection?.getTextBeforeCursor(40, 0)?.toString()
                ?.takeLastWhile { !it.isWhitespace() && it !in ".,!?،؛:()[]{}" } ?: ""
        } catch (_: Exception) { "" }
    }

    private fun refreshEmojis() {
        emojiRow1.removeAllViews()
        emojiRow2.removeAllViews()
        val recent = getSharedPreferences("hana", MODE_PRIVATE)
            .getString("recent", "😂,🎀,🩷,😭,✨,🥺,💕,🐈,🫧,🌸,💖,🦋")!!
            .split(",").filter { it.isNotBlank() }.distinct()
        recent.take(6).forEach { e ->
            addToRow(emojiRow1, e, { commitEmoji(e) }, "#FFFFFF", size = 23f)
        }
        recent.drop(6).take(6).forEach { e ->
            addToRow(emojiRow2, e, { commitEmoji(e) }, "#FFFFFF", size = 23f)
        }
    }

    private fun commitEmoji(emoji: String) {
        commit(emoji)
        val prefs = getSharedPreferences("hana", MODE_PRIVATE)
        val old = prefs.getString("recent", "")!!.split(",").filter { it.isNotBlank() }
        prefs.edit().putString("recent", (listOf(emoji) + old.filter { it != emoji }).take(12).joinToString(",")).apply()
        refreshEmojis()
    }

    private fun addLetterKeys() {
        val r = row()
        listOf("ه", "ا", "پ").forEach { letter ->
            addToRow(r, letter, {
                currentInputConnection?.commitText(letter, 1)
                refreshSuggestions()
            }, "#FFD4E8", weight = 1f, size = 27f)
        }
        root.addView(r, rowParams(54))
    }

    private fun addNumberKeys() {
        listOf("۱ ۲ ۳", "۴ ۵ ۶", "۷ ۸ ۹", "۰ ، ؟").forEach { group ->
            val r = row()
            group.split(" ").forEach { digit ->
                addToRow(r, digit, { commit(digit) }, "#FFD4E8", size = 23f)
            }
            root.addView(r, rowParams(43))
        }
    }

    private fun addBottomKeys() {
        val r = row()
        addToRow(r, "🌐", { numbersMode = !numbersMode; setInputView(onCreateInputView()) }, "#E7DFFF", size = 20f)
        addToRow(r, "فاصله", { currentInputConnection?.commitText(" ", 1); refreshSuggestions() }, "#FFFFFF", weight = 1.7f, size = 14f)
        addToRow(r, "⌫", {
            currentInputConnection?.deleteSurroundingText(1, 0)
            refreshSuggestions()
        }, "#FFE0EF", size = 23f)
        addToRow(r, "↵", {
            val ic = currentInputConnection
            if (ic != null) ic.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE)
            else ic?.commitText("\n", 1)
        }, "#E7DFFF", size = 21f)
        root.addView(r, rowParams(46))
        val footer = row()
        addToRow(footer, "♡ MIKU HANA ♡", { openSettings() }, "#F8D5E8", weight = 1f, size = 12f)
        root.addView(footer, rowParams(27))
    }

    private fun commit(text: String) {
        currentInputConnection?.commitText(text, 1)
        learnWord(text)
        refreshSuggestions()
    }

    private fun learnWord(text: String) {
        if (text.length < 2 || text.any { it.isWhitespace() }) return
        val prefs = getSharedPreferences("hana", MODE_PRIVATE)
        val words = prefs.getStringSet("learned", emptySet())?.toMutableSet() ?: mutableSetOf()
        words.add(text)
        prefs.edit().putStringSet("learned", words.takeLast(300).toSet()).apply()
    }

    private fun openSettings() {
        val i = Intent(this, SettingsActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(i)
    }
}
