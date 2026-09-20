package com.flashdrop.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private val InkBg = Color(0xFF07111F)
private val SurfaceColor = Color(0xFF0D1B2A)
private val Elevated = Color(0xFF13263A)
private val Fg = Color(0xFFE8F1F7)
private val Muted = Color(0xFF9FB1C1)
private val Subtle = Color(0xFF6D8294)
private val Accent = Color(0xFF38BDF8)
private val AccentFg = Color(0xFF04111C)
private val Volt = Color(0xFF2DD4BF)
private val BorderColor = Color(0xFF1E3A52)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent,
                    onPrimary = AccentFg,
                    background = InkBg,
                    surface = SurfaceColor,
                    onBackground = Fg,
                    onSurface = Fg
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = InkBg
                ) {
                    FlashDropRoot()
                }
            }
        }
    }
}

@Composable
private fun FlashDropRoot() {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1_900L)
        showSplash = false
    }

    if (showSplash) {
        FlashDropSplash()
    } else {
        FlashDropApp()
    }
}

@Composable
private fun FlashDropSplash() {
    val word = "FlashDrop"
    val letterProgress = remember { List(word.length) { Animatable(0f) } }

    LaunchedEffect(Unit) {
        letterProgress.forEachIndexed { index, animation ->
            launch {
                delay(index * 65L)
                animation.animateTo(1f, animationSpec = tween(durationMillis = 850))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBg),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            word.forEachIndexed { index, character ->
                val progress = letterProgress[index].value
                val startX = if (index % 2 == 0) -90 else 90
                val startY = when (index % 3) {
                    0 -> -58
                    1 -> 58
                    else -> -34
                }
                Text(
                    text = character.toString(),
                    color = if (index % 3 == 0) Accent else Volt,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset {
                        IntOffset(
                            ((1f - progress) * startX).roundToInt(),
                            ((1f - progress) * startY).roundToInt()
                        )
                    }
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("by openApp", color = Muted, fontSize = 12.sp)
        }
    }
}

private enum class Tab { Orbit, Files, Log }

private data class ConnectedDevice(
    val id: String,
    val name: String,
    val detail: String
)

private data class ReceiverAddress(
    val host: String,
    val port: Int,
    val token: String,
    val deviceId: String
)

private data class HotspotInvite(
    val credentials: HotspotCredentials,
    val p2pCredentials: P2pCredentials,
    val host: String,
    val port: Int,
    val token: String,
    val deviceId: String
)

private fun parseHotspotInvite(value: String): HotspotInvite? {
    return try {
        val uri = Uri.parse(value)
        if (uri.scheme != "flashdrop" || uri.host != "p2p") return null
        val ssid = uri.getQueryParameter("ssid") ?: return null
        val password = uri.getQueryParameter("password") ?: return null
        val host = uri.getQueryParameter("host") ?: return null
        val port = uri.getQueryParameter("port")?.toIntOrNull() ?: return null
        val token = uri.getQueryParameter("token") ?: return null
        val deviceAddress = uri.getQueryParameter("device")
        HotspotInvite(
            credentials = HotspotCredentials(ssid, password),
            p2pCredentials = P2pCredentials(ssid, password, host, deviceAddress),
            host = host,
            port = port,
            token = token,
            deviceId = deviceAddress ?: "sender"
        )
    } catch (_: Exception) {
        null
    }
}

private data class TransferFile(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val size: Long
)

private data class TransferProgress(
    val filesSelected: Int,
    val totalBytes: Long,
    val filesTransferred: Int = 0,
    val bytesTransferred: Long = 0,
    val speedBytesPerSecond: Long = 0,
    val currentFile: String = "",
    val finished: Boolean = false,
    val error: String? = null,
)

private fun localIpv4Address(): String? {
    return NetworkInterface.getNetworkInterfaces()?.toList()
        ?.flatMap { it.inetAddresses.toList() }
        ?.firstOrNull { address ->
            address is Inet4Address && !address.isLoopbackAddress && address.isSiteLocalAddress
        }
        ?.hostAddress
}

private fun makeQrBitmap(value: String, size: Int = 640): Bitmap {
    val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size)
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until size) {
            for (y in 0 until size) {
                setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
    }
}

private fun parseReceiverAddress(value: String): ReceiverAddress? {
    return try {
        val uri = Uri.parse(value)
        if (uri.scheme != "flashdrop" || uri.host != "receive") return null
        val host = uri.getQueryParameter("host") ?: return null
        val port = uri.getQueryParameter("port")?.toIntOrNull() ?: return null
        val token = uri.getQueryParameter("token") ?: return null
        val deviceId = uri.getQueryParameter("device") ?: "receiver"
        ReceiverAddress(host, port, token, deviceId)
    } catch (_: Exception) {
        null
    }
}

private class ReceiverTransferServer(
    private val context: Context,
    private val onReady: (String) -> Unit,
    private val onProgress: (TransferProgress) -> Unit,
    private val onError: (String) -> Unit
) {
    private val executor = Executors.newSingleThreadExecutor()
    private var serverSocket: ServerSocket? = null
    private val deviceId = UUID.randomUUID().toString()
    private val token = UUID.randomUUID().toString()

    fun start() {
        executor.execute {
            try {
                val host = localIpv4Address() ?: throw IOException("Connect both phones to the same Wi-Fi network")
                serverSocket = ServerSocket(0)
                val address = "flashdrop://receive?host=$host&port=${serverSocket!!.localPort}&token=$token&device=$deviceId"
                onReady(address)
                serverSocket!!.accept().use { socket -> receive(socket) }
            } catch (error: Exception) {
                if (serverSocket?.isClosed != true) onError(error.message ?: "Could not start receiving")
            }
        }
    }

    private fun receive(socket: Socket) {
        DataInputStream(BufferedInputStream(socket.getInputStream())).use { input ->
            val incomingToken = input.readUTF()
            if (incomingToken != token) throw IOException("This QR code is no longer valid")
            val count = input.readInt()
            if (count !in 1..1000) throw IOException("Invalid file list")
            val destination = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "received"
            ).apply { mkdirs() }
            var totalBytes = 0L
            val files = ArrayList<Triple<String, Long, String>>(count)
            repeat(count) {
                val name = File(input.readUTF()).name.ifBlank { "file-$it" }
                val size = input.readLong()
                val mimeType = input.readUTF()
                if (size < 0 || size > 10_000_000_000L) throw IOException("Invalid file size")
                totalBytes += size
                files += Triple(name, size, mimeType)
            }

            var transferredBytes = 0L
            val startedAt = SystemClock.elapsedRealtime()
            files.forEachIndexed { index, file ->
                val outputFile = uniqueFile(destination, file.first)
                BufferedOutputStream(outputFile.outputStream()).use { output ->
                    var remaining = file.second
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (remaining > 0) {
                        val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                        if (read < 0) throw IOException("Connection ended during transfer")
                        output.write(buffer, 0, read)
                        remaining -= read
                        transferredBytes += read
                        val elapsed = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(1L)
                        onProgress(
                            TransferProgress(
                                filesSelected = count,
                                totalBytes = totalBytes,
                                filesTransferred = index,
                                bytesTransferred = transferredBytes,
                                speedBytesPerSecond = transferredBytes * 1000L / elapsed,
                                currentFile = file.first
                            )
                        )
                    }
                }
                onProgress(
                    TransferProgress(
                        filesSelected = count,
                        totalBytes = totalBytes,
                        filesTransferred = index + 1,
                        bytesTransferred = transferredBytes,
                        currentFile = file.first
                    )
                )
            }
            onProgress(
                TransferProgress(
                    filesSelected = count,
                    totalBytes = totalBytes,
                    filesTransferred = count,
                    bytesTransferred = transferredBytes,
                    finished = true
                )
            )
        }
    }

    fun stop() {
        try { serverSocket?.close() } catch (_: IOException) { }
        executor.shutdownNow()
    }
}

