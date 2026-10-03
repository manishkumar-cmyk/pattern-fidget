package com.fidget.patternlock

import android.app.Activity
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.NumberFormat
import kotlin.math.min
import kotlin.random.Random

class MainActivity : Activity() {

    private enum class Screen { HOME, DRAW, MEMORY, SAVED, REPLAY, SETTINGS }

    private lateinit var prefs: SharedPreferences
    private lateinit var store: PatternStore
    private lateinit var feedback: Feedback
    private lateinit var container: FrameLayout
    private val handler = Handler(Looper.getMainLooper())

    private var screen = Screen.HOME
    private var onLeave: (() -> Unit)? = null
    private var replayIndex = 0

    private var themeIndex = 0
    private var gridSize = 3
    private var totalDots = 0L

    private val t get() = Themes.all[themeIndex]

    private val faceLight: Typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    private val faceRegular: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    private val faceMedium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("fidget", MODE_PRIVATE)
        store = PatternStore(prefs)
        val pack = SoundPack.values().getOrElse(prefs.getInt("pack", 0)) { SoundPack.CHIME }
        feedback = Feedback(this, pack).apply {
            soundOn = prefs.getBoolean("sound", true)
            hapticsOn = prefs.getBoolean("haptics", true)
        }
        totalDots = prefs.getLong("totalDots", 0L)
        themeIndex = prefs.getInt("theme", 0).coerceIn(0, Themes.all.lastIndex)
        gridSize = prefs.getInt("grid", 3).coerceIn(3, 5)

