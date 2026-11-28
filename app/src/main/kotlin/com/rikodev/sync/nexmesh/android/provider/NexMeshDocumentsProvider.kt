package com.rikodev.sync.nexmesh.android.provider

import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.ProxyFileDescriptorCallback
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.system.ErrnoException
import android.system.OsConstants
import com.rikodev.sync.nexmesh.android.NexMeshApplication
import com.rikodev.sync.nexmesh.android.R
import kotlinx.coroutines.runBlocking
import java.io.FileNotFoundException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Android System-Level Live Virtual Storage DocumentsProvider!
 * Exposes connected Storage Phones directly inside Android's native Files app & SAF File Pickers.
 *
 * - Zero permanent file duplication on the Main Phone.
 * - API 26+: Uses StorageManager.openProxyFileDescriptor() for seekable, on-demand E2EE network streaming.
 * - API 24–25: Uses ParcelFileDescriptor.createReliablePipe() for live sequential E2EE streaming.
 * - Releases all stream/network resources automatically on file close.
 */
class NexMeshDocumentsProvider : DocumentsProvider() {

    private val proxyThread by lazy {
        HandlerThread("NexMeshDocProxy").apply { start() }
    }
    private val proxyHandler by lazy { Handler(proxyThread.looper) }
    private val rangeSeq = AtomicInteger(1)

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val cols = projection ?: DEFAULT_ROOT_PROJECTION
        val cursor = MatrixCursor(cols)
        val app = context?.applicationContext as? NexMeshApplication ?: return cursor
        val runtime = app.container.runtimeController
        val session = runtime.activeSession.value

