package com.fidget.patternlock

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputFilter
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import kotlin.random.Random

class MainActivity : Activity() {

    private enum class Screen { HOME, ZEN, MEMORY, COLLECTION, PLAYBACK, SETTINGS }

    private lateinit var prefs: Prefs
    private lateinit var store: PatternStore
    private lateinit var sound: SoundEngine
    private lateinit var haptics: Haptics
    private lateinit var root: FrameLayout
    private lateinit var ui: Ui
    private val handler = Handler(Looper.getMainLooper())

    private var screen = Screen.HOME
    private var onLeave: (() -> Unit)? = null
    private var dismissSheet: (() -> Unit)? = null
    private var playbackId = 0L
    private var playbackLoop = false
    private var collectionFilter = 0
    private var totalDots = 0L

    private val t get() = ui.t
    private val rm get() = prefs.reduceMotion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        store = PatternStore(prefs.raw)
        sound = SoundEngine(this)
        haptics = Haptics(this)
        totalDots = prefs.totalDots
        root = FrameLayout(this)
        setContentView(root)
        ui = Ui(this, currentTheme())
        applyTheme()
        show(Screen.HOME)
    }

    // ---------------------------------------------------------------- theme & senses

    private fun currentTheme() = Themes.adjusted(Themes.all[prefs.themeIndex], prefs.amoled, prefs.highContrast)

    private fun applyTheme() {
        ui.t = currentTheme()
        root.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(t.bgTop, t.bgBottom))
        window.decorView.setBackgroundColor(t.bgTop)
        applySenses()
    }

    private fun soundEnv(): SoundEnv = SoundEnv.values().getOrNull(prefs.soundEnv) ?: Themes.all[prefs.themeIndex].sound

    private fun applySenses() {
        sound.enabled = prefs.soundOn
        sound.volume = prefs.volume
        sound.directional = prefs.directional
        sound.configure(soundEnv(), t.keyRoot, t.minorKey)
        haptics.enabled = prefs.hapticsOn
        haptics.level = prefs.hapticLevel
        haptics.flavor = t.haptic
    }

    private fun configure(pv: PatternView) {
        pv.theme = t
        pv.reduceMotion = rm
        pv.largerDots = prefs.largerDots
        pv.showLines = prefs.showLines
        pv.quietCompletion = prefs.quietCompletion
        pv.highContrast = prefs.highContrast
    }

    // ---------------------------------------------------------------- navigation

    private fun show(s: Screen) {
        onLeave?.invoke()
        onLeave = null
        dismissSheet = null
        handler.removeCallbacksAndMessages(null)
        screen = s
        val v = when (s) {
            Screen.HOME -> buildHome()
            Screen.ZEN -> buildZen()
            Screen.MEMORY -> buildMemory()
            Screen.COLLECTION -> buildCollection()
            Screen.PLAYBACK -> buildPlayback()
            Screen.SETTINGS -> buildSettings()
        }
        root.removeAllViews()
        root.addView(v, FrameLayout.LayoutParams(MATCH, MATCH))
        ui.enter(v, rm)
        hideSystemBars()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        dismissSheet?.let { it(); return }
        when (screen) {
            Screen.HOME -> @Suppress("DEPRECATION") super.onBackPressed()
            Screen.PLAYBACK -> show(Screen.COLLECTION)
            else -> show(Screen.HOME)
        }
    }

    private fun back() = @Suppress("DEPRECATION") onBackPressed()

    // ---------------------------------------------------------------- home: the live canvas

    private fun buildHome(): View {
        val frame = FrameLayout(this)
        val pv = PatternView(this).apply {
            configure(this)
            gridSize = prefs.grid
            mode = prefs.mode
            mirrorFourWay = prefs.mirrorFourWay
            insetTop = ui.dp(104)
            insetBottom = ui.dp(196)
            longPressEnabled = true
            interruptible = true
        }
        frame.addView(pv, FrameLayout.LayoutParams(MATCH, MATCH))

        // Top chrome: wordmark, current mode, modes and settings
        val top = ui.horizontal().apply { setPadding(ui.dp(24), ui.dp(36), ui.dp(8), 0) }
        val titles = ui.vertical()
        titles.addView(ui.text("Pattern Fidget", 20f, Themes.alpha(t.text, 0.75f), ui.regular))
        val modeLabel = ui.text(prefs.mode.label, 14f, t.muted)
        titles.addView(modeLabel)
        top.addView(titles, LinearLayout.LayoutParams(0, WRAP, 1f))
        lateinit var openModes: () -> Unit
        top.addView(ui.icon(IconKind.TUNE, "Modes", t.muted) { openModes() })
        top.addView(ui.icon(IconKind.GEAR, "Settings", t.muted) { show(Screen.SETTINGS) })
        frame.addView(top, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP))

        // Bottom chrome: grid size and the three destinations
        val bottom = ui.vertical().apply { setPadding(ui.dp(20), 0, ui.dp(20), ui.dp(28)); gravity = Gravity.CENTER_HORIZONTAL }
        val gridSeg = ui.segmented(listOf("3×3", "4×4", "5×5"), prefs.grid - 3) { k ->
            prefs.grid = k + 3
            pv.gridSize = k + 3
        }
        bottom.addView(gridSeg, LinearLayout.LayoutParams(ui.dp(264), WRAP).apply { bottomMargin = ui.dp(14) })
        val nav = ui.nav(0, { openModes() }, { show(Screen.MEMORY) }, { show(Screen.COLLECTION) })
        bottom.addView(nav.view, LinearLayout.LayoutParams(MATCH, WRAP))
        frame.addView(bottom, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))

        // Save chip and Loop speed line sit just under the grid, outside the fading chrome.
        val saveChip = ui.chip("Save", false) {}.apply { visibility = View.INVISIBLE }
        frame.addView(saveChip, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = ui.dp(184) })
        val loopLabel = ui.text("", 13f, t.muted).apply { gravity = Gravity.CENTER; visibility = View.GONE }
        frame.addView(loopLabel, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = ui.dp(188) })

        var hint: TextView? = null
        if (prefs.hintCount("touch") < 3) {
            prefs.bumpHint("touch")
            hint = ui.text("Touch to begin", 15f, t.muted).apply { alpha = 0f }
            frame.addView(hint, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = ui.dp(188) })
            hint.animate().alpha(1f).setStartDelay(900).setDuration(800).start()
        }

        // Chrome fades while a finger is on the grid and returns 1.5 s after the last lift.
        val chrome = listOf<View>(top, bottom)
        var hidden = false
        val restore = Runnable {
            hidden = false
            chrome.forEach { it.visibility = View.VISIBLE; it.animate().alpha(1f).setDuration(320).start() }
        }
        fun hideChrome() {
            handler.removeCallbacks(restore)
            hint?.animate()?.alpha(0f)?.setStartDelay(0)?.setDuration(240)?.start()
            hint = null
            if (hidden) return
            hidden = true
            chrome.forEach { v -> v.animate().alpha(0f).setDuration(240).withEndAction { if (hidden) v.visibility = View.INVISIBLE }.start() }
        }

        val hideSave = Runnable { saveChip.animate().alpha(0f).setDuration(300).withEndAction { saveChip.visibility = View.INVISIBLE }.start() }
        fun offerSave(p: List<Int>, n: Int) {
            handler.removeCallbacks(hideSave)
            val first = prefs.hintCount("save") == 0
            saveChip.text = if (first) "Keep this one?" else "Save"
            saveChip.visibility = View.VISIBLE
            saveChip.alpha = 0f
            saveChip.animate().alpha(1f).setDuration(240).start()
            saveChip.setOnClickListener {
                if (first) prefs.bumpHint("save")
                if (store.add(n, p) != null) {
                    haptics.dot(1, 300f)
                    saveChip.text = "Saved"
                    val pill = nav.pills[2]
                    if (!rm) pill.animate().scaleX(1.08f).scaleY(1.08f).setDuration(140).withEndAction {
                        pill.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
                    }.start()
                }
                handler.removeCallbacks(hideSave)
                handler.postDelayed(hideSave, 900)
            }
            handler.postDelayed(hideSave, 3500)
        }

        fun loopText(speed: Float) = "${"%.1f".format(speed)}×  ·  drag up or down for speed  ·  tap to clear"

        var lastTouch = SystemClock.uptimeMillis()
        pv.listener = object : PatternView.Listener {
            override fun onTouchStart() {
                lastTouch = SystemClock.uptimeMillis()
                pv.patternOpacity = 1f
                hideChrome()
                handler.removeCallbacks(hideSave); hideSave.run()
            }
            override fun onTouchEnd() {
                lastTouch = SystemClock.uptimeMillis()
                handler.removeCallbacks(restore)
                handler.postDelayed(restore, 1500)
                if (!pv.isLooping) loopLabel.visibility = View.GONE
            }
            override fun onNear() = haptics.near()
            override fun onDot(index: Int, count: Int, dx: Int, dy: Int, speed: Float, fromPlayback: Boolean) {
                val n = pv.gridSize
                if (fromPlayback) {
                    // The faint idle animation stays silent; loops and double-tap replays are heard.
                    if (pv.isLooping || pv.patternOpacity >= 1f) sound.dot(count, index / n, n, dx, dy, 500f)
                    return
                }
                if (pv.mode == Mode.RIPPLE) sound.dotByPosition(index, n, if (speed > 1200f) 0.45f else 0.65f)
                else {
                    sound.dot(count, index / n, n, dx, dy, speed)
                    if (pv.mode == Mode.MIRROR) sound.harmony(0.28f)
                }
                haptics.dot(count, speed)
                totalDots++
            }
            override fun onRelease(pattern: List<Int>, shape: Shape) {
                persistCount()
                if (shape == Shape.ALL_DOTS) haptics.allDots() else haptics.complete()
                sound.complete(shape == Shape.ALL_DOTS)
                if (pv.mode == Mode.LOOP) {
                    loopLabel.text = loopText(1f)
                    loopLabel.visibility = View.VISIBLE
                }
                offerSave(pattern, pv.gridSize)
            }
            override fun onCycle() { sound.breath(); haptics.breath() }
            override fun onLongPress() { haptics.complete(); show(Screen.ZEN) }
            override fun onLoopSpeed(speed: Float) { loopLabel.text = loopText(speed) }
        }

        openModes = { openModeSheet(pv, gridSeg) { modeLabel.text = pv.mode.label; loopLabel.visibility = View.GONE } }

        // Idle: after a quiet moment the grid traces a faint pattern by itself (a favourite, if one is chosen).
        val idle = object : Runnable {
            override fun run() {
                if (screen != Screen.HOME) return
                val quiet = SystemClock.uptimeMillis() - lastTouch > 6000 && pv.currentPattern.isEmpty() &&
                    !pv.isPlaying && !pv.isLooping && !rm && dismissSheet == null
                if (quiet) {
                    pv.patternOpacity = 0.45f
                    pv.play(idlePattern(pv.gridSize), 380L) {
                        pv.celebrate(false)
                        pv.fadeOut(900L, 1400L) { pv.patternOpacity = 1f }
                    }
                    handler.postDelayed(this, 12000)
                } else handler.postDelayed(this, 2000)
            }
        }
        handler.postDelayed(idle, 3000)
        onLeave = { pv.cancelAnimations() }
        return frame
    }

    private fun idlePattern(n: Int): List<Int> {
        val fav = prefs.homePattern.takeIf { it != 0L }?.let { store.get(it) }
        return if (fav != null && fav.n == n) fav.dots else Patterns.random(n, Random.nextInt(4, minOf(8, n * n)))
    }

    private val modeSamples = mapOf(
        Mode.FREE to listOf(6, 4, 2, 5, 8), Mode.ENDLESS to listOf(0, 1, 2, 5, 4, 3, 6, 7, 8),
        Mode.CONSTELLATION to listOf(0, 4, 2, 7), Mode.RIPPLE to listOf(4, 0, 8), Mode.MIRROR to listOf(0, 3, 7),
        Mode.LOOP to listOf(0, 1, 4, 3), Mode.ZEN to listOf(4))

    private fun openModeSheet(pv: PatternView, gridSeg: View, onChanged: () -> Unit) {
        val c = ui.vertical().apply { setPadding(ui.dp(20), 0, ui.dp(20), 0) }
        c.addView(ui.text("Modes", 18f, t.text, ui.medium), LinearLayout.LayoutParams(WRAP, WRAP).apply { bottomMargin = ui.dp(12); marginStart = ui.dp(4) })
        lateinit var dismiss: () -> Unit
        val modes = Mode.values()
        for (rowStart in modes.indices step 2) {
            val row = ui.horizontal()
            for (k in 0..1) {
                val m = modes.getOrNull(rowStart + k)
                if (m == null) { row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f).apply { marginStart = ui.dp(10) }); continue }
                val on = m == pv.mode
                val style = when (m) {
                    Mode.RIPPLE -> ThumbStyle.RIPPLE; Mode.MIRROR -> ThumbStyle.MIRROR
                    Mode.CONSTELLATION -> ThumbStyle.CONSTELLATION; Mode.ZEN -> ThumbStyle.ZEN
                    else -> ThumbStyle.NORMAL
                }
                val tile = ui.vertical().apply {
                    setPadding(ui.dp(14), ui.dp(10), ui.dp(14), ui.dp(14))
                    val bg = if (on) ui.rounded(Themes.alpha(t.active, 0.16f), ui.dpf(20f), Themes.alpha(t.active, 0.7f), 1.5f)
                    else ui.rounded(ui.surface(0.6f), ui.dpf(20f))
                    background = ui.ripple(ui.dpf(20f), bg)
                    contentDescription = "${m.label}. ${m.blurb}"
                    isSelected = on
                    setOnClickListener {
                        dismiss()
                        if (m == Mode.ZEN) { show(Screen.ZEN); return@setOnClickListener }
                        prefs.mode = m
                        pv.mode = m
                        onChanged()
                    }
                    addView(PatternThumb(this@MainActivity, 3, modeSamples.getValue(m), t, style, !rm, rowStart * 300L + k * 700L, square = false),
                        LinearLayout.LayoutParams(MATCH, ui.dp(64)))
                    addView(ui.text(m.label, 15f, t.text, ui.medium).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO })
                    addView(ui.text(m.blurb, 12f, t.muted).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO })
                }
                row.addView(tile, LinearLayout.LayoutParams(0, MATCH, 1f).apply { if (k == 1) marginStart = ui.dp(10) })
            }
            c.addView(row, LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = ui.dp(10) })
        }
        c.addView(ui.text("Mirror axis", 13f, t.muted), LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = ui.dp(6); marginStart = ui.dp(4) })
        c.addView(ui.segmented(listOf("Vertical", "Four-way"), if (prefs.mirrorFourWay) 1 else 0) { k ->
            prefs.mirrorFourWay = k == 1
            pv.mirrorFourWay = k == 1
        }, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = ui.dp(6) })
        c.addView(ui.text("Grid", 13f, t.muted), LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = ui.dp(14); marginStart = ui.dp(4) })
        c.addView(ui.segmented(listOf("3×3", "4×4", "5×5"), pv.gridSize - 3) { k ->
            prefs.grid = k + 3
            pv.gridSize = k + 3
            dismiss()
            show(Screen.HOME)
        }, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = ui.dp(6) })
        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(c) }
        dismiss = ui.sheet(root, scroll, rm) { dismissSheet = null }
        dismissSheet = dismiss
    }

    // ---------------------------------------------------------------- zen

    private fun buildZen(): View {
        val frame = FrameLayout(this)
        val pv = PatternView(this).apply {
            configure(this)
            gridSize = prefs.grid
            mode = Mode.ENDLESS
            contentDescription = "Zen grid. Two-finger tap to return."
        }
        pv.listener = object : PatternView.Listener {
            override fun onNear() = haptics.near()
            override fun onDot(index: Int, count: Int, dx: Int, dy: Int, speed: Float, fromPlayback: Boolean) {
                sound.dot(count, index / pv.gridSize, pv.gridSize, dx, dy, speed)
                haptics.dot(count, speed)
                totalDots++
            }
            override fun onRelease(pattern: List<Int>, shape: Shape) {
                persistCount()
                if (shape == Shape.ALL_DOTS) haptics.allDots() else haptics.complete()
                sound.complete(shape == Shape.ALL_DOTS)
            }
            override fun onCycle() { sound.breath(); haptics.breath() }
            override fun onTwoFingerTap() = show(Screen.HOME)
        }
        frame.addView(pv, FrameLayout.LayoutParams(MATCH, MATCH))
        if (prefs.hintCount("zen") == 0) {
            prefs.bumpHint("zen")
            val hint = ui.text("Two-finger tap to return", 14f, t.muted).apply { alpha = 0f }
            frame.addView(hint, FrameLayout.LayoutParams(WRAP, WRAP, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = ui.dp(48) })
            hint.animate().alpha(1f).setStartDelay(500).setDuration(400).withEndAction {
                hint.animate().alpha(0f).setStartDelay(2000).setDuration(800).start()
            }.start()
        }
        onLeave = { pv.cancelAnimations() }
        return frame
    }

    // ---------------------------------------------------------------- memory

    private fun buildMemory(): View {
        val n = prefs.grid
        val startLen = if (n == 3) 3 else 4
        val pv = PatternView(this).apply {
            configure(this); gridSize = n; autoFade = false; interactive = false
        }
        var difficulty = prefs.difficulty
        var length = startLen
        var target = emptyList<Int>()
        var state = "idle"

        val title = ui.text("Watch, then echo", 26f, t.text, ui.light).apply { gravity = Gravity.CENTER }
        val sub = ui.text("A pattern plays. Draw it back when you're ready.", 15f, t.muted).apply { gravity = Gravity.CENTER }
        val bestLine = ui.text("", 14f, t.muted).apply { gravity = Gravity.CENTER; visibility = View.GONE }
        fun bestText(): String {
            val b = prefs.best(difficulty, n)
            return if (b > 0) "Longest remembered: $b dots" else "Nothing remembered yet. No rush."
        }
        val bestIcon = ui.icon(IconKind.CHART, "Longest remembered", t.muted) {
            bestLine.text = bestText()
            bestLine.visibility = if (bestLine.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        fun describe(d: Difficulty) = when (d) {
            Difficulty.RELAXED -> "A faint guide stays while you draw."
            Difficulty.CLASSIC -> "The pattern hides before you draw."
            Difficulty.FOCUS -> "The pattern plays once, a little faster."
        }
        val desc = ui.text(describe(difficulty), 13f, t.muted).apply { gravity = Gravity.CENTER }

        val idlePanel = ui.vertical().apply { gravity = Gravity.CENTER_HORIZONTAL }
        val missPanel = ui.vertical().apply { gravity = Gravity.CENTER_HORIZONTAL; visibility = View.GONE }
        fun panel(which: View?) {
            idlePanel.visibility = if (which === idlePanel) View.VISIBLE else View.GONE
            missPanel.visibility = if (which === missPanel) View.VISIBLE else View.GONE
            if (which != null) ui.enter(which, rm)
        }

        fun toInput() {
            state = "input"
            title.text = "Your turn"
            sub.text = "${target.size} dots"
            pv.interactive = true
        }

        fun watch(then: () -> Unit) {
            state = "watch"
            pv.interactive = false
            pv.guide = null
            pv.clearPattern()
            title.text = "Watch"
            sub.text = "${target.size} dots"
            handler.postDelayed({
                pv.play(target, if (difficulty == Difficulty.FOCUS) 300L else 450L) {
                    handler.postDelayed({
                        if (difficulty == Difficulty.RELAXED) { pv.guide = target; pv.clearPattern(); then() }
                        else pv.fadeOut(0L, 350L) { then() }
                    }, 1000L)
                }
            }, 500L)
        }

        fun startRound() {
            target = Patterns.random(n, minOf(length, n * n))
            panel(null)
            watch { toInput() }
        }

        idlePanel.addView(ui.segmented(Difficulty.values().map { it.label }, difficulty.ordinal) { k ->
            difficulty = Difficulty.values()[k]
            prefs.difficulty = difficulty
            desc.text = describe(difficulty)
            bestLine.text = bestText()
        }, LinearLayout.LayoutParams(MATCH, WRAP))
        idlePanel.addView(desc, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = ui.dp(10) })
        idlePanel.addView(ui.pill("Begin", primary = true) { length = startLen; startRound() },
            LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = ui.dp(16) })

        val showBtn = ui.pill("Show pattern") { panel(null); watch { toInput() } }
        missPanel.addView(ui.pill("Try again", primary = true) {
            panel(null)
            pv.clearPattern()
            toInput()
        }, LinearLayout.LayoutParams(MATCH, WRAP))
        missPanel.addView(showBtn, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = ui.dp(10) })
        missPanel.addView(ui.pill("Start fresh") { length = startLen; startRound() },
            LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = ui.dp(10) })

        pv.listener = object : PatternView.Listener {
            override fun onNear() = haptics.near()
            override fun onDot(index: Int, count: Int, dx: Int, dy: Int, speed: Float, fromPlayback: Boolean) {
                if (fromPlayback) {
                    if (state == "watch") { sound.dot(count, index / n, n, dx, dy, 500f); haptics.dot(count, 500f) }
                    return
                }
                sound.dot(count, index / n, n, dx, dy, speed)
                haptics.dot(count, speed)
                totalDots++
            }
            override fun onRelease(pattern: List<Int>, shape: Shape) {
                if (state != "input") return
                persistCount()
                pv.interactive = false
                if (pattern == target) {
                    state = "match"
                    title.text = "Matched"
                    sound.complete(false)
                    haptics.complete()
                    pv.guide = null
                    pv.celebrate(true)
                    if (target.size > prefs.best(difficulty, n)) prefs.setBest(difficulty, n, target.size)
                    bestLine.text = bestText()
                    length = minOf(target.size + 1, n * n)
                    handler.postDelayed({ startRound() }, 1700L)
                } else {
                    state = "missed"
                    title.text = "Pattern missed"
                    sub.text = "Take your time."
                    sound.miss()
                    haptics.miss()
                    pv.miss()
                    showBtn.visibility = if (difficulty == Difficulty.FOCUS) View.GONE else View.VISIBLE
                    panel(missPanel)
                }
            }
        }

        val bottom = FrameLayout(this).apply {
            setPadding(ui.dp(28), ui.dp(8), ui.dp(28), ui.dp(36))
            addView(idlePanel, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))
            addView(missPanel, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))
        }
        onLeave = { pv.cancelAnimations() }
        return ui.vertical().apply {
            addView(ui.topBar("Memory", ::back, bestIcon))
            addView(title, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = ui.dp(8) })
            addView(sub, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = ui.dp(4); marginStart = ui.dp(32); marginEnd = ui.dp(32) })
            addView(bestLine, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = ui.dp(4) })
            addView(pv, LinearLayout.LayoutParams(MATCH, 0, 1f))
            addView(bottom, LinearLayout.LayoutParams(MATCH, WRAP))
        }
    }

    // ---------------------------------------------------------------- collection

    private fun buildCollection(): View {
        val rootCol = ui.vertical()
        val header = ui.horizontal().apply { setPadding(ui.dp(24), ui.dp(40), ui.dp(12), ui.dp(12)) }
        header.addView(ui.text("Collection", 28f, t.text, ui.light), LinearLayout.LayoutParams(0, WRAP, 1f))
        header.addView(ui.icon(IconKind.GEAR, "Settings", t.muted) { show(Screen.SETTINGS) })
        rootCol.addView(header)

        val listHost = FrameLayout(this)
        val filters = listOf("All", "Favourites", "3×3", "4×4", "5×5")
        lateinit var fill: () -> Unit
        lateinit var chipsHolder: FrameLayout
        fun chips(): View = ui.chipRow(*filters.mapIndexed { k, s ->
            ui.chip(s, k == collectionFilter) { collectionFilter = k; fill() }
        }.toTypedArray())
        chipsHolder = FrameLayout(this)
        rootCol.addView(chipsHolder, LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = ui.dp(8) })
        rootCol.addView(listHost, LinearLayout.LayoutParams(MATCH, 0, 1f))

        fill = {
            chipsHolder.removeAllViews()
            chipsHolder.addView(chips())
            listHost.removeAllViews()
            val all = store.load()
            val items = all.filter {
                when (collectionFilter) { 1 -> it.favorite; 2 -> it.n == 3; 3 -> it.n == 4; 4 -> it.n == 5; else -> true }
            }
            if (items.isEmpty()) {
                val empty = ui.vertical().apply {
                    gravity = Gravity.CENTER
                    setPadding(ui.dp(40), 0, ui.dp(40), ui.dp(40))
                    addView(View(this@MainActivity).apply {
                        background = ui.rounded(Color.TRANSPARENT, ui.dpf(20f), Themes.alpha(t.dot, 0.5f), 1f)
                    }, LinearLayout.LayoutParams(ui.dp(120), ui.dp(150)))
                    addView(ui.text(if (all.isEmpty()) "Patterns you save will rest here." else "Nothing here yet.", 15f, t.muted).apply {
                        gravity = Gravity.CENTER
                    }, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = ui.dp(20) })
                }
                listHost.addView(empty, FrameLayout.LayoutParams(MATCH, MATCH))
            } else {
                val grid = ui.vertical().apply { setPadding(ui.dp(16), ui.dp(4), ui.dp(16), ui.dp(24)) }
                for (start in items.indices step 2) {
                    val row = ui.horizontal().apply { gravity = Gravity.TOP }
                    for (k in 0..1) {
                        val p = items.getOrNull(start + k)
                        val lp = LinearLayout.LayoutParams(0, WRAP, 1f).apply { setMargins(ui.dp(6), ui.dp(6), ui.dp(6), ui.dp(6)) }
                        if (p == null) row.addView(View(this@MainActivity), lp)
                        else row.addView(card(p, start + k) { fill() }, lp)
                    }
                    grid.addView(row)
                }
                listHost.addView(ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(grid) })
            }
        }
        fill()

        val nav = ui.nav(2, { show(Screen.HOME) }, { show(Screen.MEMORY) }, {})
        rootCol.addView(nav.view, LinearLayout.LayoutParams(MATCH, WRAP).apply {
            setMargins(ui.dp(20), ui.dp(8), ui.dp(20), ui.dp(28))
        })
        return rootCol
    }

    private fun patternTitle(p: SavedPattern) =
        p.name.ifBlank { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(p.created)) }

    private fun patternMeta(p: SavedPattern) = "${p.n}×${p.n} · ${p.dots.size} dots"

    private fun card(p: SavedPattern, index: Int, refresh: () -> Unit): View {
        val c = ui.vertical().apply {
            setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(14))
            background = ui.ripple(ui.dpf(20f), ui.rounded(ui.surface(0.5f), ui.dpf(20f), Themes.alpha(t.dot, 0.35f), 1f))
            contentDescription = "${patternTitle(p)}, ${patternMeta(p)}${if (p.favorite) ", favourite" else ""}. Double tap to play, long press for options."
            setOnClickListener { playbackId = p.id; playbackLoop = false; show(Screen.PLAYBACK) }
            setOnLongClickListener { haptics.dot(1, 300f); openPatternOptions(p, refresh); true }
        }
        val art = FrameLayout(this)
        art.addView(PatternThumb(this, p.n, p.dots, t, ThumbStyle.NORMAL, !rm, index * 450L).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, FrameLayout.LayoutParams(MATCH, WRAP))
        if (p.favorite) art.addView(Icon(this, IconKind.HEART, t.active, "").apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, FrameLayout.LayoutParams(ui.dp(28), ui.dp(28), Gravity.TOP or Gravity.END))
        c.addView(art, LinearLayout.LayoutParams(MATCH, WRAP))
        c.addView(ui.text(patternTitle(p), 15f, t.text, ui.medium).apply { maxLines = 1; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO },
            LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = ui.dp(6); marginStart = ui.dp(4) })
        c.addView(ui.text(patternMeta(p), 12f, t.muted).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO },
            LinearLayout.LayoutParams(MATCH, WRAP).apply { marginStart = ui.dp(4) })
        return c
    }

    private fun openPatternOptions(p: SavedPattern, refresh: () -> Unit) {
        val c = ui.vertical()
        c.addView(ui.text(patternTitle(p), 18f, t.text, ui.medium).apply { setPadding(ui.dp(20), 0, ui.dp(20), ui.dp(8)) })
        lateinit var dismiss: () -> Unit
        val isHome = prefs.homePattern == p.id
        c.addView(ui.row("Play on loop") { dismiss(); playbackId = p.id; playbackLoop = true; show(Screen.PLAYBACK) })
        c.addView(ui.row(if (p.favorite) "Remove from favourites" else "Add to favourites") {
            store.update(p.copy(favorite = !p.favorite)); dismiss(); refresh()
        })
        c.addView(ui.row("Rename", "Up to 24 characters") { dismiss(); openRename(p, refresh) })
        c.addView(ui.row(if (isHome) "Stop using as home animation" else "Use as home animation",
            if (isHome) null else "Plays softly on the home grid when it's resting") {
            prefs.homePattern = if (isHome) 0L else p.id
            if (!isHome && p.n != prefs.grid) prefs.grid = p.n
            dismiss()
        })
        var armed = false
        lateinit var delRow: LinearLayout
        delRow = ui.row("Delete") {
            if (!armed) {
                armed = true
                ((delRow.getChildAt(0) as LinearLayout).getChildAt(0) as TextView).text = "Tap again to delete"
            } else {
                store.remove(p.id)
                if (prefs.homePattern == p.id) prefs.homePattern = 0L
                dismiss(); refresh()
            }
        }
        c.addView(delRow)
        dismiss = ui.sheet(root, c, rm) { dismissSheet = null }
        dismissSheet = dismiss
    }

    /** Rename sits near the top of the screen so the keyboard never covers it. */
    private fun openRename(p: SavedPattern, refresh: () -> Unit) {
        val overlay = FrameLayout(this)
        val scrim = View(this).apply { setBackgroundColor(Color.BLACK); alpha = 0.45f; isClickable = true }
        val card = ui.vertical().apply {
            setPadding(ui.dp(20), ui.dp(20), ui.dp(20), ui.dp(20))
            background = ui.rounded(Themes.mix(t.bgBottom, t.surface, 0.7f), ui.dpf(24f))
            isClickable = true
        }
        val edit = EditText(this).apply {
            setText(p.name)
            hint = "Name this pattern"
            setHintTextColor(t.muted)
            setTextColor(t.text)
            textSize = 18f
            filters = arrayOf(InputFilter.LengthFilter(24))
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_DONE
            background = ui.rounded(ui.surface(0.7f), ui.dpf(14f))
            setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14))
            setSelection(text.length)
        }
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        fun close() {
            imm.hideSoftInputFromWindow(edit.windowToken, 0)
            root.removeView(overlay)
            dismissSheet = null
        }
        fun save() {
            store.update(p.copy(name = edit.text.toString().trim()))
            close(); refresh()
        }
        edit.setOnEditorActionListener { _, action, _ -> if (action == EditorInfo.IME_ACTION_DONE) { save(); true } else false }
        card.addView(ui.text("Name", 18f, t.text, ui.medium))
        card.addView(edit, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = ui.dp(12) })
        val suggestions = (listOf(Shapes.detect(p.n, p.dots).suggestion) + listOf("Wave", "Orbit", "Drift", "Spiral")).distinct().take(4)
        val chips = ui.horizontal()
        suggestions.forEach { s ->
            chips.addView(ui.chip(s, false) { edit.setText(s); edit.setSelection(s.length) },
                LinearLayout.LayoutParams(WRAP, WRAP).apply { marginEnd = ui.dp(8) })
        }
        card.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(chips) },
            LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = ui.dp(12) })
        val buttons = ui.horizontal().apply { gravity = Gravity.END }
        buttons.addView(ui.pill("Cancel") { close() }.apply { minWidth = ui.dp(110) })
        buttons.addView(ui.pill("Save", primary = true) { save() }.apply { minWidth = ui.dp(110) },
            LinearLayout.LayoutParams(WRAP, WRAP).apply { marginStart = ui.dp(10) })
        card.addView(buttons, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = ui.dp(18) })
        scrim.setOnClickListener { close() }
        overlay.addView(scrim, FrameLayout.LayoutParams(MATCH, MATCH))
        overlay.addView(card, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.TOP).apply { setMargins(ui.dp(20), ui.dp(96), ui.dp(20), 0) })
        root.addView(overlay, FrameLayout.LayoutParams(MATCH, MATCH))
        dismissSheet = { close() }
        edit.requestFocus()
        edit.postDelayed({ imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT) }, 150)
    }

    // ---------------------------------------------------------------- playback

    private fun buildPlayback(): View {
        var p = store.get(playbackId) ?: return buildCollection().also { screen = Screen.COLLECTION }
        val pv = PatternView(this).apply {
            configure(this); gridSize = p.n; autoFade = false; interactive = false
        }
        pv.listener = object : PatternView.Listener {
            override fun onDot(index: Int, count: Int, dx: Int, dy: Int, speed: Float, fromPlayback: Boolean) {
                sound.dot(count, index / p.n, p.n, dx, dy, 500f)
                haptics.dot(count, 500f)
            }
        }
        var speed = 1f
        var loop = playbackLoop
        lateinit var play: () -> Unit
        val again = Runnable { play() }
        play = {
            handler.removeCallbacks(again)
            pv.play(p.dots, (420 / speed).toLong()) {
                val shape = Shapes.detect(p.n, p.dots)
                sound.complete(shape == Shape.ALL_DOTS)
                if (shape == Shape.ALL_DOTS) haptics.allDots() else haptics.complete()
                pv.celebrate(true)
                if (loop) handler.postDelayed(again, (2200 / speed).toLong())
            }
        }
        handler.postDelayed(again, 400)

        val heart = ui.icon(if (p.favorite) IconKind.HEART else IconKind.HEART_OUTLINE,
            if (p.favorite) "Remove from favourites" else "Add to favourites", if (p.favorite) t.active else t.muted)
        heart.background = ui.ripple(ui.dpf(24f))
        heart.isClickable = true
        heart.setOnClickListener {
            p = p.copy(favorite = !p.favorite)
            store.update(p)
            heart.kind = if (p.favorite) IconKind.HEART else IconKind.HEART_OUTLINE
            heart.color = if (p.favorite) t.active else t.muted
            heart.contentDescription = if (p.favorite) "Remove from favourites" else "Add to favourites"
            haptics.dot(1, 300f)
            heart.invalidate()
        }

        val speedText = ui.text("1.0×", 14f, t.muted).apply { minWidth = ui.dp(48); gravity = Gravity.END }
        val speedRow = ui.horizontal().apply { setPadding(ui.dp(20), 0, ui.dp(20), 0) }
        speedRow.addView(ui.text("Speed", 15f, t.text))
        speedRow.addView(ui.slider(1f / 3f, "Playback speed", { v ->
            speed = 0.5f + v * 1.5f
            speedText.text = "${"%.1f".format(speed)}×"
        }, {}), LinearLayout.LayoutParams(0, WRAP, 1f))
        speedRow.addView(speedText)

        lateinit var loopToggle: Toggle
        loopToggle = Toggle(this, t, loop, "Loop") { v -> loop = v; if (v && !pv.isPlaying) play() }
        val loopRow = ui.row("Loop", control = loopToggle) { loopToggle.set(!loopToggle.checked, true) }

        onLeave = { pv.cancelAnimations() }
        return ui.vertical().apply {
            addView(ui.topBar(patternTitle(p), ::back, heart))
            addView(ui.text(patternMeta(p), 14f, t.muted).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(MATCH, WRAP))
            addView(pv, LinearLayout.LayoutParams(MATCH, 0, 1f))
            addView(speedRow, LinearLayout.LayoutParams(MATCH, WRAP))
            addView(loopRow, LinearLayout.LayoutParams(MATCH, WRAP))
            addView(ui.pill("Play again", primary = true) { play() }, LinearLayout.LayoutParams(WRAP, WRAP).apply {
                gravity = Gravity.CENTER_HORIZONTAL; topMargin = ui.dp(8); bottomMargin = ui.dp(36)
            })
        }
    }

    // ---------------------------------------------------------------- settings

    private fun toggleRow(title: String, sub: String?, value: Boolean, onChange: (Boolean) -> Unit): View {
        lateinit var tg: Toggle
        tg = Toggle(this, t, value, title, onChange)
        return ui.row(title, sub, tg) { tg.set(!tg.checked, true) }
    }

    private fun rebuildSettings() {
        applyTheme()
        val scrollY = (root.getChildAt(0) as? ViewGroup)?.let { findScroll(it)?.scrollY } ?: 0
        show(Screen.SETTINGS)
        (root.getChildAt(0) as? ViewGroup)?.let { g -> findScroll(g)?.let { s -> s.post { s.scrollTo(0, scrollY) } } }
    }

    private fun findScroll(g: ViewGroup): ScrollView? {
        for (k in 0 until g.childCount) {
            val c = g.getChildAt(k)
            if (c is ScrollView) return c
            if (c is ViewGroup) findScroll(c)?.let { return it }
        }
        return null
    }

    private fun buildSettings(): View {
        val col = ui.vertical().apply { setPadding(ui.dp(4), 0, ui.dp(4), ui.dp(40)) }

        // Theme
        col.addView(ui.section("THEME"))
        val tiles = ui.horizontal().apply { setPadding(ui.dp(16), 0, ui.dp(16), 0) }
        Themes.all.forEachIndexed { i, base ->
            val th = Themes.adjusted(base, prefs.amoled, prefs.highContrast)
            val on = i == prefs.themeIndex
            val tile = ui.vertical().apply {
                setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12))
                background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(th.bgTop, th.bgBottom)).apply {
                    cornerRadius = ui.dpf(20f)
                    if (on) setStroke(ui.dp(2), th.active) else setStroke(ui.dp(1), Themes.alpha(th.dot, 0.6f))
                }
                foreground = ui.ripple(ui.dpf(20f))
                contentDescription = "${base.name} theme. ${base.blurb}${if (on) ", selected" else ""}"
                isSelected = on
                setOnClickListener {
                    if (on) return@setOnClickListener
                    prefs.themeIndex = i
                    rebuildSettings()
                    sound.preview()
                }
                addView(PatternThumb(this@MainActivity, 3, listOf(6, 4, 2, 5, 8), th, ThumbStyle.NORMAL, !rm, i * 500L).apply {
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(MATCH, WRAP))
                addView(TextView(this@MainActivity).apply {
                    text = base.name; textSize = 15f; setTextColor(th.text); typeface = ui.medium
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                })
                addView(TextView(this@MainActivity).apply {
                    text = base.blurb; textSize = 11f; setTextColor(th.muted); maxLines = 2
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                })
            }
            tiles.addView(tile, LinearLayout.LayoutParams(ui.dp(128), ui.dp(184)).apply { marginEnd = ui.dp(10) })
        }
        col.addView(HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(tiles)
            post { scrollTo(((prefs.themeIndex - 1).coerceAtLeast(0)) * ui.dp(138), 0) }
        })

        // Sound
        col.addView(ui.section("SOUND"))
        col.addView(toggleRow("Sound", null, prefs.soundOn) { prefs.soundOn = it; applySenses(); if (it) sound.preview() })
        col.addView(ui.row("Volume", control = ui.slider(prefs.volume, "Volume", { v -> prefs.volume = v; sound.volume = v }) {
            sound.playDegree(4, 0.7f)
        }.apply { layoutParams = LinearLayout.LayoutParams(ui.dp(170), WRAP) }))
        fun envName() = if (prefs.soundEnv < 0) "Match theme (${Themes.all[prefs.themeIndex].sound.label})" else soundEnv().label
        lateinit var envRow: LinearLayout
        envRow = ui.row("Sound environment", envName()) {
            val next = prefs.soundEnv + 1
            prefs.soundEnv = if (next >= SoundEnv.values().size) -1 else next
            applySenses()
            ((envRow.getChildAt(0) as LinearLayout).findViewWithTag<TextView>("sub")).text = envName()
            handler.postDelayed({ sound.preview() }, 700)
        }
        col.addView(envRow)
        col.addView(toggleRow("Direction-aware notes", "Upward strokes rise, downward strokes fall", prefs.directional) {
            prefs.directional = it; applySenses()
        })

        // Touch
        col.addView(ui.section("TOUCH"))
        col.addView(toggleRow("Vibration", null, prefs.hapticsOn) { prefs.hapticsOn = it; applySenses(); if (it) haptics.preview() })
        col.addView(ui.text("Haptic intensity", 16f, t.text).apply { setPadding(ui.dp(20), ui.dp(12), ui.dp(20), ui.dp(8)) })
        col.addView(ui.segmented(listOf("Light", "Medium", "Strong"), prefs.hapticLevel - 1) { k ->
            prefs.hapticLevel = k + 1; applySenses(); haptics.preview()
        }, LinearLayout.LayoutParams(MATCH, WRAP).apply { setMargins(ui.dp(20), 0, ui.dp(20), ui.dp(8)) })
        col.addView(ui.text("Default grid", 16f, t.text).apply { setPadding(ui.dp(20), ui.dp(12), ui.dp(20), ui.dp(8)) })
        col.addView(ui.segmented(listOf("3×3", "4×4", "5×5"), prefs.grid - 3) { k -> prefs.grid = k + 3 },
            LinearLayout.LayoutParams(MATCH, WRAP).apply { setMargins(ui.dp(20), 0, ui.dp(20), ui.dp(8)) })
        col.addView(toggleRow("Show path lines", "Off shows dots only", prefs.showLines) { prefs.showLines = it })

        // Comfort & accessibility
        col.addView(ui.section("COMFORT & ACCESSIBILITY"))
        col.addView(toggleRow("Reduce motion", "Fades instead of springs and ripples", prefs.reduceMotion) { prefs.reduceMotion = it; rebuildSettings() })
        col.addView(toggleRow("High contrast", "Brighter dots and lines, flat background", prefs.highContrast) { prefs.highContrast = it; rebuildSettings() })
        col.addView(toggleRow("AMOLED true black", "Black background in every theme", prefs.amoled) { prefs.amoled = it; rebuildSettings() })
        col.addView(toggleRow("Larger dots", "Bigger dots and touch areas", prefs.largerDots) { prefs.largerDots = it })
        col.addView(toggleRow("Quiet completions", "Patterns simply fade when you lift", prefs.quietCompletion) { prefs.quietCompletion = it })

        // About
        col.addView(ui.section("ABOUT"))
        col.addView(ui.row("Pattern Fidget 3.0", "Nothing leaves your device. No accounts, ads or notifications."))
        val countRow = ui.row("Dots connected", NumberFormat.getIntegerInstance().format(totalDots)).apply {
            visibility = if (prefs.showCount) View.VISIBLE else View.GONE
        }
        col.addView(toggleRow("Show lifetime count", null, prefs.showCount) { prefs.showCount = it; countRow.visibility = if (it) View.VISIBLE else View.GONE })
        col.addView(countRow)

        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(col) }
        return ui.vertical().apply {
            addView(ui.topBar("Settings", ::back))
            addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))
        }
    }

    // ---------------------------------------------------------------- system

    private fun persistCount() { prefs.totalDots = totalDots }

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
        sound.shutdown()
    }

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}