private fun uniqueFile(directory: File, originalName: String): File {
    val safeName = originalName.replace(Regex("[^A-Za-z0-9._ -]"), "_")
    var candidate = File(directory, safeName)
    var index = 1
    while (candidate.exists()) {
        candidate = File(directory, "${index++}-$safeName")
    }
    return candidate
}

private suspend fun loadTransferFiles(context: Context, uris: List<Uri>): List<TransferFile> =
    withContext(Dispatchers.IO) {
        uris.mapIndexed { index, uri ->
            val resolver = context.contentResolver
            var name = "file-${index + 1}"
            var size = -1L
            resolver.query(uri, arrayOf("_display_name", "_size"), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex("_display_name")
                    val sizeIndex = cursor.getColumnIndex("_size")
                    if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
            if (size < 0) size = resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
            TransferFile(uri, name, resolver.getType(uri) ?: "application/octet-stream", size)
        }
    }

private suspend fun sendFiles(
    context: Context,
    address: ReceiverAddress,
    files: List<TransferFile>,
    onProgress: (TransferProgress) -> Unit
) = withContext(Dispatchers.IO) {
    Socket(address.host, address.port).use { socket ->
        socket.soTimeout = 30_000
        val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
        output.writeUTF(address.token)
        output.writeInt(files.size)
        files.forEach { file ->
            output.writeUTF(file.name)
            output.writeLong(file.size)
            output.writeUTF(file.mimeType)
        }
        output.flush()
        var transferredBytes = 0L
        val totalBytes = files.sumOf { it.size }
        val startedAt = SystemClock.elapsedRealtime()
        files.forEachIndexed { index, file ->
            context.contentResolver.openInputStream(file.uri)?.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    transferredBytes += read
                    val elapsed = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(1L)
                    onProgress(
                        TransferProgress(
                            filesSelected = files.size,
                            totalBytes = totalBytes,
                            filesTransferred = index,
                            bytesTransferred = transferredBytes,
                            speedBytesPerSecond = transferredBytes * 1000L / elapsed,
                            currentFile = file.name
                        )
                    )
                }
            } ?: throw IOException("Could not open ${file.name}")
            output.flush()
            onProgress(
                TransferProgress(
                    filesSelected = files.size,
                    totalBytes = totalBytes,
                    filesTransferred = index + 1,
                    bytesTransferred = transferredBytes,
                    currentFile = file.name
                )
            )
        }
        onProgress(
            TransferProgress(
                filesSelected = files.size,
                totalBytes = totalBytes,
                filesTransferred = files.size,
                bytesTransferred = transferredBytes,
                finished = true
            )
        )
    }
}

