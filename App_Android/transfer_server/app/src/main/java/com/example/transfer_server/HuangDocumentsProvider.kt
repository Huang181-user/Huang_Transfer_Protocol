package com.example.transfer_server

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import org.json.JSONObject
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

    // 🛠️ NHÁT 1: Tự thân vận động nạp Két Sắt ngay khi Provider sống dậy
    override fun onCreate(): Boolean {
        context?.let {
            AppSecrets.init(it)
            NetworkConfig.ROOT_PATH = AppSecrets.ROOT_PATH
            NetworkConfig.LOCAL_IP = AppSecrets.LOCAL_IP
            NetworkConfig.TS_IP = AppSecrets.TS_IP

            // Nếu ný mở File Explorer trước khi mở App bóp cò, thì gán tạm IP LAN để xài
            if (NetworkConfig.SERVER_IP.isEmpty()) {
                NetworkConfig.SERVER_IP = AppSecrets.LOCAL_IP
            }
        }
        return true
    }

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
        // 🛠️ NHÁT 2: Vá lỗi cache rỗng của Android
        val path = if (documentId.isNullOrEmpty()) NetworkConfig.ROOT_PATH else documentId

        val json = HuangTransport.getStat(path)
        var size = 0L
        var name = path.substringAfterLast("/")
        var isDir = true

        if (!json.startsWith("ERROR") && json.isNotBlank() && json.trim() != "null") {
            val obj = JSONObject(json)
            size = obj.getLong("size")
            name = obj.getString("name")
            isDir = obj.getBoolean("is_dir")
        }

        cursor.newRow().apply {
            add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, path)
            add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, name)
            add(DocumentsContract.Document.COLUMN_MIME_TYPE, if(isDir) DocumentsContract.Document.MIME_TYPE_DIR else getMimeType(name))
            add(DocumentsContract.Document.COLUMN_SIZE, size)
            add(DocumentsContract.Document.COLUMN_FLAGS, DocumentsContract.Document.FLAG_SUPPORTS_DELETE)
        }
        return cursor
    }

    override fun queryChildDocuments(parentDocId: String?, projection: Array<out String>?, sortOrder: String?): Cursor {
        val matrix = MatrixCursor(projection ?: COLUMNS)
        // 🛠️ NHÁT 3: Tương tự, trị dứt điểm cái path rỗng do hệ điều hành ný lưu láo
        val path = if (parentDocId.isNullOrEmpty()) NetworkConfig.ROOT_PATH else parentDocId

        val json = HuangTransport.listFiles(path)

        // 🛠️ NHÁT 4: Chặn đứng lỗi Parse Crash nếu thư mục đang trống không có file nào ("null")
        if (!json.startsWith("ERROR") && json.isNotBlank() && json.trim() != "null") {
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