package com.example.domain.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit

data class DiscoveredServer(
    val name: String,
    val host: String,
    val port: Int,
    val fullUrl: String,
    val isVerifiedLetta: Boolean = false,
    val serverVersion: String? = null
)

class LanDiscoveryService(private val context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())
    val discoveredServers: StateFlow<List<DiscoveredServer>> = _discoveredServers

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true
        _discoveredServers.value = emptyList()

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val host = serviceInfo.host?.hostAddress ?: return
                        val port = serviceInfo.port
                        val name = serviceInfo.serviceName ?: "Letta Server"
                        val url = "http://$host:$port"

                        val current = _discoveredServers.value.toMutableList()
                        if (current.none { it.host == host && it.port == port }) {
                            current.add(
                                DiscoveredServer(
                                    name = name,
                                    host = host,
                                    port = port,
                                    fullUrl = url
                                )
                            )
                            _discoveredServers.value = current
                        }
                    }
                })
            }

            override fun onServiceLost(service: NsdServiceInfo) {}

            override fun onDiscoveryStopped(serviceType: String) {
                _isScanning.value = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isScanning.value = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                _isScanning.value = false
            }
        }

        try {
            nsdManager?.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            _isScanning.value = false
        }
    }

    fun stopDiscovery() {
        try {
            discoveryListener?.let { nsdManager?.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            // Ignore if already stopped
        } finally {
            discoveryListener = null
            _isScanning.value = false
        }
    }

    /**
     * Probes common development/LAN endpoints like Android Emulator Host (10.0.2.2:8283)
     * and local subnet hosts on port 8283.
     */
    suspend fun quickSubnetProbe(): List<DiscoveredServer> = withContext(Dispatchers.IO) {
        val found = mutableListOf<DiscoveredServer>()
        val testClient = OkHttpClient.Builder()
            .connectTimeout(1500, TimeUnit.MILLISECONDS)
            .readTimeout(1500, TimeUnit.MILLISECONDS)
            .build()

        val candidates = listOf(
            "10.0.2.2", // Android Studio Emulator default host loopback
            "127.0.0.1",
            "localhost"
        )

        for (host in candidates) {
            val url = "http://$host:8283/v1/health"
            try {
                val req = Request.Builder().url(url).build()
                val resp = testClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    found.add(
                        DiscoveredServer(
                            name = "Local Dev Letta ($host:8283)",
                            host = host,
                            port = 8283,
                            fullUrl = "http://$host:8283",
                            isVerifiedLetta = true,
                            serverVersion = if (body.contains("version")) "Letta v1" else null
                        )
                    )
                }
            } catch (e: Exception) {
                // Not responding on this candidate
            }
        }

        val current = _discoveredServers.value.toMutableList()
        found.forEach { s ->
            if (current.none { it.host == s.host && it.port == s.port }) {
                current.add(s)
            }
        }
        _discoveredServers.value = current
        found
    }
}
