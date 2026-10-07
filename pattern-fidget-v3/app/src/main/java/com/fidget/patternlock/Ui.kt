package com.fidget.patternlock

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/** Small view builders shared by every screen. Everything is code-built, so the app needs no layout files. */
class Ui(val a: Activity, var t: Theme) {

    val density = a.resources.displayMetrics.density
    val light: Typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    val regular: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    val medium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    fun dp(v: Int) = (v * density).toInt()
    fun dpf(v: Float) = v * density

    fun text(s: String, size: Float, color: Int = t.text, face: Typeface = regular) = TextView(a).apply {
        text = s
        textSize = size
        setTextColor(color)
        typeface = face
        letterSpacing = 0.01f
    }

    fun vertical() = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
    fun horizontal() = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

    fun rounded(color: Int, radius: Float, strokeColor: Int = 0, strokeDp: Float = 0f) = GradientDrawable().apply {
        cornerRadius = radius
        setColor(color)
        if (strokeDp > 0f) setStroke(dpf(strokeDp).toInt().coerceAtLeast(1), strokeColor)
    }

    fun ripple(radius: Float, base: Drawable? = null): Drawable {
        val mask = GradientDrawable().apply { cornerRadius = radius; setColor(Color.WHITE) }
        return RippleDrawable(ColorStateList.valueOf(Themes.alpha(t.active, 0.22f)), base, mask)
    }

    /** Translucent surface used by cards, pills and panels. */
    fun surface(alpha: Float = 0.55f) = Themes.alpha(t.surface, alpha)

    fun pill(label: String, primary: Boolean = false, onClick: () -> Unit) = text(label, 16f, if (primary) t.bgTop else t.text, medium).apply {
        gravity = Gravity.CENTER
        minHeight = dp(52)
        minWidth = dp(150)
        setPadding(dp(28), dp(12), dp(28), dp(12))
        val bg = if (primary) rounded(t.active, dpf(26f)) else rounded(surface(0.7f), dpf(26f), Themes.alpha(t.dot, 0.5f), 1f)
        background = ripple(dpf(26f), bg)
        setOnClickListener { onClick() }
    }

    fun icon(kind: IconKind, label: String, color: Int = t.text, sizeDp: Int = 48, onClick: (() -> Unit)? = null) =
        Icon(a, kind, color, label).apply {
            layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
            if (onClick != null) {
                background = ripple(dpf(sizeDp / 2f))
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            }
        }

