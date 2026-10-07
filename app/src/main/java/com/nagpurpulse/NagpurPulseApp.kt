package com.nagpurpulse

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.nagpurpulse.notifications.ScheduledPushManager
import com.nagpurpulse.notifications.createNotificationChannels
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import android.content.Context
import com.nagpurpulse.ui.locale.AppLocale

@HiltAndroidApp
class NagpurPulseApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)
        // Schedule 4× daily push notifications
        ScheduledPushManager.schedule(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
