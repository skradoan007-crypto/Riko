package com.rikodev.ibd3d.android.files

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

/** Read-only bundled files, available in Android's system document picker. */
class ModelsProvider : DocumentsProvider() {
    private val rootColumns =
        arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_TITLE,
            Root.COLUMN_FLAGS,
            Root.COLUMN_MIME_TYPES,
        )
    private val docColumns =
        arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE,
        )
    private val models by lazy {
        val rows =
            requireNotNull(context).assets.open("catalog.json").bufferedReader().use {
                JSONObject(it.readText()).getJSONArray("items")
            }
        buildMap<String, String> {
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                if (row.has("modelAsset")) put(row.getString("modelAsset"), row.getString("name"))
            }
        }
    }

    override fun onCreate() = true

    override fun queryRoots(projection: Array<out String>?): Cursor =
        MatrixCursor(projection ?: rootColumns).apply {
            val values =
                mapOf(
                    Root.COLUMN_ROOT_ID to "models",
                    Root.COLUMN_DOCUMENT_ID to "root",
                    Root.COLUMN_TITLE to "IBD3D Panel",
                    Root.COLUMN_FLAGS to Root.FLAG_LOCAL_ONLY,
                    Root.COLUMN_MIME_TYPES to "application/octet-stream",
                )
            addRow(columnNames.map { values[it] }.toTypedArray())
        }

    private fun row(cursor: MatrixCursor, id: String) {
        require(id == "root" || id in models)
        val values =
            mapOf(
                Document.COLUMN_DOCUMENT_ID to id,
                Document.COLUMN_DISPLAY_NAME to
                    if (id == "root") "RGS Load Files" else "${models[id]}.glb",
                Document.COLUMN_MIME_TYPE to
                    if (id == "root") Document.MIME_TYPE_DIR else "application/octet-stream",
                Document.COLUMN_FLAGS to 0,
            )
        cursor.addRow(cursor.columnNames.map { values[it] }.toTypedArray())
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor =
        MatrixCursor(projection ?: docColumns).also { row(it, documentId) }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?,
    ): Cursor =
        MatrixCursor(projection ?: docColumns).also { cursor ->
            require(parentDocumentId == "root")
            models.keys.forEach { row(cursor, it) }
        }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor {
        require(mode == "r" && documentId in models)
        signal?.throwIfCanceled()
        val file = runBlocking { ModelFiles.bundled(requireNotNull(context), documentId) }
        signal?.throwIfCanceled()
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }
}
