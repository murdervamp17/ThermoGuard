package com.thermoguard.app

import android.app.Application
import com.thermoguard.app.work.MonitorWorker
import com.thermoguard.app.work.Notifier

class ThermoGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannel(this)
        MonitorWorker.schedule(this)
    }
}
