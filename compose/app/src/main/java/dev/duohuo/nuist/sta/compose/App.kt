package dev.duohuo.nuist.sta.compose

import android.app.Application
import android.os.Build
import com.kieronquinn.monetcompat.core.MonetCompat
import dev.duohuo.nuist.sta.compose.core.crash.CrashHandler
import dev.duohuo.nuist.sta.compose.core.env.AppConfig
import dev.duohuo.nuist.sta.compose.data.notification.TimetableNotificationScheduler
import dev.duohuo.nuist.sta.compose.di.init.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        CrashHandler.init()
        super.onCreate()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            MonetCompat.setup(this)
            MonetCompat.enablePaletteCompat()
            MonetCompat.getInstance().updateMonetColors()
        }

        if (AppConfig.isLogEnabled) {
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement) = super.createStackElementTag(element)
                    ?.substringBefore('$')
                    ?.take(23)
            })
        }

        startKoin {
            // Koin Android Logger
            androidLogger()
            // Koin Android Context
            androidContext(this@App)
            // use modules
            modules(appModules)
        }

        // 闹钟不跨重启保留：进程每次冷启动都按缓存重新登记，用户打开一次即可恢复。
        TimetableNotificationScheduler(this).rearmFromCache()
    }
}
