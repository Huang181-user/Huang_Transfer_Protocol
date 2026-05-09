package com.example.transfer_server

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException

class HuangDocumentsProvider : DocumentsProvider() {
    private val PROJ = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        DocumentsContract.Document.COLUMN_FLAGS,
        DocumentsContract.Document.COLUMN_SIZE
    )

    private fun getMimeType(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    // 🛠 VŨ KHÍ MỚI: Bẻ lái mạng từ tận gốc UI
    private fun applyNetworkBinding(ip: String) {
        val ctx = context ?: return
        if (ip == NetworkConfig.IP_LAN) {
            NetworkUtils.clearProcessBinding(ctx)
        } else {
            NetworkUtils.bindProcessToVpn(ctx)
        }
    }

    override fun onCreate() = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID, DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_TITLE, DocumentsContract.Root.COLUMN_DOCUMENT_ID
        ))
        val rootFlags = DocumentsContract.Root.FLAG_SUPPORTS_CREATE or DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD

        cursor.newRow().apply {
            add(DocumentsContract.Root.COLUMN_ROOT_ID, "huang_lan")
            add(DocumentsContract.Root.COLUMN_FLAGS, rootFlags)
            add(DocumentsContract.Root.COLUMN_TITLE, "Zhiserver (LAN)")
            add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, "lan::DIR::/mnt/HDD_merge")
        }

        cursor.newRow().apply {
            add(DocumentsContract.Root.COLUMN_ROOT_ID, "huang_ts")
            add(DocumentsContract.Root.COLUMN_FLAGS, rootFlags)
            add(DocumentsContract.Root.COLUMN_TITLE, "Zhiserver (Tailscale)")
            add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, "ts::DIR::/mnt/HDD_merge")
        }
        return cursor
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(projection ?: PROJ)
        val parts = documentId?.split("::") ?: return cursor
        val isDir = parts[1] == "DIR"
        val path = parts[2]
        val name = path.substringAfterLast("/")

        var flags = if (isDir) DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE else DocumentsContract.Document.FLAG_SUPPORTS_WRITE
        flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_DELETE

        cursor.newRow().apply {
            add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, documentId)
            add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, name)
            add(DocumentsContract.Document.COLUMN_MIME_TYPE, if (isDir) DocumentsContract.Document.MIME_TYPE_DIR else getMimeType(name))
            add(DocumentsContract.Document.COLUMN_FLAGS, flags)
            add(DocumentsContract.Document.COLUMN_SIZE, 0)
        }
        return cursor
    }

    override fun queryChildDocuments(parentDocumentId: String?, projection: Array<out String>?, sortOrder: String?): Cursor {
        val cursor = MatrixCursor(projection ?: PROJ)
        val parts = parentDocumentId?.split("::") ?: return cursor
        val ip = if (parts[0] == "lan") NetworkConfig.IP_LAN else NetworkConfig.IP_TS
        val path = parts[2]

        // 🎯 Đảm bảo mạng chuẩn trước khi SFTP quét file!
        applyNetworkBinding(ip)

        val files = UnifiedTransport.listFiles(ip, path)
        for (file in files) {
            val docId = "${parts[0]}::${if (file.isDir) "DIR" else "FILE"}::${file.fullPath}"
            var flags = if (file.isDir) DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE else DocumentsContract.Document.FLAG_SUPPORTS_WRITE
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_DELETE

            cursor.newRow().apply {
                add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, docId)
                add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.name)
                add(DocumentsContract.Document.COLUMN_MIME_TYPE, if (file.isDir) DocumentsContract.Document.MIME_TYPE_DIR else getMimeType(file.name))
                add(DocumentsContract.Document.COLUMN_FLAGS, flags)
                add(DocumentsContract.Document.COLUMN_SIZE, file.size)
            }
        }
        return cursor
    }

    override fun openDocument(documentId: String?, mode: String?, signal: CancellationSignal?): ParcelFileDescriptor {
        val parts = documentId?.split("::") ?: throw FileNotFoundException()
        val ip = if (parts[0] == "lan") NetworkConfig.IP_LAN else NetworkConfig.IP_TS
        val remotePath = parts[2]

        // 🎯 Bẻ lái mạng trước khi Download/Upload
        applyNetworkBinding(ip)

        val ctx = context ?: throw FileNotFoundException()
        val cacheFile = File(ctx.cacheDir, remotePath.substringAfterLast("/"))
        val pfdMode = ParcelFileDescriptor.parseMode(mode)

        if (mode?.contains("w") == true) {
            return ParcelFileDescriptor.open(cacheFile, pfdMode, Handler(Looper.getMainLooper())) {
                UnifiedTransport.upload(ip, remotePath, cacheFile, ctx)
            }
        } else {
            UnifiedTransport.download(ip, remotePath, cacheFile, ctx)
            return ParcelFileDescriptor.open(cacheFile, pfdMode)
        }
    }

    override fun createDocument(parentId: String?, mimeType: String?, displayName: String?): String? {
        val parts = parentId?.split("::") ?: return null
        val ip = if (parts[0] == "lan") NetworkConfig.IP_LAN else NetworkConfig.IP_TS
        val newPath = "${parts[2]}/$displayName"

        // 🎯 Bẻ lái mạng trước khi Tạo mới
        applyNetworkBinding(ip)

        if (DocumentsContract.Document.MIME_TYPE_DIR == mimeType) {
            if (UnifiedTransport.makeDir(ip, newPath)) return "${parts[0]}::DIR::$newPath"
        } else {
            val temp = File(context?.cacheDir, "new_file_tmp").apply { createNewFile() }
            UnifiedTransport.upload(ip, newPath, temp, context!!)
            return "${parts[0]}::FILE::$newPath"
        }
        return null
    }

    override fun deleteDocument(documentId: String?) {
        val parts = documentId?.split("::") ?: return
        val ip = if (parts[0] == "lan") NetworkConfig.IP_LAN else NetworkConfig.IP_TS
        val path = parts[2]
        val isDir = parts[1] == "DIR"

        // 🎯 Bẻ lái mạng trước khi Xóa
        applyNetworkBinding(ip)

        UnifiedTransport.deleteItem(ip, path, isDir)
    }
}