@Composable
private fun FlashDropApp() {
    var tab by remember { mutableStateOf(Tab.Orbit) }
    var connectedDevices by remember { mutableStateOf<List<ConnectedDevice>>(emptyList()) }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showSenderQr by remember { mutableStateOf(false) }
    var showSourceChooser by remember { mutableStateOf(false) }
    var hotspotInvite by remember { mutableStateOf<HotspotInvite?>(null) }
    var showReceiverTransfer by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val appContext = LocalContext.current
    val historyRepository = remember(appContext) { TransferHistoryRepository(appContext) }
    val historyScope = rememberCoroutineScope()
    val recentHistory by historyRepository.history.collectAsState(initial = emptyList())

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        selectedUris = uris
        if (uris.isEmpty()) {
            statusMessage = "No files selected."
        } else {
            tab = Tab.Files
            statusMessage = "${uris.size} file${if (uris.size == 1) "" else "s"} selected."
        }
    }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        selectedUris = uris
        if (uris.isEmpty()) {
            statusMessage = "No photos or videos selected."
        } else {
            tab = Tab.Files
            statusMessage = "${uris.size} photo/video${if (uris.size == 1) "" else "s"} selected."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        filePicker.launch(arrayOf("*/*"))
    }

    fun chooseFilesFromStorage() {
        val permissions = when {
            Build.VERSION.SDK_INT >= 33 -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }

    fun chooseGallery() {
        galleryPicker.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageAndVideo
            )
        )
    }

    fun chooseFiles() {
        showSourceChooser = true
    }

    if (showSourceChooser) {
        FileSourceChooser(
            onGallery = {
                showSourceChooser = false
                chooseGallery()
            },
            onFiles = {
                showSourceChooser = false
                chooseFilesFromStorage()
            },
            onCancel = { showSourceChooser = false }
        )
        return
    }

    if (showSenderQr) {
        HotspotReceiverQrScreen(onClose = { showSenderQr = false })
        return
    }

    if (showReceiverTransfer && hotspotInvite != null) {
        HotspotSenderTransferScreen(
            invite = hotspotInvite!!,
            selectedUris = selectedUris,
            onFinished = { completed, peerName ->
                showReceiverTransfer = false
                historyScope.launch {
                    historyRepository.record(completed, completed.connectionDurationMillis, peerName, "Sent to")
                }
                statusMessage = "Receive complete."
            },
            onCancel = {
                showReceiverTransfer = false
            }
        )
        return
    }

    if (showQrScanner) {
        QrScannerScreen(
            onQrScanned = { value ->
                val parsed = parseHotspotInvite(value)
                if (parsed == null) {
                    statusMessage = "That is not a FlashDrop hotspot QR code."
                } else {
                    hotspotInvite = parsed
                    connectedDevices = listOf(
                        ConnectedDevice(parsed.deviceId, "Sending phone", "Hotspot QR scanned")
                    )
                    showQrScanner = false
                    showReceiverTransfer = true
                }
            },
            onCancel = { showQrScanner = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("FD", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            when (tab) {
                Tab.Orbit -> OrbitScreenClean(
                    devices = connectedDevices,
                    recentDrops = recentHistory.take(3),
                    onChooseFiles = ::chooseFiles,
                    onSeeAll = { tab = Tab.Log },
                    onWaitReceive = {
                        showSenderQr = true
                    }
                )
                Tab.Files -> FilesPanel(
                    selectedCount = selectedUris.size,
                    onChooseFiles = ::chooseFiles,
                    onDone = { showQrScanner = true }
                )
                Tab.Log -> HistoryLogScreen(
                    repository = historyRepository,
                    body = "No completed transfers yet. History stays on this phone."
                )
            }
        }

        statusMessage?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = msg,
                color = Muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Elevated)
                    .padding(12.dp)
            )
        }

        Spacer(Modifier.height(10.dp))
        BottomDock(
            selected = tab,
            onSelect = {
                tab = it
                statusMessage = null
            }
        )
    }
}

