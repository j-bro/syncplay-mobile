package app.home.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.theme.palette
import app.uicomponents.controls.AccentAction
import app.uicomponents.controls.ActionStatus
import app.uicomponents.controls.SecondaryAction
import org.jetbrains.compose.resources.stringResource
import syncplaymobile.shared.generated.resources.Res
import syncplaymobile.shared.generated.resources.about_update_available
import syncplaymobile.shared.generated.resources.about_update_button
import syncplaymobile.shared.generated.resources.about_update_checking
import syncplaymobile.shared.generated.resources.about_update_current
import syncplaymobile.shared.generated.resources.about_update_failed

@Composable
internal fun UpdateCheckAction(
    result: UpdateCheck.Result?,
    isChecking: Boolean,
    onCheck: () -> Unit,
    onOpenRelease: (String) -> Unit,
) {
    when (result) {
        UpdateCheck.Result.UpToDate -> ActionStatus(
            text = stringResource(Res.string.about_update_current),
            color = palette.ok,
            modifier = Modifier.fillMaxWidth(),
        )
        is UpdateCheck.Result.Newer -> AccentAction(
            text = stringResource(Res.string.about_update_available, result.version),
            onClick = { onOpenRelease(result.url) },
            modifier = Modifier.fillMaxWidth(),
        )
        else -> SecondaryAction(
            text = stringResource(when {
                isChecking -> Res.string.about_update_checking
                result == UpdateCheck.Result.Unreachable -> Res.string.about_update_failed
                else -> Res.string.about_update_button
            }),
            enabled = !isChecking,
            onClick = onCheck,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
