package app.room.ui.rightcards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import app.LocalRoomUiState
import app.LocalRoomViewmodel
import app.player.models.MediaFile
import app.preferences.Preferences.USER_INFO_VIEW
import app.preferences.set
import app.preferences.watchPref
import app.protocol.WireMessage
import app.protocol.models.User
import app.theme.Radius
import app.theme.Motion
import app.theme.Space
import app.theme.Type
import app.theme.palette
import app.uicomponents.controls.Chevron
import app.uicomponents.controls.ChevronDirection
import app.uicomponents.controls.Feedback
import app.uicomponents.controls.GlyphButton
import app.uicomponents.controls.Icon
import app.uicomponents.controls.ListRow
import app.uicomponents.controls.RowGap
import app.uicomponents.controls.Rule
import app.uicomponents.controls.SecondaryAction
import app.uicomponents.controls.Text
import app.uicomponents.frames.PanelFrame
import app.utils.FileComparison
import app.utils.timestampFromMillis
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import syncplaymobile.shared.generated.resources.Res
import syncplaymobile.shared.generated.resources.room_alone
import syncplaymobile.shared.generated.resources.room_card_title_user_info
import syncplaymobile.shared.generated.resources.room_file_different
import syncplaymobile.shared.generated.resources.room_file_has
import syncplaymobile.shared.generated.resources.room_file_none
import syncplaymobile.shared.generated.resources.room_file_same
import syncplaymobile.shared.generated.resources.room_roster_duration
import syncplaymobile.shared.generated.resources.room_roster_size
import syncplaymobile.shared.generated.resources.room_roster_view_compact
import syncplaymobile.shared.generated.resources.room_roster_view_standard
import syncplaymobile.shared.generated.resources.room_user_controller
import syncplaymobile.shared.generated.resources.room_user_details_hidden
import syncplaymobile.shared.generated.resources.room_user_details_shown
import syncplaymobile.shared.generated.resources.room_user_mute
import syncplaymobile.shared.generated.resources.room_user_not_ready_label
import syncplaymobile.shared.generated.resources.room_user_ready_label
import syncplaymobile.shared.generated.resources.room_user_report
import syncplaymobile.shared.generated.resources.room_user_set_not_ready
import syncplaymobile.shared.generated.resources.room_user_set_ready
import syncplaymobile.shared.generated.resources.room_user_unmute
import syncplaymobile.shared.generated.resources.room_user_you
import kotlin.math.roundToLong

object CardUserInfo {
    @Composable
    fun UserInfoCard(shape: Shape = Radius.panelShape) {
        val vm = LocalRoomViewmodel.current
        val ui = LocalRoomUiState.current
        val users by vm.session.userList.collectAsState()
        val myFile by vm.playerManager.media.collectAsState()
        val viewKey by USER_INFO_VIEW.watchPref()
        val scope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current
        val me = vm.session.currentUsername
        val canSetReady = users.any { it.name == me && it.isController }
        UserRosterPanel(
            users = users,
            me = me,
            myFile = myFile,
            // Saved "files" and unknown values naturally fall back to the full-information view.
            compact = viewKey == "compact",
            onCompactChange = { compact -> scope.launch { USER_INFO_VIEW.set(if (compact) "compact" else "standard") } },
            mutedUsers = ui.mutedUsers,
            onToggleMute = ui::toggleMute,
            onReport = { uriHandler.openUri(reportUserUrl(it)) },
            onSetReady = if (canSetReady) { user ->
                vm.networkManager.sendAsync(WireMessage.readiness(!user.readiness, manuallyInitiated = true, username = user.name))
            } else null,
            shape = shape,
        )
    }
}

/** The same roster in two densities. Information and row actions have independent visibility. */
@Composable
internal fun UserRosterPanel(
    users: List<User>,
    me: String,
    myFile: MediaFile?,
    compact: Boolean,
    onCompactChange: (Boolean) -> Unit,
    mutedUsers: Set<String> = emptySet(),
    onToggleMute: (String) -> Unit = {},
    onReport: (String) -> Unit = {},
    onSetReady: ((User) -> Unit)? = null,
    shape: Shape = Radius.panelShape,
) {
    var selectedUser by remember(compact) { mutableStateOf<String?>(null) }
    PanelFrame(
        title = stringResource(Res.string.room_card_title_user_info),
        modifier = Modifier.fillMaxSize(),
        shape = shape,
        scrollable = false,
        actions = { RosterViewSwitcher(compact, onCompactChange) },
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (users.size <= 1) {
                Text(stringResource(Res.string.room_alone), color = palette.inkDim,
                    modifier = Modifier.padding(horizontal = Space.gutter, vertical = Space.gapTight))
            }
            users.forEach { user ->
                key(user.name) {
                    val isSelf = user.name == me
                    val selected = selectedUser == user.name
                    RosterUserRow(
                        user = user, isSelf = isSelf, myFile = myFile, compact = compact,
                        expanded = selected,
                        onClick = if (compact || !isSelf) ({ selectedUser = if (selected) null else user.name }) else null,
                    )
                    if (selected && !isSelf) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(start = Space.gutter, end = Space.gutter, bottom = Space.gapTight),
                            horizontalArrangement = Arrangement.spacedBy(Space.gapTight),
                        ) {
                            SecondaryAction(stringResource(if (user.name in mutedUsers) Res.string.room_user_unmute else Res.string.room_user_mute), { onToggleMute(user.name) })
                            SecondaryAction(stringResource(Res.string.room_user_report), { onReport(user.name) })
                            if (onSetReady != null) {
                                SecondaryAction(stringResource(if (user.readiness) Res.string.room_user_set_not_ready else Res.string.room_user_set_ready), { onSetReady(user) })
                            }
                        }
                    }
                    Rule(Modifier.padding(horizontal = Space.gutter))
                }
            }
        }
    }
}

