package com.example.transfer_server

import android.util.Log

object NetworkConfig {
    const val IP_LAN = "192.168.1.83"
    const val IP_TS = "100.125.141.48"
    const val MOUNT_PATH = "/mnt/HDD_merge"

    // Chuyển thành var để cập nhật động sau khi dò đường
    var QUIC_MTU = 1000
    
    fun log(msg: String) {
        Log.d("HUANG_DEBUG", ">>> $msg")
    }
}