@Composable
private fun OrbitScreenClean(
    devices: List<ConnectedDevice>,
    recentDrops: List<TransferHistory>,
    onChooseFiles: () -> Unit,
    onSeeAll: () -> Unit,
    onWaitReceive: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text("Private local transfer", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text("FlashDrop", color = Fg, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))

        if (devices.isNotEmpty()) {
            devices.forEach { device ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .background(Elevated)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(device.name, color = Fg, fontWeight = FontWeight.Medium)
                        Text(device.detail, color = Muted, fontSize = 12.sp)
                    }
                    Text("Connected", color = Volt, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
                .background(SurfaceColor)
                .padding(18.dp)
        ) {
            Button(
                onClick = onChooseFiles,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AccentFg)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(9.dp))
                Text("Send", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onWaitReceive,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AccentFg)
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(9.dp))
                Text("Receive", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(20.dp))
        RecentActivityFeed(recentDrops, onSeeAll)
    }
}

@Composable
private fun OrbitScreen(
    devices: List<ConnectedDevice>,
    recentDrops: List<TransferHistory>,
    onChooseFiles: () -> Unit,
    onSeeAll: () -> Unit,
    onWaitReceive: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text("Private local transfer", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text("FlashDrop", color = Fg, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))

        if (devices.isNotEmpty()) {
            devices.forEach { device ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .background(Elevated)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(device.name, color = Fg, fontWeight = FontWeight.Medium)
                        Text(device.detail, color = Muted, fontSize = 12.sp)
                    }
                    Text("Connected", color = Volt, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .background(SurfaceColor)
                .padding(16.dp)
        ) {
            Text("0 files · —", color = Fg, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (devices.isEmpty()) {
                    "Link a device with QR, then send over local Wi-Fi. The internet stays out of it."
                } else {
                    "To ${devices.first().name} over a local radio. The internet stays out of it."
                },
                color = Subtle,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onChooseFiles,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = AccentFg
                )
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Send", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onWaitReceive,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = AccentFg
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = AccentFg,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text("Receive", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(18.dp))
        RecentActivityFeed(recentDrops, onSeeAll)
    }
}

@Composable
private fun RecentActivityFeed(
    entries: List<TransferHistory>,
    onSeeAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Recent Drops", color = Fg, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onSeeAll) { Text("See All", color = Accent) }
    }
    if (entries.isEmpty()) {
        Text("Your completed transfers will appear here.", color = Muted, fontSize = 13.sp)
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 246.dp),
        userScrollEnabled = false
    ) {
        items(entries, key = { it.id }) { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceColor)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Share, contentDescription = null, tint = Accent) }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${entry.direction.safeDirection()} ${entry.peerDeviceName.safeDeviceName()}",
                        color = Fg,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        "${relativeTime(entry.timestamp)} • ${entry.totalFilesCount.coerceAtLeast(0)} files",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }
                Text(formatBytes(entry.totalBytesTransferred), color = Volt, fontSize = 12.sp)
            }
        }
    }
}

private fun String?.safeDeviceName(): String {
    val value = this?.trim().orEmpty()
    return if (value.isBlank() || value.equals("unknown", true) || value.equals("android device", true)) {
        "Android device"
    } else value.take(32)
}

