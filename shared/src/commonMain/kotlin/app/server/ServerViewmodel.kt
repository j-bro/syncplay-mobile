package app.server

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.Screen
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
    val deviceIpAddress = mutableStateOf<String?>(null)
    /** Public IP fetched from external service, or null if unavailable/still loading. */
    val publicIpAddress = mutableStateOf<String?>(null)
    val publicIpLoading = mutableStateOf(false)

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
                isServerRunning = true

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
                isServerRunning = false
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

        // Cancel the process scope to clean up any lingering coroutines,
        // then recreate it so a future startServer() works.
        serverProcessScope.cancel()
        serverScopeJob = SupervisorJob()
        serverProcessScope = CoroutineScope(serverScopeJob + CoroutineName("ServerProcess"))

        isServerRunning = false
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

        /** Whether the server is currently running in [serverProcessScope]. */
        @Volatile
        var isServerRunning: Boolean = false
            private set

        /** The LAN IP address the server is listening on (set on start, cleared on stop). */
        @Volatile
        var serverIpAddress: String? = null
            private set

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
            serverProcessScope.cancel()
            serverScopeJob = SupervisorJob()
            serverProcessScope = CoroutineScope(serverScopeJob + CoroutineName("ServerProcess"))
            isServerRunning = false
            serverIpAddress = null
        }
    }
}

enum class ServerStatus {
    Stopped, Starting, Running, Error
}
