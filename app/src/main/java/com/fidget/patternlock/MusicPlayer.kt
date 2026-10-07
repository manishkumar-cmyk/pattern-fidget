package com.fidget.patternlock

import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * Quiet looping background music. It plays only while the app is in the foreground and music is on, and it
 * fades in and out. The track is `res/raw/ambient_music` (mp3, ogg or wav); with no file the player stays idle
 * and the Settings row is hidden.
 */
class MusicPlayer(private val context: Context) {

    private val resId = context.resources.getIdentifier("ambient_music", "raw", context.packageName)
    val available get() = resId != 0

    var enabled = true
    var volume = 0.5f
        set(value) { field = value.coerceIn(0f, 1f); if (player != null && fade >= 1f) apply() }
    var foreground = false
        set(value) { field = value; sync() }

    private var player: MediaPlayer? = null
    private var fade = 0f
    private var fader: ValueAnimator? = null

    private fun apply() {
        val v = (volume * volume * fade).coerceIn(0f, 1f)
        player?.setVolume(v, v)
    }

    private fun fadeTo(target: Float, ms: Long, then: (() -> Unit)? = null) {
        fader?.cancel()
        fader = ValueAnimator.ofFloat(fade, target).apply {
            duration = ms
            addUpdateListener { fade = it.animatedValue as Float; apply() }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                private var cancelled = false
                override fun onAnimationCancel(animation: android.animation.Animator) { cancelled = true }
                override fun onAnimationEnd(animation: android.animation.Animator) { if (!cancelled) then?.invoke() }
            })
            start()
        }
    }

    /** Starts or stops playback to match [enabled] and [foreground]. Safe to call often. */
    fun sync() {
        if (!available) return
        if (enabled && foreground) play() else stop()
    }

    private fun play() {
        if (player == null) {
            val p = try { MediaPlayer.create(context, resId) } catch (e: Exception) { null } ?: return
            p.isLooping = true
            p.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            player = p
            fade = 0f
            apply()
            p.start()
        } else if (player?.isPlaying == false) player?.start()
        fadeTo(1f, 1500)
    }

    private fun stop() {
        val p = player ?: return
        fadeTo(0f, 500) {
            if (!(enabled && foreground)) { try { p.pause() } catch (e: Exception) { } }
        }
    }

    fun release() {
        fader?.cancel()
        player?.release()
        player = null
    }
}
