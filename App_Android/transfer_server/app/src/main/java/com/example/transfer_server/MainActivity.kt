package com.example.transfer_server

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.transfer_server.ui.theme.Transfer_serverTheme

// ... (giữ nguyên import)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // NẠP VŨ KHÍ BÍ MẬT TRƯỚC KHI LÊN GIAO DIỆN
        AppSecrets.init(this)

        // Gán IP mặc định lên giao diện
        NetworkConfig.LOCAL_IP = AppSecrets.LOCAL_IP
        NetworkConfig.TS_IP = AppSecrets.TS_IP
        NetworkConfig.ROOT_PATH = AppSecrets.ROOT_PATH // <--- Thêm dòng này vô!

        setContent {
            Transfer_serverTheme {
                Surface(modifier = Modifier.fillMaxSize()) { QuicSuperScreen() }
            }
        }
    }
}
// ... (giữ nguyên phần còn lại)

@Composable
fun QuicSuperScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var logMsg by remember { mutableStateOf("Ready to scan...") }

    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("HUANG QUIC SUPER PLUGIN", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(NetworkConfig.LOCAL_IP, { NetworkConfig.LOCAL_IP = it }, label = { Text("IP LAN") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(NetworkConfig.TS_IP, { NetworkConfig.TS_IP = it }, label = { Text("IP Tailscale") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(NetworkConfig.ROOT_PATH, { NetworkConfig.ROOT_PATH = it }, label = { Text("Server Path") }, modifier = Modifier.fillMaxWidth())

        Button(onClick = {
            scope.launch {
                logMsg = NetworkConfig.getString("scan")
                val isLocal = NetworkUtils.pingHost(NetworkConfig.LOCAL_IP)
                
                if (isLocal) {
                    NetworkConfig.SERVER_IP = NetworkConfig.LOCAL_IP
                    NetworkUtils.clearProcessBinding(context)
                    logMsg = NetworkConfig.getString("lan_ok")
                } else {
                    NetworkConfig.SERVER_IP = NetworkConfig.TS_IP
                    NetworkUtils.bindProcessToVpn(context)
                    logMsg = NetworkConfig.getString("lan_fail")
                }

                val probeUrl = "https://${NetworkConfig.SERVER_IP}:${AppSecrets.QUIC_PORT}/api/list?path=${NetworkConfig.ROOT_PATH}"
                NetworkConfig.QUIC_MTU = NetworkUtils.discoverBestMtu(probeUrl, !isLocal)
                logMsg = "✅ IP: ${NetworkConfig.SERVER_IP} | MTU: ${NetworkConfig.QUIC_MTU}"
            }
        }, Modifier.padding(top = 16.dp)) {
            Text(NetworkConfig.getString("confirm"))
        }

        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.DarkGray)) {
            Text(logMsg, Modifier.padding(16.dp), color = Color.Cyan)
        }
    }
}
