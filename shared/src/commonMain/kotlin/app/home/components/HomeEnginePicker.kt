package app.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.i18n.AppStrings
import app.i18n.strings
import app.player.PlayerEngine
import app.theme.Motion
import app.theme.Radius
import app.theme.Space
import app.theme.Type
import app.theme.palette
import app.uicomponents.controls.Feedback
import app.uicomponents.controls.FontSizeRange
import app.uicomponents.controls.Tag
import app.uicomponents.controls.Text
import app.uicomponents.controls.Tone
import app.uicomponents.controls.VerticalRule
import app.uicomponents.controls.controlStates
import app.uicomponents.controls.pressFeedback
import org.jetbrains.compose.resources.painterResource

/**
 * Every engine at once: one hairline frame, one cell per engine with its mark, name and badge,
 * the active cell filled with the accent and carrying the bottom edge. Under the frame a "?"
 * square sits below the selected cell; a tap morphs it into a full-width card with the
 * engine's story, so there is never a doubt which engine the words are about. [compact] is the
 * short-window size: smaller marks and tighter padding, twenty points less per cell.
 */
@Composable
fun HomeEnginePicker(
    engines: List<PlayerEngine>,
    selectedEngine: String,
    onSelectEngine: (PlayerEngine) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (engines.isEmpty()) return
    val p = palette
    val selectedIndex = engines.indexOfFirst { it.name == selectedEngine }

    Column(modifier) {
        /* The row takes the height of its tallest cell rather than a fixed one, so bigger
         * system text grows the cells instead of clipping the badge out of them. */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(Radius.controlShape)
                .border(Space.hair, p.rule, Radius.controlShape)
                .selectableGroup(),
        ) {
            engines.forEachIndexed { i, engine ->
                if (i > 0) VerticalRule()
                EngineCell(
                    engine = engine,
                    active = i == selectedIndex,
                    compact = compact,
                    modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = if (compact) CompactCellHeight else CellHeight),
                    onClick = {
                        if (i == selectedIndex) return@EngineCell
                        Feedback.tick()
                        onSelectEngine(engine)
                    },
                )
            }
        }
        EngineInfoMorph(
            selectedIndex = selectedIndex.coerceAtLeast(0),
            count = engines.size,
            info = engines.getOrNull(selectedIndex)?.let { infoOf(it, strings) },
        )
    }
}

private val CellHeight = 118.dp
private val CompactCellHeight = 98.dp
private val MarkSize = 40.dp
private val CompactMarkSize = 32.dp
private val TipSize = 18.dp

/**
 * The "?" square under the selected cell, which glides to the start edge and widens into the
 * story card on a tap; a second tap folds it back. The card follows the selection while open.
 */
@Composable
private fun EngineInfoMorph(selectedIndex: Int, count: Int, info: String?) {
    val p = palette
    var open by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val name = strings.helpTip

    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = Space.gapTight)) {
        val cellWidth = maxWidth / count.coerceAtLeast(1)
        val underCell = cellWidth * selectedIndex + (cellWidth - TipSize) / 2
        val start by animateDpAsState(if (open) 0.dp else underCell, Motion.move(), label = "tipStart")
        val width by animateDpAsState(if (open) maxWidth else TipSize, Motion.move(), label = "tipWidth")
        val edge by animateColorAsState(if (open) p.accent else p.rule, Motion.quick(), label = "tipEdge")

        Box(
            modifier = Modifier
                .padding(start = start)
                .width(width)
                .clip(Radius.tightShape)
                .border(Space.hair, edge, Radius.tightShape)
                .clickable(interactionSource = source, indication = null, role = Role.Button, enabled = info != null) { Feedback.tick(); open = !open }
                .hoverable(source)
                .semantics { contentDescription = name }
                .controlStates(source, Radius.tightShape)
                .pointerHoverIcon(PointerIcon.Hand)
                .pressFeedback(source)
                .animateContentSize(Motion.move()),
        ) {
            if (open && info != null) {
                AnimatedContent(
                    targetState = info,
                    transitionSpec = { fadeIn(Motion.quick()) togetherWith fadeOut(Motion.quick()) },
                    label = "engineStory",
                ) { story ->
                    Text(story, style = Type.note, color = p.ink, modifier = Modifier.fillMaxWidth().padding(Space.gap))
                }
            } else {
                Box(Modifier.size(TipSize), contentAlignment = Alignment.Center) {
                    Text("?", style = Type.group, color = p.inkDim)
                }
            }
        }
    }
}

