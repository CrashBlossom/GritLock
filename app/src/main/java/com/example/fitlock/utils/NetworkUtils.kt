package com.example.fitlock.utils

import android.content.Context
import android.net.wifi.WifiManager

object NetworkUtils {
    fun getCurrentSsid(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wifiManager.connectionInfo
            if (info != null) {
                val ssid = info.ssid
                if (ssid != null && ssid != "<unknown ssid>" && ssid != "0x") {
                    return ssid.replace("\"", "")
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun getNearbySsids(context: Context): List<String> {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiManager.scanResults
                .map { it.SSID }
                .filter { it.isNotBlank() && it != "<unknown ssid>" }
                .distinct()
                .sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
