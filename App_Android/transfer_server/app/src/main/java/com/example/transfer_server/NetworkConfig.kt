package com.example.transfer_server

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object NetworkConfig {
    // Đã che IP thật để push GitHub không bị "lộ hàng"
    var LOCAL_IP by mutableStateOf("")
    var TS_IP by mutableStateOf("")
    var SERVER_IP by mutableStateOf("")
    var ROOT_PATH by mutableStateOf("")
    var QUIC_MTU by mutableStateOf(1200)
    var CURRENT_LOCALE by mutableStateOf("vi")

    fun getString(key: String): String {
        val strings = mapOf(
            "en" to mapOf("scan" to "Checking LAN...", "lan_ok" to "LAN OK", "lan_fail" to "LAN FAIL -> TAILSCALE", "confirm" to "CONFIRM", "net_error" to "Connection issue!", "fallback" to "DERP detected! Falling back to TCP"),
            "vi" to mapOf("scan" to "Đang quét LAN...", "lan_ok" to "NỘI BỘ THÔNG!", "lan_fail" to "NỘI BỘ TỊT! Chuyển Tailscale", "confirm" to "XÁC NHẬN", "net_error" to "Đường truyền có vấn đề!", "fallback" to "Gặp hầm DERP rồi! Hạ cấp TCP..."),
            "cn" to mapOf("scan" to "正在扫描...", "lan_ok" to "局域网在线", "lan_fail" to "切换至Tailscale", "confirm" to "确认", "net_error" to "网络连接故障!", "fallback" to "检测到转发模式! 切换至 TCP")
        )
        return strings[CURRENT_LOCALE]?.get(key) ?: key
    }
}