/** What a badge says, and in what tone. */
private class EngineBadge(val label: String, val tone: Tone)

/** The long story of an engine, by name, for the card. */
private fun infoOf(engine: PlayerEngine, s: AppStrings): String? = when (engine.name.lowercase()) {
    "exoplayer" -> s.engineInfoExoplayer
    "mpv" -> s.engineInfoMpv
    "kiteplayer" -> s.engineInfoKiteplayer
    "avplayer" -> s.engineInfoAvplayer
    "vlckit" -> s.engineInfoVlckit
    else -> null
}

/**
 * One badge per engine, the most important thing to know first: missing beats experimental
 * beats default beats the platform's own player.
 */
private fun badgeOf(engine: PlayerEngine, s: AppStrings): EngineBadge? = when {
    !engine.isAvailable -> EngineBadge(s.connectEngineBadgeUnavailable, Tone.Bad)
    engine.isExperimental -> EngineBadge(s.connectEngineBadgeExperimental, Tone.Warn)
    engine.isDefault -> EngineBadge(s.connectEngineBadgeDefault, Tone.Accent)
    engine.isSystem -> EngineBadge(s.connectEngineBadgeSystem, Tone.Neutral)
    else -> null
}

@Composable
private fun EngineCell(engine: PlayerEngine, active: Boolean, compact: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = palette
    val source = remember { MutableInteractionSource() }
    val available = engine.isAvailable
    val badge = badgeOf(engine, strings)
    val fill by animateColorAsState(if (active) p.accent.copy(alpha = 0.16f) else p.accent.copy(alpha = 0f), Motion.quick(), label = "fill")
    val edge by animateColorAsState(if (active) p.accent else p.accent.copy(alpha = 0f), Motion.quick(), label = "edge")
    val spoken = engine.name + (badge?.let { ", " + it.label } ?: "")

    Column(
        modifier = modifier
            // Always selectable: tapping an engine this build lacks lets the caller say so.
            .selectable(selected = active, role = Role.RadioButton, interactionSource = source, indication = null, onClick = onClick)
            .hoverable(source)
            .semantics { contentDescription = spoken }
            .drawBehind {
                drawRect(fill)
                val w = 2.dp.toPx()
                drawRect(edge, Offset(0f, size.height - w), Size(size.width, w))
            }
            .controlStates(source, Radius.controlShape)
            .pointerHoverIcon(PointerIcon.Hand)
            .pressFeedback(source)
            .padding(horizontal = 2.dp, vertical = if (compact) Space.gapTight else Space.gap),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(engine.img),
            contentDescription = null,
            modifier = Modifier.size(if (compact) CompactMarkSize else MarkSize),
            // An engine that cannot be picked loses its colour, not only its word.
            colorFilter = if (available) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
            alpha = if (available) 1f else 0.5f,
        )
        Spacer(Modifier.height(Space.gapTight))
        Text(
            text = engine.name,
            style = Type.label,
            color = when {
                !available -> p.disabled
                active -> p.ink
                else -> p.inkDim
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // A name wider than its cell steps down toward the group size before it is cut.
            autoSize = FontSizeRange(Type.label.fontSize),
        )
        if (badge != null) {
            Spacer(Modifier.height(Space.gapTight))
            Tag(badge.label, tone = badge.tone, filled = active && badge.tone != Tone.Neutral, autoSize = true)
        }
    }
}