    fun topBar(title: String, onBack: () -> Unit, right: View? = null): View = horizontal().apply {
        setPadding(dp(12), dp(36), dp(16), dp(4))
        addView(icon(IconKind.BACK, "Back", onClick = onBack))
        addView(text(title, 18f, t.text, medium), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(4) })
        if (right != null) addView(right)
    }

    /** A pill track with one highlighted option. */
    fun segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit): LinearLayout {
        val row = horizontal()
        row.background = rounded(surface(0.6f), dpf(24f))
        row.setPadding(dp(4), dp(4), dp(4), dp(4))
        val items = ArrayList<TextView>()
        fun paint(sel: Int) = items.forEachIndexed { k, tv ->
            tv.background = if (k == sel) rounded(Themes.alpha(t.active, 0.22f), dpf(20f), Themes.alpha(t.active, 0.6f), 1f) else null
            tv.setTextColor(if (k == sel) t.text else t.muted)
            tv.isSelected = k == sel
        }
        options.forEachIndexed { k, s ->
            val tv = text(s, 14f, t.muted, medium).apply {
                gravity = Gravity.CENTER
                minHeight = dp(44)
                setPadding(dp(18), 0, dp(18), 0)
                setOnClickListener { paint(k); onSelect(k) }
            }
            items.add(tv)
            row.addView(tv, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        paint(selected)
        return row
    }

    fun chip(label: String, selected: Boolean, onClick: () -> Unit) = text(label, 14f, if (selected) t.text else t.muted, medium).apply {
        gravity = Gravity.CENTER
        minHeight = dp(40)
        setPadding(dp(16), 0, dp(16), 0)
        val bg = if (selected) rounded(Themes.alpha(t.active, 0.2f), dpf(20f), Themes.alpha(t.active, 0.6f), 1f)
        else rounded(surface(0.5f), dpf(20f))
        background = ripple(dpf(20f), bg)
        setOnClickListener { onClick() }
    }

    fun chipRow(vararg chips: View): View {
        val row = horizontal().apply { setPadding(dp(20), 0, dp(20), 0) }
        chips.forEach { row.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(8) }) }
        return HorizontalScrollView(a).apply { isHorizontalScrollBarEnabled = false; addView(row) }
    }

    class Nav(val view: LinearLayout, val pills: List<View>)

    /** Draw · Memory · Collection. [active] is 0, 1 or 2. */
    fun nav(active: Int, onDraw: () -> Unit, onMemory: () -> Unit, onCollection: () -> Unit): Nav {
        val row = horizontal()
        val pills = ArrayList<View>()
        listOf(Triple("Draw", IconKind.DRAW, onDraw), Triple("Memory", IconKind.MEMORY, onMemory),
            Triple("Collection", IconKind.COLLECTION, onCollection)).forEachIndexed { k, (label, kind, click) ->
            val on = k == active
            val col = vertical().apply {
                gravity = Gravity.CENTER
                minimumHeight = dp(64)
                val bg = if (on) rounded(Themes.alpha(t.active, 0.2f), dpf(22f), Themes.alpha(t.active, 0.55f), 1f)
                else rounded(surface(0.45f), dpf(22f))
                background = ripple(dpf(22f), bg)
                isClickable = true
                isFocusable = true
                contentDescription = label
                isSelected = on
                setOnClickListener { click() }
                addView(Icon(a, kind, if (on) t.active else t.muted, "").apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO },
                    LinearLayout.LayoutParams(dp(28), dp(28)).apply { gravity = Gravity.CENTER_HORIZONTAL })
                addView(text(label, 12f, if (on) t.text else t.muted, medium).apply {
                    gravity = Gravity.CENTER; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER_HORIZONTAL })
            }
            pills.add(col)
            row.addView(col, LinearLayout.LayoutParams(0, dp(68), 1f).apply {
                if (k > 0) marginStart = dp(10)
            })
        }
        return Nav(row, pills)
    }

    /** A settings row: title (+ optional subtitle) on the left, a control on the right. */
    fun row(title: String, sub: String? = null, control: View? = null, onClick: (() -> Unit)? = null): LinearLayout {
        val r = horizontal().apply {
            minimumHeight = dp(60)
            setPadding(dp(20), dp(8), dp(12), dp(8))
            if (onClick != null) {
                background = ripple(dpf(16f))
                setOnClickListener { onClick() }
            }
        }
        val texts = vertical()
        texts.addView(text(title, 16f, t.text))
        if (sub != null) texts.addView(text(sub, 13f, t.muted).apply { tag = "sub" })
        r.addView(texts, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        if (control != null) r.addView(control)
        return r
    }

    fun section(title: String) = text(title, 13f, t.muted, medium).apply {
        setPadding(dp(20), dp(28), dp(20), dp(6))
        isAllCaps = false
        letterSpacing = 0.06f
    }

    fun slider(value: Float, label: String, onChange: (Float) -> Unit, onRelease: (Float) -> Unit) = SeekBar(a).apply {
        max = 100
        progress = (value * 100).toInt()
        contentDescription = label
        progressTintList = ColorStateList.valueOf(t.active)
        thumbTintList = ColorStateList.valueOf(t.active)
        progressBackgroundTintList = ColorStateList.valueOf(t.dot)
        minimumHeight = dp(48)
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) { if (fromUser) onChange(p / 100f) }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) = onRelease(s.progress / 100f)
        })
    }

    /** Rises a view in: fade plus a 16dp drift. */
    fun enter(v: View, reduceMotion: Boolean) {
        v.alpha = 0f
        v.translationY = if (reduceMotion) 0f else dpf(16f)
        v.animate().alpha(1f).translationY(0f).setDuration(if (reduceMotion) 150 else 320)
            .setInterpolator(DecelerateInterpolator(2f)).start()
    }

    /** A bottom sheet over [host]. Returns a function that dismisses it. */
    fun sheet(host: FrameLayout, content: View, reduceMotion: Boolean, onDismissed: () -> Unit = {}): () -> Unit {
        val overlay = FrameLayout(a)
        val scrim = View(a).apply {
            setBackgroundColor(Color.BLACK)
            alpha = 0f
            isClickable = true
            contentDescription = "Close"
        }
        val panel = FrameLayout(a).apply {
            background = GradientDrawable().apply {
                cornerRadii = floatArrayOf(dpf(28f), dpf(28f), dpf(28f), dpf(28f), 0f, 0f, 0f, 0f)
                setColor(Themes.mix(t.bgBottom, t.surface, 0.7f))
            }
            setPadding(0, dp(12), 0, dp(28))
            isClickable = true
            addView(View(a).apply { background = rounded(Themes.alpha(t.muted, 0.5f), dpf(2f)) },
                FrameLayout.LayoutParams(dp(36), dp(4)).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL })
            addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(16) })
        }
        overlay.addView(scrim, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        overlay.addView(panel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        host.addView(overlay, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val dur = if (reduceMotion) 120L else 280L
        scrim.animate().alpha(0.45f).setDuration(dur).start()
        panel.translationY = dpf(400f)
        panel.alpha = if (reduceMotion) 0f else 1f
        panel.post { panel.translationY = if (reduceMotion) 0f else panel.height.toFloat(); panel.animate().translationY(0f).alpha(1f).setDuration(dur).setInterpolator(DecelerateInterpolator(2f)).start() }
        var gone = false
        val dismiss = {
            if (!gone) {
                gone = true
                scrim.animate().alpha(0f).setDuration(dur).start()
                panel.animate().translationY(if (reduceMotion) 0f else panel.height.toFloat()).alpha(if (reduceMotion) 0f else 1f)
                    .setDuration(dur).withEndAction { host.removeView(overlay); onDismissed() }.start()
            }
        }
        scrim.setOnClickListener { dismiss() }
        return dismiss
    }
}
