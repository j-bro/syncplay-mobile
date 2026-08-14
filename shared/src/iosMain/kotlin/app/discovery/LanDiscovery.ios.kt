package app.discovery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class LanDiscovery {
    private val _servers = MutableStateFlow<List<LanServer>>(emptyList())
    actual val discoveredServers: StateFlow<List<LanServer>> = _servers.asStateFlow()

    actual fun startDiscovery() {
        // TODO: implement with NWBrowser (Network.framework Bonjour browsing)
    }

    actual fun stopDiscovery() {
        _servers.value = emptyList()
    }
}

actual class LanServiceAdvertiser {
    actual fun advertise(name: String, port: Int) {
        // TODO: implement with NWListener (Network.framework Bonjour advertising)
    }

    actual fun stop() {}
}