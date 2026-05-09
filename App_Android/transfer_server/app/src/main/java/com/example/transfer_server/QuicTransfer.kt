package com.example.transfer_server

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import quicclient.Quicclient

object QuicTransfer {
    private const val TAG = "QUIC_DEBUG_CORE"

    suspend fun downloadFast(url: String, savePath: String, customMtu: Int): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "==========================================================================")
        Log.d(TAG, "[QUIC_START] KÍCH HOẠT LÕI GOMOBILE - GIAO THỨC HTTP/3 QUIC")
        Log.d(TAG, "[QUIC_INFO] URL Nguồn   : $url")
        Log.d(TAG, "[QUIC_INFO] Đích Lưu    : $savePath")
        Log.d(TAG, "[QUIC_INFO] MTU Ép Dòng : $customMtu bytes")
        Log.d(TAG, "==========================================================================")

        try {
            Log.d(TAG, "[QUIC_EXECUTE] Đang đẩy lệnh xuống tầng C/C++ của Go...")
            
            // Gomobile sẽ tự chuyển tham số Int của Kotlin thành Long để map với int của Go
            val result = Quicclient.downloadFast(url, savePath, customMtu.toLong())
            
            Log.d(TAG, "[QUIC_RESPONSE] Dữ liệu thô từ Go trả về: [$result]")

            // Lõi Go mới đúc nó trả về "QUIC_SUCCESS_MTU_xxxx" nên dùng startsWith
            if (result.startsWith("QUIC_SUCCESS")) {
                Log.d(TAG, "[QUIC_SUCCESS] 🚀 BOOM! TẢI XONG! TỐC ĐỘ BÀN THỜ LÀ ĐÂY!")
                Log.d(TAG, "==========================================================================")
                return@withContext true
            } else {
                Log.e(TAG, "[QUIC_ERROR] ❌ LỖI TỪ LÕI GO: $result")
                Log.d(TAG, "==========================================================================")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "[QUIC_FATAL] 💥 APP CRASH KHI GIAO TIẾP VỚI LÕI GO: ${e.message}")
            Log.d(TAG, "==========================================================================")
            return@withContext false
        }
    }
}