/** The original small header switch: one icon, two densities, no duplicate room count. */
@Composable
private fun RosterViewSwitcher(compact: Boolean, onCompactChange: (Boolean) -> Unit) {
    AnimatedContent(
        targetState = compact,
        transitionSpec = {
            (scaleIn(Motion.move(), initialScale = 0.6f) + fadeIn(Motion.move()))
                .togetherWith(scaleOut(Motion.quick(), targetScale = 0.6f) + fadeOut(Motion.quick()))
        },
        label = "rosterView",
    ) { isCompact ->
        val current = stringResource(if (isCompact) Res.string.room_roster_view_compact else Res.string.room_roster_view_standard)
        GlyphButton(
            icon = if (isCompact) Icons.Filled.ViewCompact else Icons.AutoMirrored.Filled.ViewList,
            name = stringResource(if (isCompact) Res.string.room_roster_view_standard else Res.string.room_roster_view_compact),
            modifier = Modifier.semantics { stateDescription = current },
            tint = palette.accent,
            onClick = { Feedback.tick(); onCompactChange(!compact) },
        )
    }
}

@Composable
private fun RosterUserRow(user: User, isSelf: Boolean, myFile: MediaFile?, compact: Boolean, expanded: Boolean, onClick: (() -> Unit)?) {
    val p = palette
    val file = user.file
    val myDuration = myFile?.fileDuration?.takeIf { it.isFinite() && it > 0.0 }
    val peerDuration = file?.fileDuration?.takeIf { it.isFinite() && it > 0.0 }
    val sameFile = file != null && myFile != null && FileComparison.sameFilename(myFile.fileName, file.fileName) &&
        FileComparison.sameFilesize(myFile.fileSize, file.fileSize) &&
        (myDuration == null || peerDuration == null || FileComparison.sameFileduration(myDuration, peerDuration))
    val fileState = stringResource(when {
        file == null -> Res.string.room_file_none
        myFile == null -> Res.string.room_file_has
        sameFile -> Res.string.room_file_same
        else -> Res.string.room_file_different
    })
    val ready = stringResource(if (user.readiness) Res.string.room_user_ready_label else Res.string.room_user_not_ready_label, user.name)
    val controller = if (user.isController) stringResource(Res.string.room_user_controller) else ""
    val self = if (isSelf) stringResource(Res.string.room_user_you) else ""
    val filename = file?.fileName ?: stringResource(Res.string.room_file_none)
    val expansion = if (onClick != null) stringResource(if (expanded) Res.string.room_user_details_shown else Res.string.room_user_details_hidden) else ""
    val state = listOf(ready, self, controller, fileState, expansion).filter { it.isNotEmpty() }.joinToString(", ")
    ListRow(
        modifier = Modifier.background(if (isSelf) p.ink.copy(alpha = 0.045f) else Color.Transparent)
            .semantics(mergeDescendants = true) { stateDescription = state },
        onClick = onClick,
        selected = user.isController,
        minHeight = Space.touchMin,
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = Space.gapTight), verticalArrangement = Arrangement.spacedBy(Space.gapTight)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                // At enlarged text, the compact row becomes two lines before either column gets squeezed.
                val stacked = maxWidth < 260.dp * LocalDensity.current.fontScale
                val nameWidth = maxWidth * 0.36f
                val identity: @Composable () -> Unit = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ReadinessDot(user.readiness)
                        RowGap(Space.gapTight + 2.dp)
                        FullRosterText(user.name, modifier = Modifier.weight(1f), style = Type.label, preferredLines = 1)
                        if (user.isController) {
                            RowGap(Space.gapTight)
                            Icon(Icons.Filled.Star, contentDescription = controller, tint = p.accent, modifier = Modifier.size(12.dp))
                        }
                    }
                }
                if (compact && !stacked) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(nameWidth)) { identity() }
                        RowGap(Space.gap)
                        CompactFilename(filename, file != null, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.gapTight)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { identity() }
                            if (isSelf && !compact) {
                                RowGap(Space.gap)
                                Text(self, style = Type.value, color = p.inkDim)
                            }
                            if (onClick != null && !compact) {
                                RowGap(Space.gap)
                                Chevron(if (expanded) ChevronDirection.Up else ChevronDirection.Down, size = 12.dp)
                            }
                        }
                        if (compact) CompactFilename(filename, file != null, Modifier.fillMaxWidth().padding(start = 15.dp))
                    }
                }
            }
            if (!compact || expanded) {
                val detailSize = lerp(Type.group.fontSize, Type.note.fontSize, 0.5f)
                val filenameStyle = Type.note.copy(fontSize = detailSize, lineHeight = lerp(Type.group.lineHeight, Type.note.lineHeight, 0.5f))
                val metadataStyle = Type.value.copy(fontSize = detailSize, lineHeight = lerp(Type.group.lineHeight, Type.value.lineHeight, 0.5f))
                Column(Modifier.padding(start = 15.dp), verticalArrangement = Arrangement.spacedBy(Space.gapTight)) {
                    FullRosterText(filename, style = filenameStyle)
                    if (file != null) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.gap), verticalArrangement = Arrangement.spacedBy(Space.gapTight)) {
                            val duration = peerDuration?.let { timestampFromMillis((it * 1000).toLong()) } ?: "—"
                            Text(stringResource(Res.string.room_roster_duration, duration), style = metadataStyle, color = p.inkDim)
                            Text(stringResource(Res.string.room_roster_size, rosterFileSize(file.fileSize)), style = metadataStyle, color = p.inkDim)
                            if (!isSelf && myFile != null) {
                                Text(fileState, style = metadataStyle, color = if (sameFile) p.ok else p.warn)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadinessDot(ready: Boolean) {
    val color = if (ready) palette.ok else palette.bad
    Box(Modifier.size(7.dp).border(Space.hair, color, CircleShape)
        .background(if (ready) color else color.copy(alpha = 0.2f), CircleShape))
}

@Composable
private fun CompactFilename(filename: String, hasFile: Boolean, modifier: Modifier) {
    val cleaned = remember(filename, hasFile) { if (hasFile) compactRosterFileName(filename) else filename }
    val measurer = rememberTextMeasurer()
    val style = Type.note
    val minSize = Type.group.fontSize
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val width = with(density) { maxWidth.roundToPx() }.coerceAtLeast(0)
        val (display, fontSize) = remember(cleaned, width, density, style, minSize, measurer) {
            val size = (0..2).map { lerp(style.fontSize, minSize, it / 2f) }.firstOrNull { size ->
                measurer.measure(cleaned, style.copy(fontSize = size), softWrap = false, maxLines = 1).size.width <= width
            } ?: minSize
            // MiddleEllipsis is not implemented consistently by the platform text engines.
            // Measure the actual head/ellipsis/tail string so the episode suffix survives everywhere.
            abbreviateRosterFileName(cleaned) { candidate ->
                measurer.measure(candidate, style.copy(fontSize = size), softWrap = false, maxLines = 1).size.width <= width
            } to size
        }
        Text(display, modifier = Modifier.fillMaxWidth().semantics { contentDescription = filename },
            style = style.copy(fontSize = fontSize), color = palette.inkDim, maxLines = 1, softWrap = false)
    }
}

/** Shrink toward the preferred line count; longer names grow the row at the readable floor. */
@Composable
private fun FullRosterText(filename: String, modifier: Modifier = Modifier, style: TextStyle = Type.note, preferredLines: Int = 2) {
    val measurer = rememberTextMeasurer()
    val minSize = Type.group.fontSize
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val width = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val size = remember(filename, width, density, style, minSize, measurer, preferredLines) {
            (0..2).map { step -> lerp(style.fontSize, minSize, step / 2f) }.firstOrNull { size ->
                !measurer.measure(filename, style.copy(fontSize = size), maxLines = preferredLines, constraints = Constraints(maxWidth = width)).hasVisualOverflow
            } ?: minSize
        }
        Text(filename, style = style.copy(fontSize = size), modifier = Modifier.fillMaxWidth())
    }
}

/** Unknown/withheld sizes stay unknown; byte counts get a short decimal unit. */
internal fun rosterFileSize(raw: String): String {
    val bytes = raw.toLongOrNull()?.takeIf { it > 0L } ?: return "—"
    val (divisor, suffix) = when {
        bytes >= 1_000_000_000_000L -> 1_000_000_000_000.0 to "TB"
        bytes >= 1_000_000_000L -> 1_000_000_000.0 to "GB"
        bytes >= 1_000_000L -> 1_000_000.0 to "MB"
        bytes >= 1_000L -> 1_000.0 to "KB"
        else -> return "$bytes B"
    }
    val tenths = (bytes / divisor * 10).roundToLong()
    val value = if (tenths % 10L == 0L) (tenths / 10).toString() else "${tenths / 10}.${tenths % 10}"
    return "$value $suffix"
}

private fun reportUserUrl(username: String): String {
    val body = "Reporting a user in a Synkplay room.\n\nUser: $username\nWhat happened:\n"
    return "https://github.com/yuroyami/syncplay-mobile/issues/new?title=${"[Report] user report".encodeURLParameter()}&body=${body.encodeURLParameter()}"
}
