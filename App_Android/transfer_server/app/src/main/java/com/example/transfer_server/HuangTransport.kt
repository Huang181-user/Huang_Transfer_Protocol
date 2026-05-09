package com.example.transfer_server

import android.util.Log
import kotlinx.coroutines.runBlocking
import quicclient.Quicclient

object HuangTransport {
    private const val TAG = "HUANG_TRANSPORT"

    fun listFiles(path: String): String = runBlocking {
        val ip = NetworkConfig.SERVER_IP
        Log.d(TAG, "[STRATEGY] 🚀 Thử dùng QUIC duyệt: $path")
        
        if (Quicclient.probeMTU("https://$ip:4433/api/list?path=/", 1200L)) {
            val url = "https://$ip:4433/api/list?path=$path"
            return@runBlocking Quicclient.fetchJson(url, NetworkConfig.QUIC_MTU.toLong())
        }
        
        Log.w(TAG, "[STRATEGY] ⚠️ QUIC nghẽn, chuyển sang TCP/SFTP...")
        return@runBlocking SftpFallbackClient.listFilesJson(ip, path)
    }

    fun delete(path: String) {
        val ip = NetworkConfig.SERVER_IP
        val url = "https://$ip:4433/api/delete?path=$path"
        Log.d(TAG, "[DELETE] 🗑️ Xóa: $path")
        Quicclient.fetchJson(url, NetworkConfig.QUIC_MTU.toLong())
    }

    fun download(remotePath: String, localPath: String): Boolean {
        val ip = NetworkConfig.SERVER_IP
        val url = "https://$ip:4433/download$remotePath"
        Log.d(TAG, "[DOWNLOAD] 📥 Tải: $url")
        val res = Quicclient.downloadFast(url, localPath, NetworkConfig.QUIC_MTU.toLong())
        return res.startsWith("SUCCESS")
    }
}
