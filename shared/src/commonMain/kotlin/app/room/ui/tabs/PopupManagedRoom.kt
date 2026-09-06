package app.room.ui.tabs

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import app.LocalRoomViewmodel
import app.i18n.strings
import app.protocol.WireMessage
import app.theme.Space
import app.theme.Type
import app.theme.palette
import app.uicomponents.controls.AccentAction
import app.uicomponents.controls.Field
import app.uicomponents.controls.SecondaryAction
import app.uicomponents.controls.Segmented
import app.uicomponents.controls.Text
import app.uicomponents.frames.Modal
import app.uicomponents.frames.ModalSize
import app.utils.generateRoomPassword
import syncplaymobile.shared.generated.resources.cancel
import syncplaymobile.shared.generated.resources.okay

/**
 * Managed rooms in one modal: a segmented choice between creating a room and identifying as
 * its operator, then the one field that choice needs. No chooser in front of it.
 */
@Composable
fun ManagedRoomModal() {
    val viewmodel = LocalRoomViewmodel.current
    val ui = viewmodel.uiState
    val open by ui.managedRoom.collectAsState()
    if (!open) return

    var create by remember { mutableStateOf(true) }
    var roomName by remember { mutableStateOf(viewmodel.session.currentRoom) }
    var password by remember { mutableStateOf("") }
    val input = if (create) roomName else password

    fun close() { ui.managedRoom.value = false }
    fun send() {
        close()
        val auth = if (create) {
            // Creating moves us to the minted room; the flag mutes the transition's own events.
            viewmodel.protocol.isRoomChanging = true
            WireMessage.controllerAuth(room = roomName, password = generateRoomPassword())
        } else {
            // Identifying stays in this room. The attempt is kept so a success can store it for
            // the re-identification every reconnect performs.
            val attempt = password.trim().uppercase()
            viewmodel.session.lastControlPasswordAttempt = attempt
            WireMessage.controllerAuth(room = viewmodel.session.currentRoom, password = attempt)
        }
        viewmodel.networkManager.sendAsync(auth)
    }

    Modal(
        open = true,
        onDismiss = ::close,
        title = strings.roomManagedRoom,
        size = ModalSize.Ask,
        actions = {
            SecondaryAction(strings.cancel, onClick = ::close)
            AccentAction(strings.okay, onClick = ::send, enabled = input.isNotBlank())
        },
    ) {
        Segmented(
            options = listOf(strings.roomOverflowCreateManagedRoom, strings.roomOverflowIdentifyAsOperator),
            selected = if (create) 0 else 1,
            onSelect = { create = it == 0 },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Space.gap))
        Text(
            text = if (create) strings.roomManagedRoomPopupCreate else strings.roomManagedRoomPopupPwIdentifyAsOperator,
            style = Type.note,
            color = palette.inkDim,
        )
        Spacer(Modifier.height(Space.gap))
        if (create) {
            Field(value = roomName, onValueChange = { roomName = it }, imeAction = ImeAction.Done, onImeAction = { if (roomName.isNotBlank()) send() }, name = strings.roomOverflowCreateManagedRoom)
        } else {
            Field(value = password, onValueChange = { password = it }, imeAction = ImeAction.Done, onImeAction = { if (password.isNotBlank()) send() }, name = strings.roomOverflowIdentifyAsOperator)
        }
    }
}
