package com.flashdrop.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean

class WifiConnectionManager(context: Context) {
    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(WifiManager::class.java)
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var legacyNetworkId: Int? = null

    fun connect(
        credentials: HotspotCredentials,
        onConnected: (Network?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasConnectionPermission()) {
            onError("Nearby Wi-Fi permission is required")
            return
        }
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.Q..Build.VERSION_CODES.S_V2) {
            val locationManager = appContext.getSystemService(LocationManager::class.java)
            if (!locationManager.isLocationEnabled) {
                onError("Turn on Location services to connect to Wi-Fi on Android 10–12")
                return
            }
        }
        disconnect()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val specifier = WifiNetworkSpecifier.Builder()
                    .setSsid(credentials.ssid)
                    .setWpa2Passphrase(credentials.password)
                    .build()
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build()
                val delivered = AtomicBoolean(false)
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        if (!delivered.compareAndSet(false, true)) return
                        // Use Network.socketFactory for the transfer. A process-wide bind is
                        // unnecessary and can be rejected by some Android builds.
                        onConnected(network)
                    }

                    override fun onUnavailable() {
                        if (delivered.compareAndSet(false, true)) {
                            onError("The phone could not connect to the local hotspot")
                        }
                    }

                    override fun onLost(network: Network) {
                        if (!delivered.get()) onError("The local Wi-Fi connection was lost")
                    }
                }
                networkCallback = callback
                connectivityManager.requestNetwork(request, callback)
            } else {
                @Suppress("DEPRECATION")
                val configuration = WifiConfiguration().apply {
                    SSID = "\"${credentials.ssid}\""
                    preSharedKey = "\"${credentials.password}\""
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                }
                @Suppress("DEPRECATION")
                val networkId = wifiManager.addNetwork(configuration)
                if (networkId == -1) {
                    onError("Android rejected the local hotspot configuration")
                    return
                }
                legacyNetworkId = networkId
                @Suppress("DEPRECATION")
                wifiManager.disconnect()
                @Suppress("DEPRECATION")
                wifiManager.enableNetwork(networkId, true)
                @Suppress("DEPRECATION")
                wifiManager.reconnect()
                handler.postDelayed({
                    @Suppress("DEPRECATION")
                    val connected = wifiManager.connectionInfo?.networkId == networkId
                    if (connected) onConnected(null) else onError("The phone could not connect to the local hotspot")
                }, 5_000L)
            }
        } catch (error: SecurityException) {
            onError("Android denied permission to connect to the local hotspot")
        } catch (error: IllegalArgumentException) {
            onError(error.message ?: "The hotspot credentials were rejected")
        } catch (error: IllegalStateException) {
            onError(error.message ?: "Wi-Fi connection is currently unavailable")
        }
    }

    fun disconnect() {
        networkCallback?.let {
            try { connectivityManager.unregisterNetworkCallback(it) } catch (_: Exception) { }
        }
        networkCallback = null
        legacyNetworkId?.let {
            @Suppress("DEPRECATION")
            wifiManager.removeNetwork(it)
        }
        legacyNetworkId = null
    }

    private fun hasConnectionPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
    }
}
