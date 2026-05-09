package com.example.transfer_server

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import quicclient.Quicclient
import java.net.InetAddress

object NetworkUtils {
    private const val TAG = "HUANG_NET_UTILS"

    suspend fun pingHost(ip: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "[PING] 🔍 Kiểm tra sức khỏe IP: $ip")
            InetAddress.getByName(ip).isReachable(1500)
        } catch (e: Exception) { false }
    }

    suspend fun discoverBestMtu(targetUrl: String, isVpn: Boolean): Int = withContext(Dispatchers.IO) {
        var min = 1200
        var max = if (isVpn) 1280 else 1472
        var best = 1200
        Log.d(TAG, "==================================================")
        Log.d(TAG, "[MTU_SCAN] 🚀 BẮT ĐẦU CHẶT NHỊ PHÂN MTU")
        while (min <= max) {
            val mid = (min + max) / 2
            if (Quicclient.probeMTU(targetUrl, mid.toLong())) {
                Log.d(TAG, "[MTU_SCAN] ✅ $mid bytes: OK")
                best = mid
                min = mid + 1
            } else {
                Log.e(TAG, "[MTU_SCAN] ❌ $mid bytes: NGHẼN")
                max = mid - 1
            }
        }
        Log.d(TAG, "[MTU_SCAN] 🏆 MTU TỐT NHẤT: $best")
        return@withContext best
    }

    fun bindProcessToVpn(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.allNetworks.forEach { network ->
            if (cm.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                Log.d(TAG, "[VPN] 🔗 Đã ép app vào hầm Tailscale")
                return cm.bindProcessToNetwork(network)
            }
        }
        return false
    }

    fun clearProcessBinding(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.bindProcessToNetwork(null)
        Log.d(TAG, "[VPN] 🔓 Đã gỡ bỏ ràng buộc mạng")
    }
}
