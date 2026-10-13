package com.mikuhana.keyboard

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*
import android.view.ViewGroup

class SettingsActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("hana", Context.MODE_PRIVATE) }
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            background = bg("#FFF0F7", "#E7B4D0", 24)
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(label("🎀 Miku Hana Keyboard 🎀", 25, "#9D4EDD"))
        root.addView(label("کیبورد انیمه‌ای صورتی تو ♡", 16, "#713A68"))
        root.addView(label("کلمه یا جملهٔ آماده اضافه کن؛ در نوار پیشنهادها نمایش داده می‌شود.", 15, "#713A68"))

        val input = EditText(this).apply {
            hint = "مثلاً: سلام عزیزم خوبی؟ 🩷"
            textSize = 16f
            setTextColor(Color.parseColor("#713A68"))
            background = bg("#FFFFFF", "#E7B4D0", 14)
            setPadding(18, 14, 18, 14)
        }
        root.addView(input, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 16; bottomMargin = 10
        })

        val add = button("＋ افزودن کلمه / جمله")
        root.addView(add)
        add.setOnClickListener {
            val value = input.text.toString().trim()
            if (value.isNotEmpty()) {
                val phrases = prefs.getStringSet("phrases", emptySet())?.toMutableSet() ?: mutableSetOf()
                phrases.add(value)
                prefs.edit().putStringSet("phrases", phrases).apply()
                input.text.clear()
                renderSaved()
                Toast.makeText(this, "ذخیره شد ♡", Toast.LENGTH_SHORT).show()
            }
        }

        root.addView(label("کلمات و جمله‌های ذخیره‌شده", 19, "#9D4EDD"))
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        renderSaved()

        root.addView(label("فعالسازی کیبورد", 19, "#9D4EDD"))
        root.addView(button("۱. باز کردن تنظیمات کیبوردهای اندروید").apply {
            setOnClickListener {
                startActivity(android.content.Intent("android.settings.INPUT_METHOD_SETTINGS"))
            }
        })
        root.addView(button("۲. انتخاب Miku Hana Keyboard").apply {
            setOnClickListener {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showInputMethodPicker()
            }
        })
        root.addView(label("حریم خصوصی: این نسخه کلمات سفارشی و ایموجی‌های اخیر را فقط به‌صورت محلی در گوشی نگه می‌دارد. از وارد کردن اطلاعات حساس خودداری کن.", 13, "#713A68"))
    }

    private fun renderSaved() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        val values = prefs.getStringSet("phrases", emptySet())?.toList()?.sorted() ?: emptyList()
        if (values.isEmpty()) list.addView(label("هنوز چیزی اضافه نکردی 🌸", 14, "#713A68"))
        values.forEach { value ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = bg("#FFFFFF", "#E7B4D0", 12)
                setPadding(12, 6, 12, 6)
            }
            val text = TextView(this).apply {
                this.text = value
                textSize = 15f
                setTextColor(Color.parseColor("#713A68"))
                setPadding(4, 8, 4, 8)
            }
            row.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
            val delete = button("حذف")
            delete.setOnClickListener {
                val updated = prefs.getStringSet("phrases", emptySet())?.toMutableSet() ?: mutableSetOf()
                updated.remove(value)
                prefs.edit().putStringSet("phrases", updated).apply()
                renderSaved()
            }
            row.addView(delete)
            list.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 })
        }
    }

    private fun label(value: String, size: Int, color: String) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(Color.parseColor(color))
        typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(8, 12, 8, 12)
    }

    private fun button(value: String) = Button(this).apply {
        text = value
        textSize = 14f
        setTextColor(Color.parseColor("#713A68"))
        background = bg("#FFD4E8", "#E7B4D0", 14)
        isAllCaps = false
        setPadding(10, 6, 10, 6)
    }

    private fun bg(fill: String, stroke: String, radius: Int) = GradientDrawable().apply {
        setColor(Color.parseColor(fill))
        setStroke(2, Color.parseColor(stroke))
        cornerRadius = radius.toFloat()
    }
}
