package com.example.transfer_server

import android.util.Log
import kotlinx.coroutines.runBlocking
import quicclient.Quicclient

object HuangTransport {
    fun getStat(path: String): String = runBlocking {
        val url = "https://${NetworkConfig.SERVER_IP}:${AppSecrets.QUIC_PORT}/api/stat?path=$path"
        return@runBlocking Quicclient.fetchJson(url, NetworkConfig.QUIC_MTU.toLong())
    }

    fun listFiles(path: String): String = runBlocking {
        val url = "https://${NetworkConfig.SERVER_IP}:${AppSecrets.QUIC_PORT}/api/list?path=$path"
        return@runBlocking Quicclient.fetchJson(url, NetworkConfig.QUIC_MTU.toLong())
    }

    fun download(remotePath: String, localPath: String): Boolean {
        val url = "https://${NetworkConfig.SERVER_IP}:${AppSecrets.QUIC_PORT}/download$remotePath"
        return Quicclient.downloadFast(url, localPath, NetworkConfig.QUIC_MTU.toLong()).startsWith("SUCCESS")
    }
}
