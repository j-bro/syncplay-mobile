package app.room

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/** Every change restarts the idle window, including an interaction with unchanged playback. */
internal data class HudAutoHideState(
    val idleSeconds: Int,
    val hudVisible: Boolean,
    val hasVideo: Boolean,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val held: Boolean,
    val activity: Long,
) {
    val playbackActive: Boolean get() = hasVideo && isPlaying && !isBuffering
}

/**
 * Only uninterrupted playback can hide the controls. Pausing or loading brings back controls
 * hidden by this timer; a deliberate background tap remains hidden until the user reveals it.
 */
internal suspend fun autoHideHud(
    states: Flow<HudAutoHideState>,
    setHudVisible: (Boolean) -> Unit,
) {
    var automaticallyHidden = false
    states.collectLatest { state ->
        if (state.hudVisible) automaticallyHidden = false
        if (!state.playbackActive) {
            if (automaticallyHidden) {
                automaticallyHidden = false
                setHudVisible(true)
            }
            return@collectLatest
        }
        if (state.idleSeconds <= 0 || !state.hudVisible || state.held) return@collectLatest
        delay(state.idleSeconds * 1000L)
        automaticallyHidden = true
        setHudVisible(false)
    }
}
