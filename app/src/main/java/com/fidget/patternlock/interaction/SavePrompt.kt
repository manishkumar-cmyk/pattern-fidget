package com.fidget.patternlock.interaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fidget.patternlock.data.FidgetEnv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The quiet "Save" offer that appears under the grid after a pattern, then goes away by itself. */
class SavePrompt(private val env: FidgetEnv, private val scope: CoroutineScope) {
    var visible by mutableStateOf(false)
        private set
    var label by mutableStateOf("Save")
        private set
    private var candidate: Pair<Int, List<Int>>? = null
    private var hideJob: Job? = null

    fun offer(n: Int, dots: List<Int>) {
        candidate = n to dots
        label = if (env.prefs.hintCount("save") == 0) "Keep this one?" else "Save"
        visible = true
        scheduleHide(3500)
    }

    fun hide() {
        hideJob?.cancel()
        visible = false
    }

    fun save() {
        val (n, dots) = candidate ?: return
        if (env.store.add(n, dots) != null) {
            if (env.prefs.hintCount("save") == 0) env.prefs.bumpHint("save")
            env.haptics.dot(1, 300f)
            label = "Saved"
        }
        scheduleHide(900)
    }

    private fun scheduleHide(ms: Long) {
        hideJob?.cancel()
        hideJob = scope.launch { delay(ms); visible = false }
    }
}
