package com.example.fitlock.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.fitlock.ui.AppInfo

object AppInfoFetcher {
    fun getInstalledApps(context: Context): List<AppInfo> {
        val packageManager = context.packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
        intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        
        val launchableApps = packageManager.queryIntentActivities(intent, 0)
        
        return launchableApps
            .map {
                AppInfo(
                    name = it.loadLabel(packageManager).toString(),
                    packageName = it.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.name }
    }
}
