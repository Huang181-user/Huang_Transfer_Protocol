package com.example.transfer_server

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.util.Log
import android.webkit.MimeTypeMap
import org.json.JSONArray
import java.io.File
import java.io.FileNotFoundException

class HuangDocumentsProvider : DocumentsProvider() {
    private val TAG = "HUANG_PROVIDER"
    private val COLUMNS = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_FLAGS
    )

    override fun onCreate() = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: arrayOf(DocumentsContract.Root.COLUMN_ROOT_ID, DocumentsContract.Root.COLUMN_TITLE, DocumentsContract.Root.COLUMN_DOCUMENT_ID, DocumentsContract.Root.COLUMN_FLAGS))
        cursor.newRow().apply {
            add(DocumentsContract.Root.COLUMN_ROOT_ID, "huang_root")
            add(DocumentsContract.Root.COLUMN_TITLE, "Huang Super Server")
            add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, NetworkConfig.ROOT_PATH)
            add(DocumentsContract.Root.COLUMN_FLAGS, DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD)
        }
        return cursor
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: COLUMNS)
        val path = documentId ?: NetworkConfig.ROOT_PATH
        cursor.newRow().apply {
            add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, path)
            add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, path.substringAfterLast("/"))
            add(DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR)
            add(DocumentsContract.Document.COLUMN_FLAGS, DocumentsContract.Document.FLAG_SUPPORTS_DELETE)
            add(DocumentsContract.Document.COLUMN_SIZE, 0L)
        }
        return cursor
    }

    override fun queryChildDocuments(parentDocId: String?, projection: Array<out String>?, sortOrder: String?): Cursor {
        val matrix = MatrixCursor(projection ?: COLUMNS)
        val json = HuangTransport.listFiles(parentDocId ?: NetworkConfig.ROOT_PATH)
        if (!json.startsWith("ERROR")) {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                matrix.newRow().apply {
                    add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, obj.getString("path"))
                    add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, obj.getString("name"))
                    add(DocumentsContract.Document.COLUMN_SIZE, obj.getLong("size"))
                    add(DocumentsContract.Document.COLUMN_MIME_TYPE, if(obj.getBoolean("is_dir")) DocumentsContract.Document.MIME_TYPE_DIR else getMimeType(obj.getString("name")))
                    add(DocumentsContract.Document.COLUMN_FLAGS, DocumentsContract.Document.FLAG_SUPPORTS_DELETE)
                }
            }
        }
        return matrix
    }

    override fun deleteDocument(documentId: String?) {
        documentId?.let { HuangTransport.delete(it) }
    }

    override fun openDocument(documentId: String?, mode: String?, signal: CancellationSignal?): ParcelFileDescriptor {
        val file = File(context?.cacheDir, documentId?.substringAfterLast("/") ?: "tmp")
        if (HuangTransport.download(documentId!!, file.absolutePath)) {
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
        throw FileNotFoundException()
    }

    private fun getMimeType(name: String): String {
        val ext = name.substringAfterLast(".", "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
}
