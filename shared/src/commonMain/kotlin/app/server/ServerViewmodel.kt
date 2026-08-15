package app.server

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.Screen
import app.discovery.LanServiceAdvertiser
import app.server.model.ServerConfig
import app.server.network.ServerNetworkEngine
import app.utils.getDeviceIpAddress
import app.utils.loggy
import app.utils.platformCallback
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the server hosting screen: owns server lifecycle, editable config, and the
 * observable state the UI binds to. Independent of [app.room.RoomViewmodel] and client state.
 *
 * ## Lifecycle note
 *
 * The server runs in a **companion-object [CoroutineScope]** ([serverProcessScope]) so it
 * survives ViewModel recreation (e.g. when the user navigates away and back). The scope is
 * only cancelled when the user explicitly presses Stop or when the platform's server service
 * (foreground service on Android) is destroyed.
 *
 * This means [onCleared] does NOT stop the server — the server outlives this ViewModel.
 * The UI simply re-attaches to the already-running server on re-entry.
 */
class ServerViewmodel(
    val backStack: MutableList<Screen>
) : ViewModel() {

    // --- Configuration (editable by UI before starting) ---
    val port = mutableStateOf(ServerConfig.DEFAULT_PORT.toString())
    val password = mutableStateOf("")
    val motd = mutableStateOf("")
    val isolateRooms = mutableStateOf(true)
    val disableChat = mutableStateOf(false)
    val disableReady = mutableStateOf(false)

    // --- Server state ---
    val serverStatus = MutableStateFlow(ServerStatus.Stopped)
    val connectedClients = MutableStateFlow(0)
    /** Local/LAN IP — companion-backed so it persists across VM recreation. */
    val deviceIpAddress get() = _deviceIpAddress
    /** Public IP — companion-backed so it persists across VM recreation. */
    val publicIpAddress get() = _publicIpAddress
    /** Whether the public IP is still being fetched — companion-backed. */
    val publicIpLoading get() = _publicIpLoading

    /** Server event log entries for UI display. */
    val serverLogs = mutableStateListOf<ServerLogEntry>()

    // --- Re-attach to an already-running server ---

    init {
        // If the server is already running in the companion scope (e.g. user
        // navigated away and came back, or the service restarted this ViewModel),
        // re-attach our state flows to the live server.
        if (isServerRunning) {
            attachToRunningServer()
        }

        // Observe companion-level server lifecycle so that an external stop
        // (e.g. the notification Stop button calling stopServerFromCompanion) is
        // reflected in this instance's UI state without needing to recreate the VM.
        viewModelScope.launch {
            isServerRunningFlow.collect { running ->
                if (!running) {
                    if (serverStatus.value == ServerStatus.Running ||
                        serverStatus.value == ServerStatus.Starting) {
                        serverStatus.value = ServerStatus.Stopped
                        connectedClients.value = 0
                        deviceIpAddress.value = null
                        serverIpAddress = null
                        publicIpAddress.value = null
                        publicIpLoading.value = false
                        addLog("Server stopped")
                    }
                }
            }
        }
    }

    fun startServer() {
        if (serverStatus.value == ServerStatus.Running) return

        val portInt = port.value.toIntOrNull()
        if (portInt == null || portInt !in 1..65535) {
            addLog("Invalid port number")
            serverStatus.value = ServerStatus.Error
            return
        }

        val config = ServerConfig(
            port = portInt,
            password = password.value,
            isolateRooms = isolateRooms.value,
            disableReady = disableReady.value,
            disableChat = disableChat.value,
            motd = motd.value
        )

        serverStatus.value = ServerStatus.Starting

        // Launch the server in the companion-object scope so it survives
        // ViewModel clearing (navigation pop, Activity recreation, etc.)
        serverProcessScope.launch(Dispatchers.IO) {
            try {
                // Cancel any previous server instance
                _server?.shutdown()
                _engine?.stop()

                val server = SyncplayServer(config, serverProcessScope)
                _server = server
                _isServerRunning.value = true

                launch {
                    server.serverLog.collect { entries ->
                        for (entry in entries.drop(serverLogs.size)) {
                            serverLogs.add(entry)
                        }
                    }
                }

                launch {
                    server.connectedClients.collect { count ->
                        connectedClients.value = count
                    }
                }

                val engine = ServerNetworkEngine(server, serverProcessScope)
                _engine = engine

                engine.startListening(portInt)
                serverStatus.value = ServerStatus.Running
                val ip = getDeviceIpAddress()
                deviceIpAddress.value = ip
                serverIpAddress = ip
                addLog("Server started on port $portInt")

                // Advertise on the LAN so other devices can auto-discover us.
                try {
                    val adv = LanServiceAdvertiser()
                    _advertiser = adv
                    adv.advertise("Synkplay (${ip ?: "localhost"})", portInt)
                    addLog("Advertising on LAN via mDNS")
                } catch (e: Exception) {
                    loggy("Server: mDNS advertise failed: ${e.message}")
                }

                launch {
                    publicIpLoading.value = true
                    publicIpAddress.value = try {
                        val client = HttpClient()
                        val ip = client.get("https://api.ipify.org").bodyAsText().trim()
                        client.close()
                        ip
                    } catch (_: Exception) { null }
                    publicIpLoading.value = false
                }
                platformCallback.serverServiceStart(portInt)
            } catch (e: Exception) {
                loggy("Server: Failed to start: ${e.stackTraceToString()}")
                addLog("Failed to start: ${e.message}")
                serverStatus.value = ServerStatus.Error
                _isServerRunning.value = false
            }
        }
    }

    fun stopServer() {
        // Stop through the platform callback first (stops foreground service on
        // Android), then tear down the server scope.
        platformCallback.serverServiceStop()

        serverProcessScope.launch(Dispatchers.IO) {
            try {
                _server?.shutdown()
                _engine?.stop()
                _server = null
                _engine = null
            } catch (e: Exception) {
                loggy("Server: Error stopping: ${e.message}")
            }
        }
        _advertiser?.stop()
        _advertiser = null

        // Cancel the process scope to clean up any lingering coroutines,
        // then recreate it so a future startServer() works.
        serverProcessScope.cancel()
        serverScopeJob = SupervisorJob()
        serverProcessScope = CoroutineScope(serverScopeJob + CoroutineName("ServerProcess"))

        _isServerRunning.value = false
        serverStatus.value = ServerStatus.Stopped
        connectedClients.value = 0
        deviceIpAddress.value = null
        serverIpAddress = null
        publicIpAddress.value = null
        publicIpLoading.value = false
        serverLogs.clear()
        addLog("Server stopped")
    }

    private fun attachToRunningServer() {
        val server = _server ?: return
        serverStatus.value = ServerStatus.Running

        viewModelScope.launch {
            server.serverLog.collect { entries ->
                for (entry in entries.drop(serverLogs.size)) {
                    serverLogs.add(entry)
                }
            }
        }
        viewModelScope.launch {
            server.connectedClients.collect { count ->
                connectedClients.value = count
            }
        }
    }

    private fun addLog(message: String) {
        serverLogs.add(
            ServerLogEntry(
                timestamp = app.utils.generateTimestampMillis(),
                message = message
            )
        )
    }

    /**
     * IMPORTANT: Does NOT stop the server. The server runs in [serverProcessScope]
     * which outlives this ViewModel. The server only stops when the user explicitly
     * presses Stop or when the platform's server service shuts down.
     */
    override fun onCleared() {
        super.onCleared()
        // No-op: the server outlives the ViewModel.
        // It's stopped by stopServer() or when the foreground service is destroyed.
    }

    companion object {
        /** Job backing [serverProcessScope]. Replaced on stop/restart. */
        @Volatile
        private var serverScopeJob = SupervisorJob()

        /**
         * Process-level scope for the server. Coroutines launched here survive
         * ViewModel clearing and Activity recreation. Cancelled only when the
         * user explicitly stops the server.
         */
        @Volatile
        var serverProcessScope = CoroutineScope(serverScopeJob + CoroutineName("ServerProcess"))
            private set

        /** Reference to the running server (null when stopped). */
        @Volatile
        private var _server: SyncplayServer? = null

        /** Reference to the running network engine (null when stopped). */
        @Volatile
        private var _engine: ServerNetworkEngine? = null

        /** Advertises the running server on the LAN via mDNS/Bonjour so other
         *  devices can auto-discover it in the server dropdown. */
        @Volatile
        private var _advertiser: LanServiceAdvertiser? = null

        /** Whether the server is currently running in [serverProcessScope].
         *  Backed by a [StateFlow] so live [ServerViewmodel] instances can
         *  observe external stops (e.g. notification Stop button) and
         *  reconcile their UI state. */
        private val _isServerRunning = MutableStateFlow(false)
        val isServerRunningFlow: StateFlow<Boolean> = _isServerRunning
        val isServerRunning: Boolean get() = _isServerRunning.value

        /** The LAN IP address the server is listening on (set on start, cleared on stop). */
        @Volatile
        var serverIpAddress: String? = null
            private set

        /** Companion-backed UI state so it survives ViewModel recreation
         *  (e.g. leaving and returning to the Host Server screen). */
        val _deviceIpAddress = mutableStateOf<String?>(null)
        val _publicIpAddress = mutableStateOf<String?>(null)
        val _publicIpLoading = mutableStateOf(false)

        /** Stops the server from outside a ViewModel (e.g. from notification action). */
        fun stopServerFromCompanion() {
            if (!isServerRunning) return
            serverProcessScope.launch(Dispatchers.IO) {
                try {
                    _server?.shutdown()
                    _engine?.stop()
                    _server = null
                    _engine = null
                } catch (e: Exception) {
                    loggy("Server: Error stopping from companion: ${e.message}")
                }
            }
            _advertiser?.stop()
            _advertiser = null
            serverProcessScope.cancel()
            serverScopeJob = SupervisorJob()
            serverProcessScope = CoroutineScope(serverScopeJob + CoroutineName("ServerProcess"))
            _isServerRunning.value = false
            serverIpAddress = null
            _deviceIpAddress.value = null
            _publicIpAddress.value = null
            _publicIpLoading.value = false
        }
    }
}

enum class ServerStatus {
    Stopped, Starting, Running, Error
}
