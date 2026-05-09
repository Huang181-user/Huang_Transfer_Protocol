package com.example.transfer_server

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object NetworkConfig {
    var LOCAL_IP by mutableStateOf("192.168.1.83")
    var TS_IP by mutableStateOf("100.125.141.48")
    var SERVER_IP by mutableStateOf("192.168.1.83")
    var ROOT_PATH by mutableStateOf("/mnt/HDD_merge")
    var QUIC_MTU by mutableStateOf(1200)
    var CURRENT_LOCALE by mutableStateOf("vi")

    fun getString(key: String): String {
        val strings = mapOf(
            "en" to mapOf("scan" to "Checking LAN...", "lan_ok" to "LAN OK", "lan_fail" to "LAN FAIL -> TAILSCALE", "confirm" to "CONFIRM & DISCOVER"),
            "vi" to mapOf("scan" to "Đang quét LAN...", "lan_ok" to "NỘI BỘ THÔNG!", "lan_fail" to "NỘI BỘ TỊT! Chuyển Tailscale", "confirm" to "XÁC NHẬN & DÒ MTU"),
            "cn" to mapOf("scan" to "正在扫描...", "lan_ok" to "局域网在线", "lan_fail" to "切换至Tailscale", "confirm" to "确认")
        )
        return strings[CURRENT_LOCALE]?.get(key) ?: key
    }
}