        val row = cursor.newRow()
        row.add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID_PRIMARY)
        row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, DOC_ID_ROOT)
        row.add(DocumentsContract.Root.COLUMN_TITLE, "NexMesh")
        row.add(
            DocumentsContract.Root.COLUMN_SUMMARY,
            if (session != null) "Connected: ${session.peerDeviceName}" else "No Storage Phone connected"
        )
        row.add(DocumentsContract.Root.COLUMN_ICON, R.mipmap.ic_launcher)
        row.add(
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD
        )
        row.add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, runtime.remoteAvailableBytes.value)
        row.add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*")
        return cursor
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val cols = projection ?: DEFAULT_DOCUMENT_PROJECTION
        val cursor = MatrixCursor(cols)
        val app = context?.applicationContext as? NexMeshApplication ?: return cursor
        val runtime = app.container.runtimeController

        if (documentId == DOC_ID_ROOT) {
            val row = cursor.newRow()
            row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DOC_ID_ROOT)
            row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, runtime.activeSession.value?.peerDeviceName ?: "NexMesh Storage")
            row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR)
            row.add(DocumentsContract.Document.COLUMN_FLAGS, 0)
            row.add(DocumentsContract.Document.COLUMN_SIZE, null)
            row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, System.currentTimeMillis())
            return cursor
        }

        val session = runtime.activeSession.value ?: return cursor
        val host = runtime.activePeerHost.value ?: return cursor
        val port = runtime.activePeerPort.value

        val meta = runBlocking {
            app.container.controllerClient.getRemoteFileMeta(host, port, session.sessionId, documentId)
        } ?: return cursor

        val row = cursor.newRow()
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, meta.fileId)
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, meta.name)
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, meta.mimeType)
        row.add(DocumentsContract.Document.COLUMN_FLAGS, 0)
        row.add(DocumentsContract.Document.COLUMN_SIZE, if (meta.isDirectory) null else meta.sizeBytes)
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, meta.lastModifiedMs)
        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val cols = projection ?: DEFAULT_DOCUMENT_PROJECTION
        val cursor = MatrixCursor(cols)
        val app = context?.applicationContext as? NexMeshApplication ?: return cursor
        val runtime = app.container.runtimeController
        val session = runtime.activeSession.value ?: return cursor
        val host = runtime.activePeerHost.value ?: return cursor
        val port = runtime.activePeerPort.value

        val remoteParent = if (parentDocumentId == DOC_ID_ROOT) null else parentDocumentId
        val listing = runCatching {
            runBlocking {
                app.container.controllerClient.listRemoteVaultFiles(host, port, session.sessionId, remoteParent)
            }
        }.getOrNull() ?: return cursor

        for (item in listing.files) {
            val row = cursor.newRow()
            row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, item.fileId)
            row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, item.name)
            row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, item.mimeType)
            row.add(DocumentsContract.Document.COLUMN_FLAGS, 0)
            row.add(DocumentsContract.Document.COLUMN_SIZE, if (item.isDirectory) null else item.sizeBytes)
            row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, item.lastModifiedMs)
        }
        return cursor
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val app = context?.applicationContext as? NexMeshApplication
            ?: throw FileNotFoundException("NexMesh application context unavailable")
        val runtime = app.container.runtimeController
        val session = runtime.activeSession.value
            ?: throw FileNotFoundException("Storage Phone is offline or disconnected")
        val host = runtime.activePeerHost.value
            ?: throw FileNotFoundException("Storage Phone host address unavailable")
        val port = runtime.activePeerPort.value

        val meta = runBlocking {
            app.container.controllerClient.getRemoteFileMeta(host, port, session.sessionId, documentId)
        } ?: throw FileNotFoundException("Remote file is no longer available on Storage Phone")

        runtime.recordActivity("Streaming '${meta.name}' from ${session.peerDeviceName}")

        // API 26+: Seekable, on-demand live network streaming via StorageManager ProxyFileDescriptor (ZERO local copy!)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val storageManager = app.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val callback = object : ProxyFileDescriptorCallback() {
                override fun onGetSize(): Long = meta.sizeBytes

                override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
                    if (offset >= meta.sizeBytes) return 0
                    val toRead = minOf(size.toLong(), meta.sizeBytes - offset).toInt().coerceAtMost(256 * 1024)
                    return try {
                        val bytes = runBlocking {
                            app.container.controllerClient.readRemoteByteRange(
                                host = host,
                                port = port,
                                session = session,
                                fileId = documentId,
                                offset = offset,
                                length = toRead,
                                seq = rangeSeq.incrementAndGet()
                            )
                        }
                        app.container.bandwidthMeter.recordLanBytes(bytes.size.toLong())
                        System.arraycopy(bytes, 0, data, 0, bytes.size)
                        bytes.size
                    } catch (_: Exception) {
                        throw ErrnoException("onRead", OsConstants.EIO)
                    }
                }

                override fun onRelease() {
                    runtime.recordActivity("Closed live stream '${meta.name}'")
                }
            }
            return storageManager.openProxyFileDescriptor(
                ParcelFileDescriptor.MODE_READ_ONLY,
                callback,
                proxyHandler
            )
        }

        // API 24–25 Fallback: Reliable live pipe streaming directly from Storage Phone (ZERO local copy!)
        val pipe = ParcelFileDescriptor.createReliablePipe()
        val readEnd = pipe[0]
        val writeEnd = pipe[1]
        proxyHandler.post {
            runCatching {
                ParcelFileDescriptor.AutoCloseOutputStream(writeEnd).use { out ->
                    runBlocking {
                        app.container.controllerClient.downloadRemoteVaultFile(
                            host = host,
                            port = port,
                            session = session,
                            fileId = documentId,
                            totalSizeBytes = meta.sizeBytes,
                            outputStream = out
                        )
                    }
                }
            }.onFailure {
                runCatching { writeEnd.closeWithError("Storage Phone disconnected during stream") }
            }
        }
        return readEnd
    }

    companion object {
        const val AUTHORITY = "com.rikodev.sync.nexmesh.documents"
        const val ROOT_ID_PRIMARY = "nexmesh_live_root"
        const val DOC_ID_ROOT = "root"

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
        )

        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE
        )
    }
}
