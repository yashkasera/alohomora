package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import io.github.yashkasera.alohomora.desktop.data.config.ConfigRepoStatus
import io.github.yashkasera.alohomora.desktop.presentation.model.ConfigRepoUiState
import io.github.yashkasera.alohomora.desktop.presentation.model.TeamConfigCopy
import io.github.yashkasera.alohomora.ui.components.AlohomoraAlertDialog
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCircularProgressIndicator
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.icons.ChevronRight
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.dimens

private enum class WizardStep { EXPLAIN, CHOOSE, DETAILS, DONE }

private enum class SetupPath { JOIN, START_NEW }

/**
 * A guided, git-vocabulary-free path to a connected Team-config repo, for someone who has never run
 * `git clone` in their life. It is a thin view over [ConfigRepoViewModel]'s existing actions — the
 * same url/token inputs and connect/initialize calls the raw settings form used — reframed as a
 * four-step flow: explain → pick a path → paste the link → done.
 *
 * Success and failure are read off [ConfigRepoUiState.status]: a submit sets a one-shot flag, and the
 * flow advances to DONE on [ConfigRepoStatus.Connected] or surfaces a friendly error and stays put on
 * [ConfigRepoStatus.Error]. The raw error is never shown here — Developer mode in settings owns that.
 */
@Composable
fun ConfigSetupWizard(
    state: ConfigRepoUiState,
    onUrlChange: (String) -> Unit,
    onPatChange: (String) -> Unit,
    onConnect: () -> Unit,
    onInitialize: () -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(WizardStep.EXPLAIN) }
    var path by remember { mutableStateOf<SetupPath?>(null) }
    var submitted by remember { mutableStateOf(false) }

    // Advance once a submit settles: Connected → Done, Error → stay on Details and let the error show.
    LaunchedEffect(state.status, state.busy, submitted) {
        if (submitted && !state.busy) {
            when (state.status) {
                is ConfigRepoStatus.Connected -> {
                    step = WizardStep.DONE
                    submitted = false
                }

                is ConfigRepoStatus.Error -> submitted = false
                else -> Unit
            }
        }
    }

    val title = when (step) {
        WizardStep.EXPLAIN -> "Set up Team config"
        WizardStep.CHOOSE -> "How do you want to start?"
        WizardStep.DETAILS -> if (path == SetupPath.JOIN) "Join your team's config" else "Start a new shared config"
        WizardStep.DONE -> "You're all set"
    }

    AlohomoraAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md)) {
                when (step) {
                    WizardStep.EXPLAIN -> Text(
                        text = "Team config shares your journeys, mock sets, and deep links with your " +
                            "team, so everyone works from the same set. Your personal config always " +
                            "keeps working with no setup — this is only for the shared ones.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    WizardStep.CHOOSE -> {
                        PathCard(
                            title = "Join my team's config",
                            subtitle = "Someone on your team already set this up and shared a link.",
                            onClick = { path = SetupPath.JOIN; step = WizardStep.DETAILS },
                        )
                        PathCard(
                            title = "Start a new shared config",
                            subtitle = "Be the first — create a shared config for your team.",
                            onClick = { path = SetupPath.START_NEW; step = WizardStep.DETAILS },
                        )
                    }

                    WizardStep.DETAILS -> DetailsStep(
                        path = path ?: SetupPath.JOIN,
                        state = state,
                        onUrlChange = onUrlChange,
                        onPatChange = onPatChange,
                    )

                    WizardStep.DONE -> Text(
                        text = "You're connected. From now on, the Share button on any journey, mock " +
                            "set, or deep link opens a review your teammates can approve — nothing " +
                            "changes for them until they do. Switch any list to the Team tab to see " +
                            "what's shared.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            when (step) {
                WizardStep.EXPLAIN -> AlohomoraFilledButton(
                    text = "Get started",
                    onClick = { step = WizardStep.CHOOSE },
                )

                WizardStep.CHOOSE -> Unit // the two cards are the choice

                WizardStep.DETAILS -> {
                    val isJoin = path == SetupPath.JOIN
                    // Join needs a link; starting new may be local-only, so a blank link is valid there.
                    val canSubmit = !state.busy && (!isJoin || state.urlInput.isNotBlank())
                    AlohomoraFilledButton(
                        text = if (isJoin) "Connect" else "Create",
                        onClick = { submitted = true; if (isJoin) onConnect() else onInitialize() },
                        enabled = canSubmit,
                        leadingIcon = if (state.busy) {
                            {
                                AlohomoraCircularProgressIndicator(
                                    modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                                    strokeWidth = MaterialTheme.dimens.stroke.medium,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }

                WizardStep.DONE -> AlohomoraFilledButton(text = "Done", onClick = onDismiss)
            }
        },
        dismissButton = {
            when (step) {
                WizardStep.EXPLAIN -> AlohomoraTextButton(text = "Cancel", onClick = onDismiss)
                WizardStep.CHOOSE -> AlohomoraTextButton(text = "Back", onClick = { step = WizardStep.EXPLAIN })
                WizardStep.DETAILS -> AlohomoraTextButton(
                    text = "Back",
                    onClick = { if (!state.busy) step = WizardStep.CHOOSE },
                )

                WizardStep.DONE -> null
            }
        },
    )
}

@Composable
private fun DetailsStep(
    path: SetupPath,
    state: ConfigRepoUiState,
    onUrlChange: (String) -> Unit,
    onPatChange: (String) -> Unit,
) {
    val isJoin = path == SetupPath.JOIN
    val url = state.urlInput
    val showToken = url.trim().startsWith("https", ignoreCase = true)

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md)) {
        Text(
            text = if (isJoin) {
                "Paste the link your team shared. It usually starts with https:// or git@."
            } else {
                "Add the link to an empty repository your team created, or leave it blank to keep the " +
                    "shared config on your machine for now — you can add a link later."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AlohomoraTextField(
            value = url,
            onValueChange = onUrlChange,
            label = if (isJoin) "Repository link" else "Repository link (optional)",
            placeholder = "https://… or git@…",
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showToken) {
            AlohomoraTextField(
                value = state.patInput,
                onValueChange = onPatChange,
                label = "Access token",
                placeholder = "Only needed for https links — saved to your keychain",
                enabled = !state.busy,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "An access token is how an https link proves it's you. Your git host (GitHub, " +
                    "GitLab…) creates one under Settings → Developer/Access tokens.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.busy) {
            Text(
                text = if (isJoin) "Connecting…" else "Creating…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        (state.status as? ConfigRepoStatus.Error)?.let { error ->
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
            Text(
                text = TeamConfigCopy.friendlyError(error.message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun PathCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    AlohomoraCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.margin.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(MaterialTheme.dimens.icon.md),
            )
        }
    }
}
