package com.example.transfer_server

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.ChannelSftp
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

data class HuangFile(val name: String, val isDir: Boolean, val size: Long, val fullPath: String)

object SftpFallbackClient {
    private const val USER = "huang"
    private const val PASS = "huangzhi1"

    private fun getSession(ip: String): Session {
        val jsch = JSch()
        val session = jsch.getSession(USER, ip, 22)
        session.setPassword(PASS)
        val config = Properties()
        config["StrictHostKeyChecking"] = "no"
        session.setConfig(config)
        return session
    }

    fun listFiles(targetIp: String, targetPath: String): List<HuangFile> {
        val result = mutableListOf<HuangFile>()
        try {
            val session = getSession(targetIp)
            session.connect(5000)
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect(3000)
            val files = channel.ls(targetPath)
            for (entry in files) {
                val file = entry as ChannelSftp.LsEntry
                if (file.filename == "." || file.filename == "..") continue
                val path = if (targetPath.endsWith("/")) targetPath + file.filename else "$targetPath/${file.filename}"
                result.add(HuangFile(file.filename, file.attrs.isDir, file.attrs.size, path))
            }
            channel.disconnect()
            session.disconnect()
        } catch (e: Exception) { NetworkConfig.log("SFTP List Fail: ${e.message}") }
        return result
    }

    fun uploadFile(targetIp: String, remotePath: String, localFile: File): Boolean {
        return try {
            val session = getSession(targetIp)
            session.connect(5000)
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect(3000)
            FileInputStream(localFile).use { channel.put(it, remotePath) }
            channel.disconnect()
            session.disconnect()
            true
        } catch (e: Exception) { false }
    }

    fun downloadFile(targetIp: String, remotePath: String, localFile: File): Boolean {
        return try {
            val session = getSession(targetIp)
            session.connect(5000)
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect(3000)
            FileOutputStream(localFile).use { channel.get(remotePath, it) }
            channel.disconnect()
            session.disconnect()
            true
        } catch (e: Exception) { false }
    }

    fun makeDir(targetIp: String, path: String): Boolean {
        return try {
            val session = getSession(targetIp)
            session.connect(5000)
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()
            channel.mkdir(path)
            channel.disconnect()
            session.disconnect()
            true
        } catch (e: Exception) { false }
    }

    fun deleteItem(targetIp: String, path: String, isDir: Boolean): Boolean {
        return try {
            val session = getSession(targetIp)
            session.connect(5000)
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()
            if (isDir) channel.rmdir(path) else channel.rm(path)
            channel.disconnect()
            session.disconnect()
            true
        } catch (e: Exception) { false }
    }
}
