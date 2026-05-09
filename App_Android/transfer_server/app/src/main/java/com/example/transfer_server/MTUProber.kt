package com.example.transfer_server

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object MTUProber {
    private const val PROBE_PORT = 4434
    private val TEST_SIZES = intArrayOf(1200, 1280, 1350, 1400, 1450, 1470, 1480, 1490, 1500)

    fun autoScan(targetIp: String) {
        Thread {
            val socket = DatagramSocket()
            socket.soTimeout = 1000
            var maxPhysicalMtu = 1200

            Log.d("HUANG_DEBUG", ">>> 🛰️ KHỞI CHẠY RADAR ADAPTIVE CHO: $targetIp")

            // BƯỚC 1: Dò ngưỡng vật lý tối đa (Physical MTU)
            for (size in TEST_SIZES) {
                if (sendAndVerify(socket, targetIp, size)) {
                    maxPhysicalMtu = size
                } else {
                    Log.d("HUANG_DEBUG", ">>> 🛑 Chạm ngưỡng vật lý tại: $size")
                    break
                }
            }

            // BƯỚC 2: Tự động tính toán "Hiệu số hao hụt" (Adaptive Offset)
            // Tui dùng thuật toán: Nếu độ trễ (latency) của gói to vọt lên bất thường
            // so với gói nhỏ, nghĩa là gói đã bị xẻ thịt (Fragmentation).

            val smallLatency = measureLatency(socket, targetIp, 1000)
            val bigLatency = measureLatency(socket, targetIp, maxPhysicalMtu)

            // Nếu latency gói to gấp đôi gói nhỏ -> Chắc chắn bị xẻ thịt
            val isFragmented = bigLatency > (smallLatency * 1.8)

            // Tính toán QUIC_MTU "thông minh"
            NetworkConfig.QUIC_MTU = if (isFragmented || targetIp.startsWith("100.")) {
                // Nếu qua VPN hoặc bị xẻ thịt: Cắt lỗ sâu để bảo vệ gói QUIC
                Math.min(maxPhysicalMtu - 150, 1000)
            } else {
                // Nếu mạng LAN mượt mà: Trừ hao tối thiểu cho Header
                maxPhysicalMtu - 30
            }

            Log.d("HUANG_DEBUG", ">>> 🏆 KẾT QUẢ TỰ ĐỘNG: Physical=$maxPhysicalMtu, QUIC_MTU=${NetworkConfig.QUIC_MTU}")
            socket.close()
        }.start()
    }

    private fun sendAndVerify(socket: DatagramSocket, ip: String, size: Int): Boolean {
        return try {
            val buf = ByteArray(size)
            val packet = DatagramPacket(buf, buf.size, InetAddress.getByName(ip), PROBE_PORT)
            socket.send(packet)

            val recvBuf = ByteArray(64)
            val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
            socket.receive(recvPacket)
            String(recvPacket.data, 0, recvPacket.length).startsWith("RECV:$size")
        } catch (e: Exception) { false }
    }

    private fun measureLatency(socket: DatagramSocket, ip: String, size: Int): Long {
        val start = System.currentTimeMillis()
        return if (sendAndVerify(socket, ip, size)) System.currentTimeMillis() - start else 999
    }
}