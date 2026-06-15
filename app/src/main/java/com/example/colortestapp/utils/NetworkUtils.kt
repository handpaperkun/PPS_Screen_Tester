package com.example.colortestapp.utils

import java.net.NetworkInterface
import java.net.Inet4Address

object NetworkUtils {
    /**
     * 获取设备的 Wi-Fi / 以太网 IPv4 地址，
     * 用于在 CalMAN 等校准软件中输入连接地址。
     */
    fun getLocalIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces()?.asSequence()
                ?.flatMap { it.inetAddresses.asSequence() }
                ?.firstOrNull { addr ->
                    addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress
                }
                ?.hostAddress ?: "127.0.0.1"
        } catch (e: Exception) {
            "127.0.0.1"
        }
    }
}
