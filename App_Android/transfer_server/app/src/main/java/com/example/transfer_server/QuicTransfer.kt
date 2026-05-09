package com.example.transfer_server

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

object QuicTransfer {
    fun downloadFast(ip: String, remotePath: String, localFile: File, context: Context): Boolean {
        val urlString = "https://zhiserver.tailc979c1.ts.net:4433${remotePath.replace(" ", "%20")}"
        val isLan = ip == NetworkConfig.IP_LAN

        Log.d("HUANG_SUPER_DEBUG", "==================================================")
        Log.d("HUANG_SUPER_DEBUG", "🚀 [BẮT ĐẦU DOWNLOAD] -> $urlString")

        // 1. SOI DNS: Xem máy đang ưu tiên IPv4 hay IPv6
        Log.d("HUANG_SUPER_DEBUG", "📡 SOI DNS (Máy đang thấy IP nào?):")
        try {
            val addresses = InetAddress.getAllByName("zhiserver.tailc979c1.ts.net")
            addresses.forEach { Log.d("HUANG_SUPER_DEBUG", "   👉 Resolve ra: ${it.hostAddress}") }
        } catch (e: Exception) {
            Log.d("HUANG_SUPER_DEBUG", "   ❌ Lỗi phân giải DNS: ${e.message}")
        }

        return try {
            val cronetEngine = CronetNativeClient.getEngine(context, isLan)
            val url = URL(urlString)

            Log.d("HUANG_SUPER_DEBUG", "⏳ Đang cấu hình Cronet HttpURLConnection...")
            val connection = cronetEngine.openConnection(url) as HttpURLConnection
//            connection.setRequestProperty("X-Chrome-QUIC-Only", "1")
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 30000

            Log.d("HUANG_SUPER_DEBUG", "⚡ Đang Bắt tay (Connect) với Server...")
            connection.connect()

            val responseCode = connection.responseCode
            Log.d("HUANG_SUPER_DEBUG", "🎯 HTTP Status Code: $responseCode")

            // 2. ÉP CUNG GIAO THỨC: Xem nó xài UDP/QUIC hay de xe về TCP/H2
            try {
                val negotiatedProtocol = connection.javaClass.getMethod("getNegotiatedProtocol").invoke(connection) as String
                Log.d("HUANG_SUPER_DEBUG", "🔥 GIAO THỨC CHÍNH THỨC SỬ DỤNG: $negotiatedProtocol")
            } catch (e: Exception) {
                Log.d("HUANG_SUPER_DEBUG", "⚠️ Không bóc được tên giao thức từ Cronet Wrapper.")
            }

            if (responseCode == 200) {
                Log.d("HUANG_SUPER_DEBUG", "📥 Đang kéo luồng dữ liệu (InputStream)...")
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(localFile)
                val buffer = ByteArray(if (NetworkConfig.QUIC_MTU > 0) NetworkConfig.QUIC_MTU else 1200)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                inputStream.close()
                outputStream.close()
                Log.d("HUANG_SUPER_DEBUG", "✅ [THÀNH CÔNG] File size: ${localFile.length()} bytes")
                Log.d("HUANG_SUPER_DEBUG", "==================================================")
                CronetNativeClient.stopNetLog()
                true
            } else {
                Log.e("HUANG_SUPER_DEBUG", "❌ [LỖI] Server từ chối HTTP $responseCode")
                Log.d("HUANG_SUPER_DEBUG", "==================================================")
                false
            }
        } catch (e: Exception) {
            Log.e("HUANG_SUPER_DEBUG", "💀 [CRASH NẶNG] KẾT NỐI SỤP ĐỔ!")
            Log.e("HUANG_SUPER_DEBUG", "   -> Tên Lỗi: ${e.javaClass.simpleName}")
            Log.e("HUANG_SUPER_DEBUG", "   -> Nguyên nhân: ${e.message}")
            // In thẳng StackTrace ra Logcat để ný dễ túm đầu dòng code gây lỗi
            e.printStackTrace()
            Log.d("HUANG_SUPER_DEBUG", "==================================================")
            false
        }
    }

    fun uploadFast(ip: String, remotePath: String, localFile: File, context: Context): Boolean {
        val urlString = "https://zhiserver.tailc979c1.ts.net:4433${remotePath.replace(" ", "%20")}"
        val isLan = ip == NetworkConfig.IP_LAN

        Log.d("HUANG_SUPER_DEBUG", "==================================================")
        Log.d("HUANG_SUPER_DEBUG", "🚀 [BẮT ĐẦU UPLOAD] -> $urlString")

        return try {
            val cronetEngine = CronetNativeClient.getEngine(context, isLan)
            val url = URL(urlString)

            val connection = cronetEngine.openConnection(url) as HttpURLConnection
//            connection.setRequestProperty("X-Chrome-QUIC-Only", "1")
            connection.doOutput = true
            connection.requestMethod = "PUT"
//            connection.setRequestProperty("Content-Type", "application/octet-stream")
            // Cắt chunk theo MTU để chống xé lẻ
            val mtu = if (NetworkConfig.QUIC_MTU > 0) NetworkConfig.QUIC_MTU else 1200
            connection.setChunkedStreamingMode(mtu)

            Log.d("HUANG_SUPER_DEBUG", "⚡ Đang Bắt tay (Connect) với Server...")
            connection.connect()

            Log.d("HUANG_SUPER_DEBUG", "📤 Đang đẩy luồng dữ liệu (OutputStream)...")
            val outputStream = connection.outputStream
            val inputStream = FileInputStream(localFile)
            val buffer = ByteArray(mtu)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            inputStream.close()
            outputStream.close()

            val responseCode = connection.responseCode
            Log.d("HUANG_SUPER_DEBUG", "🎯 HTTP Status Code: $responseCode")

            try {
                val negotiatedProtocol = connection.javaClass.getMethod("getNegotiatedProtocol").invoke(connection) as String
                Log.d("HUANG_SUPER_DEBUG", "🔥 GIAO THỨC CHÍNH THỨC SỬ DỤNG: $negotiatedProtocol")
            } catch (e: Exception) {}

            if (responseCode == 200) {
                Log.d("HUANG_SUPER_DEBUG", "✅ [THÀNH CÔNG] Upload xong!")
                Log.d("HUANG_SUPER_DEBUG", "==================================================")
                true
            } else {
                Log.e("HUANG_SUPER_DEBUG", "❌ [LỖI] Server từ chối HTTP $responseCode")
                false
            }
        } catch (e: Exception) {
            Log.e("HUANG_SUPER_DEBUG", "💀 [CRASH NẶNG] KẾT NỐI SỤP ĐỔ: ${e.message}")
            e.printStackTrace()
            Log.d("HUANG_SUPER_DEBUG", "==================================================")
            false
        }
    }
}