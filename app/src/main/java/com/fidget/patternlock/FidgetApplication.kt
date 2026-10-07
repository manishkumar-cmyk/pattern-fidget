package com.fidget.patternlock

import android.app.Application
import com.fidget.patternlock.data.FidgetEnv

class FidgetApplication : Application() {
    lateinit var env: FidgetEnv
        private set

    override fun onCreate() {
        super.onCreate()
        env = FidgetEnv(this)
    }
}
