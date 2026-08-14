package app.discovery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A Syncplay server discovered on the local network via mDNS/Bonjour. */
data class LanServer(
    /** Human-readable service name (e.g. "Synkplay on Phone"). */
    val name: String,
    /** Resolved IPv4 host address. */
    val host: String,
    /** Server port. */
    val port: Int,
) {
    /** Convenience: "192.168.1.5:8999" — the form used in the server dropdown. */
    val address: String get() = "$host:$port"
}

/**
 * Platform-neutral LAN service discovery (mDNS / NSD).
 *
 * The platform [actual] owns the networking. Callers observe [discoveredServers]
 * and call [startDiscovery] / [stopDiscovery] to manage the browse lifecycle.
 */
expect class LanDiscovery() {
    /** Live list of servers found on the LAN, updated as services come and go. */
    val discoveredServers: StateFlow<List<LanServer>>

    /** Begin browsing for `_syncplay._tcp` services on the local network. */
    fun startDiscovery()

    /** Stop browsing and clear the discovered list. */
    fun stopDiscovery()
}

/** Registers this device's Syncplay server so other devices on the LAN can find it. */
expect class LanServiceAdvertiser() {
    /**
     * Publish a `_syncplay._tcp` service with [name] on [port].
     * Safe to call repeatedly; re-registers only when the name/port changes.
     */
    fun advertise(name: String, port: Int)

    /** Stop advertising and unregister the service, if any. */
    fun stop()
}

/** Deduplicates + merges a freshly-resolved server into a list, replacing by host:port. */
internal fun MutableList<LanServer>.mergeOrReplace(server: LanServer) {
    val idx = indexOfFirst { it.host == server.host && it.port == server.port }
    if (idx >= 0) this[idx] = server else add(server)
}

/** Removes a server by host:port (used when a service goes away). */
internal fun MutableList<LanServer>.removeByHostPort(host: String, port: Int) {
    removeAll { it.host == host && it.port == port }
}