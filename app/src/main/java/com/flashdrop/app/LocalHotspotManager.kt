package com.flashdrop.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.net.Inet4Address
import java.net.NetworkInterface

data class HotspotCredentials(
    val ssid: String,
    val password: String
)

class LocalHotspotManager(context: Context) {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(WifiManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var reservation: WifiManager.LocalOnlyHotspotReservation? = null

    fun start(
        onStarted: (HotspotCredentials) -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onError("Local-only hotspot requires Android 8.0 or newer")
            return
        }
        if (!hasHotspotPermission()) {
            onError("Nearby Wi-Fi permission is required")
            return
        }
        if (reservation != null) {
            credentialsFromReservation(reservation!!)?.let(onStarted)
                ?: onError("The local hotspot credentials are unavailable")
            return
        }

        try {
            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback() {
                override fun onStarted(value: WifiManager.LocalOnlyHotspotReservation) {
                    reservation = value
                    val credentials = credentialsFromReservation(value)
                    if (credentials == null) {
                        stop()
                        onError("Android did not return hotspot credentials")
                    } else {
                        onStarted(credentials)
                    }
                }

                override fun onStopped() {
                    reservation = null
                }

                override fun onFailed(reason: Int) {
                    reservation = null
                    onError(
                        when (reason) {
                            2 -> "Turn off the phone's normal Wi-Fi hotspot. FlashDrop must create its own local-only hotspot."
                            3 -> "The Wi-Fi channel is unavailable. Turn Wi-Fi off and on, then try again."
                            4 -> "Android does not allow a local-only hotspot while tethering is active. Turn off system hotspot and try again."
                            else -> "Could not start FlashDrop local hotspot (error $reason). Turn off the normal hotspot and try again."
                        }
                    )
                }
            }, mainHandler)
        } catch (error: SecurityException) {
            onError("Android denied permission to start the local hotspot")
        } catch (error: IllegalStateException) {
            onError(error.message ?: "The local hotspot is unavailable")
        }
    }

    fun stop() {
        reservation?.close()
        reservation = null
    }

    fun localIpv4Address(): String? {
        return NetworkInterface.getNetworkInterfaces()?.toList()
            ?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull {
                it is Inet4Address && !it.isLoopbackAddress && it.isSiteLocalAddress
            }
            ?.hostAddress
    }

    private fun hasHotspotPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    @Suppress("DEPRECATION")
    private fun credentialsFromReservation(
        value: WifiManager.LocalOnlyHotspotReservation
    ): HotspotCredentials? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val configuration = value.softApConfiguration
            val ssid = configuration.ssid
            val password = configuration.passphrase
            if (ssid.isNullOrBlank() || password.isNullOrBlank()) null else HotspotCredentials(ssid, password)
        } else {
            val configuration = value.wifiConfiguration
            val ssid = configuration?.SSID?.trim('"')
            val password = configuration?.preSharedKey?.trim('"')
            if (ssid.isNullOrBlank() || password.isNullOrBlank()) null else HotspotCredentials(ssid, password)
        }
    }
}
