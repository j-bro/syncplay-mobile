package app.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import app.utils.contextObtainer
import app.utils.loggy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class LanDiscovery {
    private val _servers = MutableStateFlow<List<LanServer>>(emptyList())
    actual val discoveredServers: StateFlow<List<LanServer>> = _servers.asStateFlow()

    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val resolving = mutableSetOf<String>() // service names currently resolving

    actual fun startDiscovery() {
        if (discoveryListener != null) return // already running
        val ctx: Context = contextObtainer()
        val mgr = ctx.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
        nsdManager = mgr

        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                discoveryListener = null
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                discoveryListener = null
            }

            override fun onDiscoveryStarted(serviceType: String?) {
                loggy("LanDiscovery: discovery started for $serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                discoveryListener = null
            }

            override fun onServiceFound(info: NsdServiceInfo) {
                val svcName = info.serviceName
                loggy("LanDiscovery: found '$svcName' type=${info.serviceType}")
                if (resolving.contains(svcName)) return
                resolving.add(svcName)
                try {
                    mgr.resolveService(info, object : NsdManager.ResolveListener {
                        override fun onServiceResolved(svc: NsdServiceInfo) {
                            val host = svc.host?.hostAddress
                            val port = svc.port
                            loggy("LanDiscovery: resolved '${svc.serviceName}' -> $host:$port")
                            if (host != null && port > 0) {
                                val server = LanServer(
                                    name = svc.serviceName ?: host,
                                    host = host,
                                    port = port,
                                )
                                synchronized(_servers) {
                                    _servers.value = _servers.value.toMutableList().apply {
                                        mergeOrReplace(server)
                                    }
                                }
                            }
                            resolving.remove(svcName)
                        }

                        override fun onResolveFailed(svcInfo: NsdServiceInfo, errorCode: Int) {
                            resolving.remove(svcInfo.serviceName)
                        }
                    })
                } catch (e: Exception) {
                    resolving.remove(svcName)
                }
            }

            override fun onServiceLost(info: NsdServiceInfo) {
                val name = info.serviceName
                resolving.remove(name)
                // We don't always have host:port for lost services; drop by name match.
                synchronized(_servers) {
                    _servers.value = _servers.value
                        .filterNot { it.name == name }
                }
            }
        }

        discoveryListener = listener
        try {
            mgr.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            discoveryListener = null
        }
    }

    actual fun stopDiscovery() {
        val mgr = nsdManager ?: return
        val listener = discoveryListener ?: return
        discoveryListener = null
        resolving.clear()
        try {
            mgr.stopServiceDiscovery(listener)
        } catch (_: Exception) {
            // listener may already be stopped
        }
        _servers.value = emptyList()
    }

    companion object {
        /** mDNS service type for Syncplay servers. */
        const val SERVICE_TYPE = "_syncplay._tcp."
    }
}