package com.example.transfer_server

import android.content.Context
import android.util.Log
import org.chromium.net.CronetEngine
import org.chromium.net.ExperimentalCronetEngine
import java.io.File

object CronetNativeClient {
    private var engine: CronetEngine? = null
    // BÍ QUYẾT: Lưu lại trí nhớ xem Engine hiện tại đang là của LAN hay Tailscale
    private var isEngineLanMode: Boolean? = null

    // Trong CronetNativeClient.kt

    fun getEngine(context: Context, isLan: Boolean): CronetEngine {
        // 1. Trói mạng
        if (!isLan) {
            NetworkUtils.bindProcessToVpn(context)
        } else {
            NetworkUtils.clearProcessBinding(context)
        }

        // 2. NẾU ĐÃ CÓ ENGINE VÀ ĐÚNG MẠNG THÌ XÀI LUÔN, KHÔNG BUILD LẠI NỮA!
        if (engine != null && isEngineLanMode == isLan) {
            Log.d("HUANG_SUPER_DEBUG", "♻️ Xài lại Cronet Engine cũ để tránh kẹt Cache!")
            return engine!!
        }

        // 3. NẾU PHẢI BUILD LẠI (Do đổi mạng hoặc null), THÌ PHẢI TẠO TÊN FOLDER MỚI!
        val currentMtu = 1200
        // Dùng timestamp để tạo thư mục mới mỗi lần build, tránh đụng hàng!
        val cacheDir = File(context.cacheDir, "cronet_cache_${System.currentTimeMillis()}")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        isEngineLanMode = isLan

        Log.d("HUANG_SUPER_DEBUG", "--------------------------------------------------")
        Log.d("HUANG_SUPER_DEBUG", "⚙️ ĐANG BUILD CRONET ENGINE MỚI (isLan = $isLan) - Cache: ${cacheDir.name}")

        val builder = ExperimentalCronetEngine.Builder(context)
            .enableQuic(true)
            .enableHttp2(false)
            .addQuicHint("zhiserver.tailc979c1.ts.net", 4433, 4433)
            .setStoragePath(cacheDir.absolutePath)
            .enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK, 10 * 1024 * 1024)

        // ... (Phần JSON giữ nguyên như cũ, đã sạch sẽ rồi) ...
        // ...

        val experimentalOptions = """
            {
                "HostResolverRules": {
                    "host_resolver_rules": "MAP zhiserver.tailc979c1.ts.net ${if (isLan) NetworkConfig.IP_LAN else NetworkConfig.IP_TS}"
                },
                 "QUIC": { 
                     "max_packet_length": 1200,
                     "race_cert_verification": false, 
                     "connection_options": "TIME,FORCE,CHLO" 
                 },
                "Http2": { "enable": false },
                "AsyncDNS": { "enable": true }
            }
        """.trimIndent()

        builder.setExperimentalOptions(experimentalOptions)

        engine = builder.build()

        val netLogFile = File(context.cacheDir, "cronet_netlog.json")
        engine?.startNetLogToFile(netLogFile.absolutePath, true)
        Log.d("HUANG_SUPER_DEBUG", "🎯 ENGINE ĐÃ SẴN SÀNG!")
        Log.d("HUANG_SUPER_DEBUG", "--------------------------------------------------")

        return engine!!
    }

    fun stopNetLog() {
        try {
            Log.d("HUANG_SUPER_DEBUG", "🛑 LƯU Ý: NetLog vẫn đang chạy ngầm để ghi chép!")
        } catch (e: Exception) {
            Log.e("HUANG_SUPER_DEBUG", "⚠️ Lỗi khi xử lý NetLog: ${e.message}")
        }
    }
}