private fun String?.safeDirection(): String = when (this?.trim()) {
    "Sent to" -> "Sent to"
    "Received from" -> "Received from"
    else -> "Transfer with"
}

private fun relativeTime(timestamp: Long): String {
    val age = (System.currentTimeMillis() - timestamp).coerceAtLeast(0L)
    val minutes = age / 60_000L
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min${if (minutes == 1L) "" else "s"} ago"
        minutes < 1_440 -> "${minutes / 60} hr${if (minutes / 60 == 1L) "" else "s"} ago"
        else -> "${minutes / 1_440} day${if (minutes / 1_440 == 1L) "" else "s"} ago"
    }
}

@Composable
private fun FileSourceChooser(
    onGallery: () -> Unit,
    onFiles: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Choose files", color = Fg, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Pick photos and videos from your gallery, or browse all files on this phone.",
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))

        SourceChoiceCard(
            icon = Icons.Default.PhotoLibrary,
            title = "Gallery",
            subtitle = "Select multiple photos and videos",
            onClick = onGallery
        )
        Spacer(Modifier.height(12.dp))
        SourceChoiceCard(
            icon = Icons.Default.Folder,
            title = "Files",
            subtitle = "Browse documents, archives, and all file types",
            onClick = onFiles
        )
        Spacer(Modifier.height(18.dp))
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel", color = Muted)
        }
    }
}

@Composable
private fun SourceChoiceCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .background(SurfaceColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = Fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FilesPanel(
    selectedCount: Int,
    onChooseFiles: () -> Unit,
    onDone: () -> Unit
) {
    Column {
        Text("Select only what leaves this phone", color = Muted, fontSize = 12.sp)
        Text("Files", color = Fg, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Text(
            if (selectedCount == 0) {
                "Choose documents, photos, videos, or any other files. You can select multiple files at once."
            } else {
                "$selectedCount file${if (selectedCount == 1) "" else "s"} selected"
            },
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .background(SurfaceColor)
                .padding(16.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onChooseFiles,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Fg),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Text("Choose files")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onDone,
            enabled = selectedCount > 0,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = AccentFg
            )
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun ReceiverQrScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var payload by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableStateOf<TransferProgress?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val server = remember {
        ReceiverTransferServer(
            context = context,
            onReady = { value -> mainHandler.post { payload = value } },
            onProgress = { value -> mainHandler.post { progress = value } },
            onError = { value -> mainHandler.post { error = value } }
        )
    }

    LaunchedEffect(Unit) { server.start() }
    DisposableEffect(Unit) { onDispose { server.stop() } }

    val qrBitmap = remember(payload) { payload?.let { makeQrBitmap(it) } }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Wait to receive", color = Fg, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                error != null -> error!!
                progress?.finished == true -> "Receive complete"
                progress != null -> "Receiving ${progress!!.currentFile}"
                payload != null -> "Scan this unique QR code from the sending phone"
                else -> "Creating a secure local connection..."
            },
            color = Muted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        if (qrBitmap != null && progress == null) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "FlashDrop receiving QR code",
                modifier = Modifier
                    .size(280.dp)
                    .background(Color.White)
                    .padding(12.dp)
            )
        } else if (progress != null) {
            TransferSummary(progress!!)
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onClose) {
            Text("Close")
        }
    }
}

