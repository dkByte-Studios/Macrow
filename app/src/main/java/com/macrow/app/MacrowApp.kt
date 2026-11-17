package com.macrow.app

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.StrictMode
import com.macrow.app.data.BackupService
import com.macrow.app.data.MacrowDatabase
import com.macrow.app.data.MacrowRepository
import com.macrow.app.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MacrowApp : Application() {
    lateinit var repo: MacrowRepository
        private set
    lateinit var backup: BackupService
        private set

    /** Outlives any screen, so a backup or the first-run food list is never cut off by closing the app. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Debug builds only: log any disk or network work on the main thread and any leaked resources.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
        }
        repo = MacrowRepository(MacrowDatabase.create(this), SettingsStore(this))
        backup = BackupService(this, repo)
        appScope.launch { runCatching { repo.ensureSeeded() } }
    }
}
