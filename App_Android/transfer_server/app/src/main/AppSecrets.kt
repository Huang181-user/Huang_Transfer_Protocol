package com.example.transfer_server

import android.content.Context
import org.json.JSONObject

object AppSecrets {
    var LOCAL_IP = ""
    var TS_IP = ""
    var QUIC_PORT = ""
    var SFTP_USER = ""
    var SFTP_PASS = ""
    var SFTP_PORT = 22

    fun init(context: Context) {
        try {
            val jsonString = context.assets.open("config.json").bufferedReader().use { it.readText() }
            val obj = JSONObject(jsonString)
            LOCAL_IP = obj.getString("local_ip")
            TS_IP = obj.getString("ts_ip")
            QUIC_PORT = obj.getString("quic_port")
            SFTP_USER = obj.getString("sftp_user")
            SFTP_PASS = obj.getString("sftp_pass")
            SFTP_PORT = obj.getInt("sftp_port")
        } catch (e: Exception) {
            android.util.Log.e("SECRETS", "❌ Thiếu file config.json trong assets!")
        }
    }
}