@Composable
private fun TransferScreen(
    selectedUris: List<Uri>,
    receiverAddress: ReceiverAddress,
    onFinished: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var progress by remember {
        mutableStateOf(TransferProgress(filesSelected = selectedUris.size, totalBytes = 0))
    }

    LaunchedEffect(receiverAddress, selectedUris) {
        try {
            val files = loadTransferFiles(context, selectedUris)
            progress = TransferProgress(filesSelected = files.size, totalBytes = files.sumOf { it.size })
            sendFiles(context, receiverAddress, files) { value ->
                mainHandler.post {
                    progress = value
                }
            }
        } catch (error: Exception) {
            mainHandler.post { progress = progress.copy(error = error.message ?: "Transfer failed") }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Sending files", color = Muted, fontSize = 12.sp)
        Text("FlashDrop", color = Fg, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        TransferSummary(progress)
        if (progress.currentFile.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("Current: ${progress.currentFile}", color = Subtle, fontSize = 12.sp)
        }
        progress.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = Color(0xFFFF8A80), fontSize = 13.sp)
        }
        Spacer(Modifier.height(18.dp))
        if (progress.finished) {
            Button(
                onClick = onFinished,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AccentFg)
            ) { Text("Done") }
        } else {
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@Composable
private fun TransferSummary(progress: TransferProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .background(SurfaceColor)
            .padding(16.dp)
    ) {
        Text(
            "${progress.filesTransferred}/${progress.filesSelected} files transferred",
            color = Fg,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "${formatBytes(progress.bytesTransferred)} / ${formatBytes(progress.totalBytes)}",
            color = Muted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Speed: ${formatBytes(progress.speedBytesPerSecond)}/s  •  ETA: ${estimateTime(progress)}",
            color = Muted,
            fontSize = 13.sp
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return "%.1f %s".format(java.util.Locale.US, value, units[unit])
}

private fun estimateTime(progress: TransferProgress): String {
    if (progress.finished) return "done"
    if (progress.speedBytesPerSecond <= 0 || progress.totalBytes <= progress.bytesTransferred) return "calculating"
    val seconds = (progress.totalBytes - progress.bytesTransferred) / progress.speedBytesPerSecond
    return if (seconds < 60) "${seconds.coerceAtLeast(1)}s" else "${seconds / 60}m ${seconds % 60}s"
}

@Composable
private fun HotspotReceiverQrScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    val p2pManager = remember { WifiDirectManager(context) }
    val transferService = remember { FileTransferService(context) }
    val historyRepository = remember { TransferHistoryRepository(context) }
    val token = remember { UUID.randomUUID().toString() }
    var credentials by remember { mutableStateOf<P2pCredentials?>(null) }
    var host by remember { mutableStateOf<String?>(null) }
    var port by remember { mutableStateOf<Int?>(null) }
    var progress by remember { mutableStateOf<FileTransferProgress?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var groupWasFormed by remember { mutableStateOf(false) }
    var peerDeviceName by remember { mutableStateOf("Android device") }
    val permissions = if (Build.VERSION.SDK_INT >= 33) {
        arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else if (Build.VERSION.SDK_INT <= 28) {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    var permissionGranted by remember {
        mutableStateOf(
            permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result -> permissionGranted = result.values.all { it } }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(permissions)
    }
    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) return@LaunchedEffect
        p2pManager.startGroup(
            onReady = { value ->
                credentials = value
                host = value.groupOwnerAddress
                transferService.startReceiver(
                    token = token,
                    onReady = { valuePort -> mainHandler.post { port = valuePort } },
                    onProgress = { value ->
                        mainHandler.post {
                            progress = value
                        }
                    },
                    onError = { value -> mainHandler.post { error = value } }
                )
            },
            onConnectionChanged = { formed ->
                if (formed) {
                    groupWasFormed = true
                } else if (groupWasFormed && progress?.finished != true) {
                    error = "Wi-Fi Direct group disconnected"
                }
            },
            onPeerName = { value -> mainHandler.post { peerDeviceName = value } },
            onError = { error = it }
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            transferService.stop()
            p2pManager.disconnect()
        }
    }
    LaunchedEffect(progress?.finished) {
        val completed = progress
        if (completed?.finished == true) {
            historyRepository.record(completed, completed.connectionDurationMillis, peerDeviceName, "Received from")
        }
    }

    val qrPayload = remember(credentials, host, port) {
        if (credentials != null && host != null && port != null) {
            Uri.Builder()
                .scheme("flashdrop")
                .authority("p2p")
                .appendQueryParameter("ssid", credentials!!.networkName ?: "")
                .appendQueryParameter("password", credentials!!.passphrase ?: "")
                .appendQueryParameter("host", host!!)
                .appendQueryParameter("port", port.toString())
                .appendQueryParameter("token", token)
                .appendQueryParameter("device", credentials!!.ownerDeviceAddress ?: "")
                .build()
                .toString()
        } else null
    }
    val qrBitmap = remember(qrPayload) { qrPayload?.let { makeQrBitmap(it) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ready to receive", color = Fg, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                error != null -> error!!
                progress?.finished == true -> "Transfer complete"
                progress != null -> "Receiving ${progress!!.currentFile}"
                qrBitmap != null -> "Scan this QR code from the sending phone"
                else -> "Starting Wi-Fi Direct group..."
            },
            color = Muted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        if (qrBitmap != null && progress == null) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "FlashDrop Wi-Fi Direct QR code",
                modifier = Modifier
                    .size(280.dp)
                    .background(Color.White)
                    .padding(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("Waiting for the sending phone", color = Muted)
        } else if (progress != null) {
            CurrentFileCard(progress!!)
            Spacer(Modifier.height(12.dp))
            TransferQueue(progress!!.files)
            Spacer(Modifier.height(12.dp))
            HotspotTransferSummary(progress!!)
        }
        Spacer(Modifier.height(18.dp))
        if (progress?.finished == true) {
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = Volt, contentColor = AccentFg)
            ) { Text("Done") }
        } else {
            OutlinedButton(onClick = onClose) { Text("Close") }
        }
    }
}

@Composable
private fun HotspotSenderTransferScreen(
    invite: HotspotInvite,
    selectedUris: List<Uri>,
    onFinished: (FileTransferProgress, String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val connectionManager = remember { WifiDirectManager(context) }
    val transferService = remember { FileTransferService(context) }
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var permissionGranted by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<FileTransferProgress?>(null) }
    var status by remember { mutableStateOf("Requesting nearby Wi-Fi access...") }
    var error by remember { mutableStateOf<String?>(null) }
    var peerDeviceName by remember { mutableStateOf("Android device") }
    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.NEARBY_WIFI_DEVICES
    } else {
        Manifest.permission.ACCESS_FINE_LOCATION
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    LaunchedEffect(Unit) { permissionLauncher.launch(permission) }
    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) return@LaunchedEffect
        status = "Connecting to the receiver..."
        connectionManager.connect(
            credentials = invite.p2pCredentials,
            onConnected = { host, peerName ->
                peerDeviceName = peerName.safeDeviceName()
                status = "Connected. Sending files automatically..."
                scope.launch {
                    try {
                        transferService.sendTo(
                            network = null,
                            host = host,
                            port = invite.port,
                            token = invite.token,
                            files = selectedUris,
                            onProgress = { value ->
                                mainHandler.post {
                                    progress = value
                                }
                            }
                        )
                    } catch (transferError: Exception) {
                        mainHandler.post { error = transferError.message ?: "Receive failed" }
                    }
                }
            },
            onError = { error = it }
        )
    }
    DisposableEffect(Unit) {
        onDispose {
            connectionManager.disconnect()
            transferService.stop()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Sending files", color = Muted, fontSize = 12.sp)
        Text("FlashDrop", color = Fg, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        if (progress != null) {
            CurrentFileCard(progress!!)
            Spacer(Modifier.height(12.dp))
            TransferQueue(progress!!.files)
            Spacer(Modifier.height(12.dp))
            HotspotTransferSummary(progress!!)
        } else {
            Text(error ?: status, color = if (error == null) Muted else Color(0xFFFF8A80))
        }
        Spacer(Modifier.height(18.dp))
        if (progress?.finished == true) {
            Button(
                onClick = { progress?.let { onFinished(it, peerDeviceName.safeDeviceName()) } },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Volt, contentColor = AccentFg)
            ) { Text("Done") }
        } else {
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@Composable
private fun CurrentFileCard(progress: FileTransferProgress) {
    val current = progress.files.firstOrNull { it.status == TransferFileStatus.SENDING }
        ?: progress.files.lastOrNull { it.status == TransferFileStatus.SENT }
    if (current == null) return
    val fileProgress = if (current.size > 0) {
        (current.bytesTransferred.toFloat() / current.size).coerceIn(0f, 1f)
    } else 0f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .background(Elevated)
            .padding(16.dp)
    ) {
        Text(
            if (current.status == TransferFileStatus.SENT) "Last file sent" else "Currently sending",
            color = Muted,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Folder, contentDescription = null, tint = Accent) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(current.name, color = Fg, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(formatBytes(current.size), color = Muted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        androidx.compose.material3.LinearProgressIndicator(
            progress = { fileProgress },
            modifier = Modifier.fillMaxWidth(),
            color = Accent,
            trackColor = BorderColor
        )
        Spacer(Modifier.height(5.dp))
        Text("${(fileProgress * 100).toInt()}% of this file", color = Subtle, fontSize = 11.sp)
    }
}

@Composable
private fun TransferQueue(files: List<TransferFileState>) {
    if (files.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        Text("Transfer queue", color = Fg, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp),
            userScrollEnabled = true
        ) {
            items(files, key = { "${it.name}:${it.size}" }) { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Subtle, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(file.name, color = if (file.status == TransferFileStatus.PENDING) Muted else Fg, maxLines = 1, fontSize = 13.sp)
                        Text(
                            "${formatBytes(file.bytesTransferred)} / ${formatBytes(file.size)}",
                            color = Subtle,
                            fontSize = 11.sp
                        )
                    }
                    when (file.status) {
                        TransferFileStatus.SENT -> Icon(Icons.Default.CheckCircle, contentDescription = "Sent successfully", tint = Volt)
                        TransferFileStatus.SENDING -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Accent)
                        TransferFileStatus.PENDING -> Icon(Icons.Default.Schedule, contentDescription = "Pending", tint = Subtle)
                    }
                }
            }
        }
    }
}

