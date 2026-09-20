package com.flashdrop.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

data class P2pCredentials(
    val networkName: String?,
    val passphrase: String?,
    val groupOwnerAddress: String,
    val ownerDeviceAddress: String?
)

class WifiDirectManager(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(WifiP2pManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val channel = manager.initialize(appContext, Looper.getMainLooper(), null)
    private var receiver: BroadcastReceiver? = null
    private var connected = false
    private var connectionCallbackSent = false

    fun startGroup(
        onReady: (P2pCredentials) -> Unit,
        onConnectionChanged: (Boolean) -> Unit,
        onError: (String) -> Unit,
        onPeerName: (String) -> Unit = {}
    ) {
        if (!hasPermission()) {
            onError("Nearby Wi-Fi permission is required for Wi-Fi Direct")
            return
        }
        registerReceiver(onConnectionChanged, onError, onPeerName)
        recreateGroup(onReady, onError, onPeerName, attempt = 0)
    }

    fun connect(
        credentials: P2pCredentials,
        onConnected: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasPermission()) {
            onError("Nearby Wi-Fi permission is required for Wi-Fi Direct")
            return
        }
        connectionCallbackSent = false
        registerReceiver({ formed ->
            if (formed && !connectionCallbackSent) {
                connectionCallbackSent = true
                manager.requestGroupInfo(channel) { group ->
                    val peerName = group?.owner?.deviceName
                        ?.takeIf { it.isNotBlank() }
                        ?: group?.clientList?.firstOrNull()?.deviceName
                            ?.takeIf { it.isNotBlank() }
                        ?: "Android device"
                    onConnected(credentials.groupOwnerAddress, peerName)
                }
            }
        }, onError)
        attemptConnect(credentials, onError, attempt = 0)
    }

    fun disconnect() {
        try {
            manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() = Unit
                override fun onFailure(reason: Int) = Unit
            })
        } catch (_: Exception) { }
        unregisterReceiver()
        connected = false
    }

    fun close() = disconnect()

    private fun attemptConnect(
        credentials: P2pCredentials,
        onError: (String) -> Unit,
        attempt: Int
    ) {
        if (attempt >= 8) {
            onError("Could not connect to the Wi-Fi Direct group. Please scan again.")
            return
        }
        try {
            val config = buildConfig(credentials)
            manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() = Unit
                override fun onFailure(reason: Int) {
                    // Android can report BUSY/ERROR while the receiver is
                    // finishing group formation. Retry before surfacing it.
                    handler.postDelayed({ attemptConnect(credentials, onError, attempt + 1) }, 750L)
                }
            })
        } catch (error: SecurityException) {
            onError("Android denied Wi-Fi Direct permission")
        } catch (error: IllegalArgumentException) {
            onError(error.message ?: "The Wi-Fi Direct QR data is invalid")
        } catch (error: IllegalStateException) {
            handler.postDelayed({ attemptConnect(credentials, onError, attempt + 1) }, 750L)
        }
    }

    private fun recreateGroup(
        onReady: (P2pCredentials) -> Unit,
        onError: (String) -> Unit,
        onPeerName: (String) -> Unit,
        attempt: Int
    ) {
        if (attempt >= 6) {
            onError("Wi-Fi Direct could not start. Turn Wi-Fi off and on, then retry.")
            return
        }
        try {
            // A previous screen may have left a group behind. Android reports
            // that stale group as a creation failure until it is removed.
            manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() = createGroup(onReady, onError, onPeerName, attempt)
                override fun onFailure(reason: Int) = createGroup(onReady, onError, onPeerName, attempt)
            })
        } catch (_: Exception) {
            createGroup(onReady, onError, onPeerName, attempt)
        }
    }

    private fun createGroup(
        onReady: (P2pCredentials) -> Unit,
        onError: (String) -> Unit,
        onPeerName: (String) -> Unit,
        attempt: Int
    ) {
        handler.postDelayed({
            try {
                manager.createGroup(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        waitForGroupInfo(onReady, onError)
                    }

                    override fun onFailure(reason: Int) {
                        handler.postDelayed({ recreateGroup(onReady, onError, onPeerName, attempt + 1) }, 500L)
                    }
                })
            } catch (error: SecurityException) {
                onError("Android denied Wi-Fi Direct permission")
            } catch (error: IllegalStateException) {
                handler.postDelayed({ recreateGroup(onReady, onError, onPeerName, attempt + 1) }, 500L)
            }
        }, 350L)
    }

    private fun waitForGroupInfo(
        onReady: (P2pCredentials) -> Unit,
        onError: (String) -> Unit,
        attempt: Int = 0
    ) {
        manager.requestGroupInfo(channel) { group ->
            if (group != null) {
                val networkName = reflectString(group, "getNetworkName")
                val passphrase = reflectString(group, "getPassphrase")
                manager.requestConnectionInfo(channel) { connectionInfo ->
                    onReady(
                        P2pCredentials(
                            networkName = networkName,
                            passphrase = passphrase,
                            groupOwnerAddress = connectionInfo?.groupOwnerAddress?.hostAddress
                                ?: "192.168.49.1",
                            ownerDeviceAddress = group.owner?.deviceAddress
                        )
                    )
                }
            } else if (attempt < 24) {
                handler.postDelayed({ waitForGroupInfo(onReady, onError, attempt + 1) }, 500L)
            } else {
                onError("Wi-Fi Direct group details were not available")
            }
        }
    }

    private fun buildConfig(credentials: P2pCredentials): WifiP2pConfig {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !credentials.networkName.isNullOrBlank() &&
            !credentials.passphrase.isNullOrBlank()
        ) {
            WifiP2pConfig.Builder()
                .setNetworkName(credentials.networkName!!)
                .setPassphrase(credentials.passphrase!!)
                .build()
        } else {
            WifiP2pConfig().apply {
                deviceAddress = credentials.ownerDeviceAddress
                    ?: throw IllegalArgumentException("The QR code has no Wi-Fi Direct device address")
                wps.setup = android.net.wifi.WpsInfo.PBC
            }
        }
    }

    private fun registerReceiver(
        onConnectionChanged: (Boolean) -> Unit,
        onError: (String) -> Unit,
        onPeerName: (String) -> Unit = {}
    ) {
        unregisterReceiver()
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                        val enabled = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1) ==
                            WifiP2pManager.WIFI_P2P_STATE_ENABLED
                        if (!enabled) onError("Turn on Wi-Fi Direct on this phone")
                    }
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        val info = intent.parcelable<WifiP2pInfo>(WifiP2pManager.EXTRA_WIFI_P2P_INFO)
                        val formed = info?.groupFormed == true
                        connected = formed
                        onConnectionChanged(formed)
                        if (formed) {
                            manager.requestGroupInfo(channel) { group ->
                                val peerName = group?.clientList?.firstOrNull()?.deviceName
                                    ?.takeIf { it.isNotBlank() }
                                    ?: group?.owner?.deviceName
                                        ?.takeIf { it.isNotBlank() }
                                    ?: "Android device"
                                onPeerName(peerName)
                            }
                        }
                    }
                }
            }
        }
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun unregisterReceiver() {
        receiver?.let { runCatching { appContext.unregisterReceiver(it) } }
        receiver = null
    }

    private fun hasPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun reflectString(target: Any, methodName: String): String? =
        runCatching { target.javaClass.getMethod(methodName).invoke(target) as? String }.getOrNull()
}

private inline fun <reified T> Intent.parcelable(key: String): T? {
    return if (Build.VERSION.SDK_INT >= 33) getParcelableExtra(key, T::class.java)
    else @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}
