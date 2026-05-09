package com.example.transfer_server

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

object NetworkUtils {
    // Hàm này sẽ trói CẢ TIẾN TRÌNH APP vào VPN
    fun bindProcessToVpn(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networks = cm.allNetworks

        Log.d("HUANG_SUPER_DEBUG", "==================================================")
        Log.d("HUANG_SUPER_DEBUG", "🔍 ĐANG TÌM MẠNG VPN (TAILSCALE)...")

        for (network in networks) {
            val caps = cm.getNetworkCapabilities(network)
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                // ĐÂY LÀ VŨ KHÍ TỐI THƯỢNG CỦA ANDROID
                val success = cm.bindProcessToNetwork(network)
                Log.d("HUANG_SUPER_DEBUG", "✅ [THÀNH CÔNG] ĐÃ ÉP TOÀN BỘ APP CHẠY VÀO VPN: $success")
                Log.d("HUANG_SUPER_DEBUG", "👉 Gói tin C++ giờ sẽ KHÔNG THỂ lách ra ngoài được nữa!")
                Log.d("HUANG_SUPER_DEBUG", "==================================================")
                return success
            }
        }

        Log.e("HUANG_SUPER_DEBUG", "❌ KHÔNG TÌM THẤY VPN. Đã bật Tailscale chưa?")
        Log.d("HUANG_SUPER_DEBUG", "==================================================")
        return false
    }

    // Hàm này để nhả mạng ra khi ný chuyển về tải file bằng LAN
    fun clearProcessBinding(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.bindProcessToNetwork(null)
        Log.d("HUANG_SUPER_DEBUG", "🔓 ĐÃ MỞ KHÓA MẠNG (APP TRỞ VỀ ĐƯỜNG ĐI MẶC ĐỊNH)")
    }
}