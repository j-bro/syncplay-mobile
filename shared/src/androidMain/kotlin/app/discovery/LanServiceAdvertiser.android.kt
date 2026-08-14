package app.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import app.utils.contextObtainer

actual class LanServiceAdvertiser {
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var registeredName: String? = null
    private var registeredPort: Int = -1

    actual fun advertise(name: String, port: Int) {
        if (registrationListener != null && registeredName == name && registeredPort == port) {
            return // already advertising the same thing
        }
        // If something different is registered, tear it down first.
        if (registrationListener != null) {
            stop()
        }

        val ctx: Context = contextObtainer()
        val mgr = ctx.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
        nsdManager = mgr

        val info = NsdServiceInfo().apply {
            serviceName = name
            serviceType = LanDiscovery.SERVICE_TYPE
            this.port = port
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(svc: NsdServiceInfo) {
                registeredName = svc.serviceName
            }

            override fun onRegistrationFailed(svc: NsdServiceInfo, errorCode: Int) {
                registrationListener = null
            }

            override fun onUnregistrationFailed(svc: NsdServiceInfo, errorCode: Int) {}

            override fun onServiceUnregistered(svc: NsdServiceInfo) {
                registrationListener = null
            }
        }

        registrationListener = listener
        registeredName = name
        registeredPort = port
        try {
            mgr.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            registrationListener = null
        }
    }

    actual fun stop() {
        val mgr = nsdManager ?: return
        val listener = registrationListener ?: return
        registrationListener = null
        registeredName = null
        registeredPort = -1
        try {
            mgr.unregisterService(listener)
        } catch (_: Exception) {
            // listener may already be unregistered
        }
    }
}