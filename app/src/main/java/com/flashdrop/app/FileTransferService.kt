package com.flashdrop.app

import android.content.ContentValues
import android.content.Context
import android.net.Network
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

data class FileTransferProgress(
    val filesSelected: Int,
    val totalBytes: Long,
    val filesTransferred: Int = 0,
    val bytesTransferred: Long = 0,
    val speedBytesPerSecond: Long = 0,
    val currentFile: String = "",
    val finished: Boolean = false,
    val files: List<TransferFileState> = emptyList(),
    val connectionDurationMillis: Long = 0L
)

enum class TransferFileStatus { PENDING, SENDING, SENT }

data class TransferFileState(
    val name: String,
    val size: Long,
    val mimeType: String,
    val status: TransferFileStatus,
    val bytesTransferred: Long = 0L
)

class FileTransferService(private val context: Context) {
    companion object {
        const val BUFFER_SIZE = 64 * 1024
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null

    fun startSender(
        files: List<Uri>,
        token: String,
        onReady: (port: Int) -> Unit,
        onProgress: (FileTransferProgress) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                val metadata = loadMetadata(files)
                serverSocket = ServerSocket(0)
                onReady(serverSocket!!.localPort)
                serverSocket!!.accept().use { socket ->
                    send(socket, token, metadata, onProgress)
                }
            } catch (error: Exception) {
                if (serverSocket?.isClosed != true) onError(error.message ?: "Sending failed")
            }
        }
    }

    fun startReceiver(
        token: String,
        onReady: (port: Int) -> Unit,
        onProgress: (FileTransferProgress) -> Unit,
        onError: (String) -> Unit
    ) {
        scope.launch {
            try {
                serverSocket = ServerSocket(0)
                onReady(serverSocket!!.localPort)
                serverSocket!!.accept().use { socket ->
                    val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
                    val incomingToken = input.readUTF()
                    if (incomingToken != token) throw IOException("The receiving QR code is invalid or expired")
                    receiveFiles(input, onProgress)
                }
            } catch (error: Exception) {
                if (serverSocket?.isClosed != true) onError(error.message ?: "Receiving failed")
            }
        }
    }

    suspend fun receive(
        network: Network?,
        host: String,
        port: Int,
        token: String,
        onProgress: (FileTransferProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        val socket = (network?.socketFactory?.createSocket() ?: Socket()).apply {
            connect(InetSocketAddress(host, port), 30_000)
        }
        socket.use {
            val input = DataInputStream(BufferedInputStream(it.getInputStream()))
            val output = DataOutputStream(BufferedOutputStream(it.getOutputStream()))
            output.writeUTF(token)
            output.flush()
            receiveFiles(input, onProgress)
        }
    }

    suspend fun sendTo(
        network: Network?,
        host: String,
        port: Int,
        token: String,
        files: List<Uri>,
        onProgress: (FileTransferProgress) -> Unit
    ) = withContext(Dispatchers.IO) {
        val metadata = loadMetadata(files)
        val socket = (network?.socketFactory?.createSocket() ?: Socket()).apply {
            connect(InetSocketAddress(host, port), 30_000)
        }
        socket.use { send(it, token, metadata, onProgress, waitForToken = false) }
    }

    fun stop() {
        try { serverSocket?.close() } catch (_: IOException) { }
        serverSocket = null
        scope.cancel()
    }

    private suspend fun send(
        socket: Socket,
        token: String,
        files: List<TransferMetadata>,
        onProgress: (FileTransferProgress) -> Unit,
        waitForToken: Boolean = true
    ) = withContext(Dispatchers.IO) {
        if (waitForToken) {
            val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
            val incomingToken = input.readUTF()
            if (incomingToken != token) throw IOException("The receiving QR code is invalid or expired")
        }
        val output = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
        if (!waitForToken) {
            output.writeUTF(token)
            output.flush()
        }
        output.writeInt(files.size)
        output.writeUTF(files.toJson())
        output.flush()
        var transferred = 0L
        val total = files.sumOf { it.size }
        val started = SystemClock.elapsedRealtime()
        val states = files.map { TransferFileState(it.name, it.size, it.mimeType, TransferFileStatus.PENDING) }.toMutableList()
        files.forEachIndexed { index, file ->
            states[index] = states[index].copy(status = TransferFileStatus.SENDING)
            onProgress(progress(files.size, total, index, transferred, file.name, started, states))
            context.contentResolver.openInputStream(file.uri)?.use { source ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val read = source.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    transferred += read
                    states[index] = states[index].copy(bytesTransferred = transferred - files.take(index).sumOf { it.size })
                    onProgress(progress(files.size, total, index, transferred, file.name, started, states))
                }
            } ?: throw IOException("Cannot open ${file.name}")
            output.flush()
            states[index] = states[index].copy(status = TransferFileStatus.SENT, bytesTransferred = file.size)
            onProgress(progress(files.size, total, index + 1, transferred, file.name, started, states))
        }
        onProgress(FileTransferProgress(files.size, total, files.size, transferred, 0, "", true, states.toList(), SystemClock.elapsedRealtime() - started))
    }

    private fun receiveFiles(
        input: DataInputStream,
        onProgress: (FileTransferProgress) -> Unit
    ) {
        val count = input.readInt()
        if (count !in 1..1000) throw IOException("Invalid file list")
        val metadataJson = input.readUTF()
        val metadata = parseMetadataJson(metadataJson, count)
        val total = metadata.sumOf { it.size }
        val destination = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
            "received"
        ).apply { mkdirs() }
        var transferred = 0L
        val started = SystemClock.elapsedRealtime()
        val states = metadata.map { TransferFileState(it.name, it.size, it.mimeType, TransferFileStatus.PENDING) }.toMutableList()
        metadata.forEachIndexed { index, file ->
            states[index] = states[index].copy(status = TransferFileStatus.SENDING)
            onProgress(progress(count, total, index, transferred, file.name, started, states))
            val outputTarget = openReceivedOutput(file.name, file.mimeType, destination)
            BufferedOutputStream(outputTarget.stream, BUFFER_SIZE).use { output ->
                var remaining = file.size
                val buffer = ByteArray(BUFFER_SIZE)
                while (remaining > 0) {
                    val read = input.read(buffer, 0, minOf(BUFFER_SIZE.toLong(), remaining).toInt())
                    if (read < 0) throw IOException("Connection ended during ${file.name}")
                    output.write(buffer, 0, read)
                    remaining -= read
                    transferred += read
                    states[index] = states[index].copy(bytesTransferred = file.size - remaining)
                    onProgress(progress(count, total, index, transferred, file.name, started, states))
                }
            }
            states[index] = states[index].copy(status = TransferFileStatus.SENT, bytesTransferred = file.size)
            onProgress(progress(count, total, index + 1, transferred, file.name, started, states))
            finishReceivedOutput(outputTarget)
        }
        onProgress(FileTransferProgress(count, total, count, transferred, 0, "", true, states.toList(), SystemClock.elapsedRealtime() - started))
    }

    private suspend fun loadMetadata(uris: List<Uri>): List<TransferMetadata> = withContext(Dispatchers.IO) {
        uris.mapIndexed { index, uri ->
            var name = "file-${index + 1}"
            var size = 0L
            context.contentResolver.query(uri, arrayOf("_display_name", "_size"), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex("_display_name")
                    val sizeIndex = cursor.getColumnIndex("_size")
                    if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
            if (size == 0L) size = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length.coerceAtLeast(0L) } ?: 0L
            TransferMetadata(name, size, context.contentResolver.getType(uri) ?: "application/octet-stream", uri)
        }
    }

    private fun progress(
        count: Int,
        total: Long,
        filesTransferred: Int,
        bytes: Long,
        current: String,
        started: Long,
        states: List<TransferFileState>
    ): FileTransferProgress {
        val elapsed = (SystemClock.elapsedRealtime() - started).coerceAtLeast(1L)
        return FileTransferProgress(count, total, filesTransferred, bytes, bytes * 1000L / elapsed, current, false, states.toList())
    }

    private fun List<TransferMetadata>.toJson(): String = JSONArray().apply {
        forEachIndexed { index, file ->
            put(JSONObject().apply {
                put("index", index)
                put("name", file.name)
                put("size", file.size)
                put("mimeType", file.mimeType)
            })
        }
    }.toString()

    private fun parseMetadataJson(value: String, expectedCount: Int): List<TransferMetadata> {
        val array = JSONArray(value)
        if (array.length() != expectedCount) throw IOException("Metadata count does not match file count")
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            TransferMetadata(
                item.getString("name"),
                item.getLong("size"),
                item.getString("mimeType"),
                Uri.EMPTY
            )
        }
    }

    private data class TransferMetadata(val name: String, val size: Long, val mimeType: String, val uri: Uri)

    private data class ReceivedOutput(
        val stream: OutputStream,
        val mediaUri: Uri?
    )

    private fun openReceivedOutput(
        name: String,
        mimeType: String,
        legacyDirectory: File
    ): ReceivedOutput {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = when {
                mimeType.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                mimeType.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, File(name).name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(collection, values)
                ?: throw IOException("Could not create a public destination for $name")
            val stream = context.contentResolver.openOutputStream(uri, "w")
                ?: throw IOException("Could not open the destination for $name")
            return ReceivedOutput(stream, uri)
        }

        val directory = when {
            mimeType.startsWith("image/") -> File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
                "FlashDrop"
            )
            mimeType.startsWith("video/") -> File(
                android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES),
                "FlashDrop"
            )
            else -> legacyDirectory
        }.apply { mkdirs() }
        return ReceivedOutput(FileOutputStream(uniqueFile(directory, name)), null)
    }

    private fun finishReceivedOutput(output: ReceivedOutput) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && output.mediaUri != null) {
            val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            context.contentResolver.update(output.mediaUri, values, null, null)
        }
    }

    private fun uniqueFile(directory: File, name: String): File {
        val safe = File(name).name.replace(Regex("[^A-Za-z0-9._ -]"), "_")
        var output = File(directory, safe)
        var index = 1
        while (output.exists()) output = File(directory, "${index++}-$safe")
        return output
    }
}