        container = FrameLayout(this)
        setContentView(container)
        applyWindowColors()
        show(Screen.HOME)
    }

    // ---------------------------------------------------------------- navigation

    private fun show(s: Screen) {
        onLeave?.invoke()
        onLeave = null
        handler.removeCallbacksAndMessages(null)
        screen = s
        val v = when (s) {
            Screen.HOME -> buildHome()
            Screen.DRAW -> buildDraw()
            Screen.MEMORY -> buildMemory()
            Screen.SAVED -> buildSaved()
            Screen.REPLAY -> buildReplay()
            Screen.SETTINGS -> buildSettings()
        }
        container.removeAllViews()
        container.addView(v, FrameLayout.LayoutParams(MATCH, MATCH))
        v.alpha = 0f
        v.animate().alpha(1f).setDuration(220).start()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when (screen) {
            Screen.HOME -> @Suppress("DEPRECATION") super.onBackPressed()
            Screen.REPLAY -> show(Screen.SAVED)
            else -> show(Screen.HOME)
        }
    }

    // ---------------------------------------------------------------- landing page

    private fun buildHome(): View {
        val hero = PatternView(this).apply {
            gridSize = 3
            theme = t
            interactive = false
            autoFade = false
        }
        // The landing page draws calm patterns on its own, so you see what the app does before touching it.
        fun loop() {
            hero.play(Patterns.random(3, Random.nextInt(4, 8)), 300L) {
                hero.fadeOut(900L, 900L) { handler.postDelayed({ loop() }, 400L) }
            }
        }
        handler.postDelayed({ loop() }, 500L)
        onLeave = { hero.stopPlayback() }

        val col = vertical().apply { setPadding(dp(32), dp(40), dp(32), dp(28)) }
        col.addView(hero, LinearLayout.LayoutParams(MATCH, 0, 1f))
        col.addView(label("Pattern Fidget", 38f, t.text, faceLight), lp(top = 4))
        col.addView(label("${fmt(totalDots)} dots connected", 15f, t.muted), lp(top = 2))

        val best = prefs.getInt("best$gridSize", 0)
        val savedCount = store.load().size
        col.addView(menuRow("Free draw", "Draw anything. Nothing unlocks.") { show(Screen.DRAW) }, rowLp(top = 24))
        col.addView(menuRow("Memory",
            if (best > 0) "Watch a pattern, then draw it back. Best: level $best"
            else "Watch a pattern, then draw it back.") { show(Screen.MEMORY) }, rowLp(top = 2))
        col.addView(menuRow("Saved patterns",
            when (savedCount) { 0 -> "Keep the ones you like from free draw."; 1 -> "1 pattern"; else -> "$savedCount patterns" }
        ) { show(Screen.SAVED) }, rowLp(top = 2))

        val settings = label("Settings", 15f, t.muted).apply {
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = ripple(dp(18).toFloat())
            setOnClickListener { show(Screen.SETTINGS) }
        }
        col.addView(settings, LinearLayout.LayoutParams(WRAP, WRAP).apply {
            topMargin = dp(14); marginStart = -dp(12)
        })
        return col
    }

    private fun menuRow(title: String, sub: String, onClick: () -> Unit): View = vertical().apply {
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = ripple(dp(16).toFloat())
        setOnClickListener { onClick() }
        addView(label(title, 20f, t.text))
        addView(label(sub, 14f, t.muted), lp(top = 2))
    }

    // ---------------------------------------------------------------- free draw

    private fun buildDraw(): View {
        var lastPattern: List<Int>? = null
        val pv = PatternView(this).apply { gridSize = this@MainActivity.gridSize; theme = t }

        val save = pill("Save pattern") {}
        fun setSave(enabled: Boolean, text: String) {
            save.text = text
            save.isEnabled = enabled
            save.animate().alpha(if (enabled) 1f else 0.4f).setDuration(200).start()
        }
        setSave(false, "Save pattern")
        save.setOnClickListener {
            val p = lastPattern ?: return@setOnClickListener
            store.add(SavedPattern(pv.gridSize, p))
            lastPattern = null
            setSave(false, "Saved")
        }

        pv.listener = object : PatternView.Listener {
            override fun onDotAdded(index: Int, countInPattern: Int, fromPlayback: Boolean) {
                feedback.onDot(countInPattern)
                totalDots++
            }
            override fun onPatternReleased(pattern: List<Int>) {
                feedback.onRelease(pattern.size)
                persistCount()
                if (pattern.size >= 2) {
                    lastPattern = pattern
                    setSave(true, "Save pattern")
                }
            }
        }

        val gridToggle = label("${gridSize}×$gridSize", 15f, t.muted).apply {
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = ripple(dp(18).toFloat())
            contentDescription = "Grid size"
            setOnClickListener {
                setGrid(if (gridSize >= 5) 3 else gridSize + 1)
                pv.gridSize = gridSize
                text = "${gridSize}×$gridSize"
                lastPattern = null
                setSave(false, "Save pattern")
            }
        }

        return vertical().apply {
            addView(topBar("Free draw", gridToggle))
            addView(pv, LinearLayout.LayoutParams(MATCH, 0, 1f))
            addView(save, LinearLayout.LayoutParams(WRAP, WRAP).apply {
                gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(44)
            })
        }
    }

    // ---------------------------------------------------------------- memory

    private fun buildMemory(): View {
        val n = gridSize
        val pv = PatternView(this).apply {
            gridSize = n; theme = t; autoFade = false; interactive = false
        }
        val levelText = label("Level 1", 30f, t.text, faceLight).apply { gravity = Gravity.CENTER }
        val hint = label("Watch the pattern, then draw it back.", 15f, t.muted).apply { gravity = Gravity.CENTER }
        val bestText = label("", 15f, t.muted).apply { setPadding(dp(14), dp(10), dp(14), dp(10)) }
        val action = pill("Start") {}

        var level = 1
        var target = emptyList<Int>()
        var state = "idle"   // idle, showing, input, correct, over

        fun best() = prefs.getInt("best$n", 0)
        fun refreshBest() { bestText.text = if (best() > 0) "Best ${best()}" else "" }
        refreshBest()

        fun startRound() {
            state = "showing"
            handler.removeCallbacksAndMessages(null)
            pv.cancelAnimations()
            pv.interactive = false
            pv.tint = null
            pv.clearPattern()
            levelText.text = "Level $level"
            hint.text = "Watch"
            target = Patterns.random(n, min(2 + level, n * n))
            handler.postDelayed({
                pv.play(target, 480L) {
                    handler.postDelayed({
                        pv.fadeOut(0L, 350L) {
                            state = "input"
                            pv.interactive = true
                            hint.text = "Your turn"
                        }
                    }, 450L)
                }
            }, 500L)
        }

        action.setOnClickListener {
            action.visibility = View.INVISIBLE
            level = 1
            startRound()
        }

        pv.listener = object : PatternView.Listener {
            override fun onDotAdded(index: Int, countInPattern: Int, fromPlayback: Boolean) {
                if (fromPlayback) {
                    if (state == "showing") feedback.onDot(countInPattern)
                } else {
                    feedback.onDot(countInPattern)
                    totalDots++
                }
            }

            override fun onPatternReleased(pattern: List<Int>) {
                if (state != "input") return
                persistCount()
                if (pattern.size < 2) { pv.clearPattern(); return }   // a stray tap doesn't count
                pv.interactive = false
                if (pattern == target) {
                    state = "correct"
                    feedback.onSuccess()
                    hint.text = "Correct"
                    if (level > best()) { prefs.edit().putInt("best$n", level).apply(); refreshBest() }
                    level++
                    pv.fadeOut(600L, 450L) { startRound() }
                } else {
                    state = "over"
                    feedback.onFail()
                    pv.tint = t.error
                    hint.text = "Not quite. Here it is again."
                    action.text = "Try again"
                    action.visibility = View.VISIBLE
                    // Show the pattern that was asked for.
                    handler.postDelayed({
                        pv.fadeOut(0L, 350L) { pv.play(target, 260L) }
                    }, 1000L)
                }
            }
        }

        onLeave = { pv.cancelAnimations() }

        return vertical().apply {
            addView(topBar("Memory", bestText))
            addView(levelText, lp(top = 8).apply { gravity = Gravity.CENTER_HORIZONTAL })
            addView(hint, LinearLayout.LayoutParams(MATCH, WRAP).apply {
                topMargin = dp(4); marginStart = dp(32); marginEnd = dp(32)
            })
            addView(pv, LinearLayout.LayoutParams(MATCH, 0, 1f))
            addView(action, LinearLayout.LayoutParams(WRAP, WRAP).apply {
                gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(44)
            })
        }
    }

    // ---------------------------------------------------------------- saved patterns

    private fun buildSaved(): View {
        val saved = store.load()
        val root = vertical()
        root.addView(topBar("Saved patterns", null))

        if (saved.isEmpty()) {
            val empty = vertical().apply {
                gravity = Gravity.CENTER
                setPadding(dp(40), 0, dp(40), dp(80))
                addView(label("Nothing saved yet", 22f, t.text, faceLight).apply { gravity = Gravity.CENTER })
                addView(label("Draw something in Free draw, then tap Save pattern.", 15f, t.muted)
                    .apply { gravity = Gravity.CENTER }, lp(top = 6))
                addView(pill("Open free draw") { show(Screen.DRAW) }, lp(top = 24))
            }
            root.addView(empty, LinearLayout.LayoutParams(MATCH, 0, 1f))
            return root
        }

        val list = vertical().apply { setPadding(dp(20), dp(4), dp(20), dp(40)) }
        val cols = 3
        for (rowStart in saved.indices step cols) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (k in 0 until cols) {
                val i = rowStart + k
                val cell = FrameLayout(this)
                if (i < saved.size) {
                    val thumb = PatternThumb(this, saved[i].n, saved[i].dots, t).apply {
                        contentDescription = "Saved pattern ${i + 1}"
                        foreground = ripple(dp(14).toFloat())
                        setOnClickListener { replayIndex = i; show(Screen.REPLAY) }
                    }
                    cell.addView(thumb, FrameLayout.LayoutParams(MATCH, WRAP))
                }
                row.addView(cell, LinearLayout.LayoutParams(0, WRAP, 1f).apply { setMargins(dp(6), dp(6), dp(6), dp(6)) })
            }
            list.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        val scroll = ScrollView(this).apply { addView(list); isVerticalScrollBarEnabled = false }
        root.addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))
        return root
    }

    private fun buildReplay(): View {
        val saved = store.load()
        val sp = saved.getOrNull(replayIndex) ?: return buildSaved().also { screen = Screen.SAVED }
        val pv = PatternView(this).apply {
            gridSize = sp.n; theme = t; autoFade = false; interactive = false
            listener = object : PatternView.Listener {
                override fun onDotAdded(index: Int, countInPattern: Int, fromPlayback: Boolean) = feedback.onDot(countInPattern)
                override fun onPatternReleased(pattern: List<Int>) {}
            }
        }
        fun replay() = pv.play(sp.dots, 380L) { feedback.onRelease(sp.dots.size) }
        handler.postDelayed({ replay() }, 350L)
        onLeave = { pv.stopPlayback() }

        val delete = label("Delete", 15f, t.muted).apply {
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = ripple(dp(20).toFloat())
            setOnClickListener {
                store.removeAt(replayIndex)
                show(Screen.SAVED)
            }
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(pill("Play again") { replay() })
            addView(delete, LinearLayout.LayoutParams(WRAP, WRAP).apply { marginStart = dp(12) })
        }
        return vertical().apply {
            addView(topBar("Saved pattern", label("${sp.n}×${sp.n}", 15f, t.muted).apply {
                setPadding(dp(14), dp(10), dp(14), dp(10))
            }))
            addView(pv, LinearLayout.LayoutParams(MATCH, 0, 1f))
            addView(buttons, LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(44) })
        }
    }

    // ---------------------------------------------------------------- settings

    private fun buildSettings(): View {
        val col = vertical().apply { setPadding(dp(32), dp(8), dp(32), dp(40)) }

        col.addView(label("Theme", 17f, t.text), lp(top = 8))
        val swatches = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        Themes.all.forEachIndexed { i, th ->
            swatches.addView(SwatchView(this, th, i == themeIndex, t.text).apply {
                background = ripple(dp(22).toFloat())
                setOnClickListener {
                    if (i == themeIndex) return@setOnClickListener
                    themeIndex = i
                    prefs.edit().putInt("theme", i).apply()
                    applyWindowColors()
                    show(Screen.SETTINGS)
                }
            }, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(10) })
        }
        col.addView(swatches, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(10); marginStart = -dp(2) })
        col.addView(label(t.name, 14f, t.muted), lp(top = 6))

        col.addView(settingRow("Grid size", { "${gridSize}×$gridSize" }) {
            setGrid(if (gridSize >= 5) 3 else gridSize + 1)
        }, rowLp(top = 20))

        lateinit var packRow: View
        col.addView(settingRow("Sounds", { if (feedback.soundOn) "On" else "Off" }) {
            feedback.soundOn = !feedback.soundOn
            prefs.edit().putBoolean("sound", feedback.soundOn).apply()
            packRow.alpha = if (feedback.soundOn) 1f else 0.4f
        }, rowLp())
        packRow = settingRow("Sound pack", { feedback.pack.label }) {
            if (!feedback.soundOn) return@settingRow
            val packs = SoundPack.values()
            feedback.pack = packs[(feedback.pack.ordinal + 1) % packs.size]
            prefs.edit().putInt("pack", feedback.pack.ordinal).apply()
            feedback.preview()
        }
        packRow.alpha = if (feedback.soundOn) 1f else 0.4f
        col.addView(packRow, rowLp())
        col.addView(settingRow("Vibration", { if (feedback.hapticsOn) "On" else "Off" }) {
            feedback.hapticsOn = !feedback.hapticsOn
            prefs.edit().putBoolean("haptics", feedback.hapticsOn).apply()
        }, rowLp())

        val scroll = ScrollView(this).apply { addView(col); isVerticalScrollBarEnabled = false }
        return vertical().apply {
            addView(topBar("Settings", null))
            addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))
        }
    }

    /** A full-width row: name on the left, current value on the right. Tapping changes the value. */
    private fun settingRow(name: String, value: () -> String, onTap: () -> Unit): View {
        val valueText = label(value(), 17f, t.muted)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(16), dp(12), dp(16))
            background = ripple(dp(14).toFloat())
            addView(label(name, 17f, t.text), LinearLayout.LayoutParams(0, WRAP, 1f))
            addView(valueText)
            setOnClickListener { onTap(); valueText.text = value() }
        }
    }

    // ---------------------------------------------------------------- building blocks

    private fun topBar(title: String, right: View?): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(36), dp(18), dp(4))
        addView(BackIcon(this@MainActivity, t.text).apply {
            background = ripple(dp(22).toFloat())
            setOnClickListener { @Suppress("DEPRECATION") onBackPressed() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
        addView(label(title, 20f, t.text), LinearLayout.LayoutParams(0, WRAP, 1f).apply { marginStart = dp(6) })
        if (right != null) addView(right)
    }

    private fun label(text: String, size: Float, color: Int, face: Typeface = faceRegular) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        typeface = face
    }

    /** The one filled control style: a quiet surface-colored pill. */
    private fun pill(text: String, onClick: () -> Unit) = label(text, 16f, t.text, faceMedium).apply {
        gravity = Gravity.CENTER
        minWidth = dp(150)
        setPadding(dp(28), dp(14), dp(28), dp(14))
        val shape = GradientDrawable().apply { cornerRadius = dp(26).toFloat(); setColor(t.surface) }
        background = RippleDrawable(ColorStateList.valueOf(withAlpha(t.dot, 0x80)), shape, null)
        setOnClickListener { onClick() }
    }

    private fun ripple(radius: Float): Drawable {
        val mask = GradientDrawable().apply { cornerRadius = radius; setColor(Color.WHITE) }
        return RippleDrawable(ColorStateList.valueOf(withAlpha(t.dot, 0x66)), null, mask)
    }

    private fun withAlpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a shl 24)

    private fun vertical() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

    /** Full-width row whose ripple bleeds 12dp past the text column, so the text itself stays aligned. */
    private fun rowLp(top: Int = 0) = LinearLayout.LayoutParams(MATCH, WRAP).apply {
        topMargin = dp(top); marginStart = -dp(12); marginEnd = -dp(12)
    }

    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(top) }

    private fun setGrid(n: Int) {
        gridSize = n
        prefs.edit().putInt("grid", n).apply()
    }

    private fun persistCount() = prefs.edit().putLong("totalDots", totalDots).apply()

    private fun fmt(v: Long) = NumberFormat.getIntegerInstance().format(v)

    private fun applyWindowColors() {
        container.setBackgroundColor(t.background)
        window.decorView.setBackgroundColor(t.background)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        }
    }

    override fun onPause() {
        super.onPause()
        persistCount()
    }

    override fun onDestroy() {
        super.onDestroy()
        onLeave?.invoke()
        handler.removeCallbacksAndMessages(null)
        feedback.shutdown()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
