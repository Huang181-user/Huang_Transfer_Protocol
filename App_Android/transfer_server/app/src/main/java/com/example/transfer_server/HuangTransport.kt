package com.example.transfer_server

import android.util.Log
import kotlinx.coroutines.runBlocking
import quicclient.Quicclient

object HuangTransport {
    private const val TAG = "HUANG_TRANSPORT"

    fun getStat(path: String): String = runBlocking {
        val ip = NetworkConfig.SERVER_IP
        val url = "https://$ip:4433/api/stat?path=$path"
        return@runBlocking Quicclient.fetchJson(url, NetworkConfig.QUIC_MTU.toLong())
    }

    fun listFiles(path: String): String = runBlocking {
        val ip = NetworkConfig.SERVER_IP
        val url = "https://$ip:4433/api/list?path=$path"
        return@runBlocking Quicclient.fetchJson(url, NetworkConfig.QUIC_MTU.toLong())
    }

    fun download(remotePath: String, localPath: String): Boolean {
        val url = "https://${NetworkConfig.SERVER_IP}:4433/download$remotePath"
        val res = Quicclient.downloadFast(url, localPath, NetworkConfig.QUIC_MTU.toLong())
        return res.startsWith("SUCCESS")
    }
}