@Composable
private fun HotspotTransferSummary(progress: FileTransferProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .background(SurfaceColor)
            .padding(16.dp)
    ) {
        Text("${progress.filesTransferred}/${progress.filesSelected} files transferred", color = Fg, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text("${formatBytes(progress.bytesTransferred)} / ${formatBytes(progress.totalBytes)}", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Speed: ${formatBytes(progress.speedBytesPerSecond)}/s  •  ETA: ${estimateHotspotTime(progress)}",
            color = Muted,
            fontSize = 13.sp
        )
    }
}

private fun estimateHotspotTime(progress: FileTransferProgress): String {
    if (progress.finished) return "done"
    if (progress.speedBytesPerSecond <= 0 || progress.totalBytes <= progress.bytesTransferred) return "calculating"
    val seconds = (progress.totalBytes - progress.bytesTransferred) / progress.speedBytesPerSecond
    return if (seconds < 60) "${seconds.coerceAtLeast(1)}s" else "${seconds / 60}m ${seconds % 60}s"
}

@Composable
private fun QrScannerScreen(
    onQrScanned: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    val latestOnQrScanned by rememberUpdatedState(onQrScanned)
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraGranted = granted }

    LaunchedEffect(Unit) {
        if (!cameraGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner, cameraGranted) {
        if (!cameraGranted) {
            onDispose { }
        } else {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val delivered = java.util.concurrent.atomic.AtomicBoolean(false)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                    } else {
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val value = barcodes.firstOrNull()?.rawValue
                                if (!value.isNullOrBlank() && delivered.compareAndSet(false, true)) {
                                    latestOnQrScanned(value)
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    }
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }, ContextCompat.getMainExecutor(context))

            onDispose {
                scanner.close()
                executor.shutdown()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (cameraGranted) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Scan the other phone's QR code", color = Color.White, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission is required to scan", color = Color.White)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Allow camera")
                }
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun HistoryLogScreen(
    repository: TransferHistoryRepository,
    body: String
) {
    val entries by repository.history.collectAsState(initial = emptyList())
    Column(Modifier.fillMaxWidth()) {
        Text("History Log", color = Fg, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text("Completed transfers saved on this phone", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        if (entries.isEmpty()) {
            SimplePanel("No transfers yet", "Local only", body)
        } else {
            entries.forEach { entry ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .background(SurfaceColor)
                        .padding(14.dp)
                ) {
                    Text(entry.peerDeviceName, color = Fg, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Connected for ${formatDuration(entry.connectionDurationMillis)} • ${entry.totalFilesCount} files • ${formatBytes(entry.totalBytesTransferred)}",
                        color = Muted,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(entry.timestamp)), color = Subtle, fontSize = 11.sp)
                }
            }
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = (milliseconds / 1000L).coerceAtLeast(0L)
    return if (seconds < 60) "${seconds}s" else "${seconds / 60}m ${seconds % 60}s"
}

@Composable
private fun SimplePanel(title: String, subtitle: String, body: String) {
    Column {
        Text(subtitle, color = Muted, fontSize = 12.sp)
        Text(title, color = Fg, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Text(
            body,
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .background(SurfaceColor)
                .padding(16.dp)
        )
    }
}

@Composable
private fun BottomDock(selected: Tab, onSelect: (Tab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .background(Elevated)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DockItem("Orbit", Icons.Default.Home, selected == Tab.Orbit) { onSelect(Tab.Orbit) }
        DockItem("Files", Icons.Default.Folder, selected == Tab.Files) { onSelect(Tab.Files) }
        DockItem("Log", Icons.AutoMirrored.Filled.List, selected == Tab.Log) { onSelect(Tab.Log) }
    }
}

@Composable
private fun DockItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SurfaceColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Fg else Muted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (selected) Fg else Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
