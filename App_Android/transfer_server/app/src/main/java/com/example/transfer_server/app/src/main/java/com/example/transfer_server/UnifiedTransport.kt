package com.example.transfer_server

import android.content.Context
import java.io.File

object UnifiedTransport {
    
    fun upload(ip: String, remotePath: String, localFile: File, context: Context): Boolean {
        NetworkConfig.log("🚀 Đang thử QUIC/UDP...")
        if (QuicTransfer.uploadFast(ip, remotePath, localFile, context)) {
            return true
        }
        NetworkConfig.log("⚠️ QUIC tịt, lùi về SFTP (TCP)...")
        return SftpFallbackClient.uploadFile(ip, remotePath, localFile)
    }

    fun download(ip: String, remotePath: String, localFile: File, context: Context): Boolean {
        NetworkConfig.log("🚀 Đang thử QUIC/UDP...")
        if (QuicTransfer.downloadFast(ip, remotePath, localFile, context)) {
            return true
        }
        NetworkConfig.log("⚠️ QUIC tịt, lùi về SFTP (TCP)...")
        return SftpFallbackClient.downloadFile(ip, remotePath, localFile)
    }

    fun listFiles(ip: String, path: String) = SftpFallbackClient.listFiles(ip, path)
    fun makeDir(ip: String, path: String) = SftpFallbackClient.makeDir(ip, path)
    fun deleteItem(ip: String, path: String, isDir: Boolean) = SftpFallbackClient.deleteItem(ip, path, isDir)
}
