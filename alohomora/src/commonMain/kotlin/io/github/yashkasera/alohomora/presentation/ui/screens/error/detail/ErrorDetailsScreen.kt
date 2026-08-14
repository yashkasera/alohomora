package io.github.yashkasera.alohomora.presentation.ui.screens.error.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.common.exceptionTypeName
import io.github.yashkasera.alohomora.ui.components.AlohomoraCircularProgressIndicator
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.dimens
import io.github.yashkasera.alohomora.presentation.ui.components.rememberClipboardCopy
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun ErrorDetailsScreen(
    errorId: Long,
    onBackClick: () -> Unit,
) {
    val viewModel = koinViewModel<ErrorDetailsViewModel> { parametersOf(errorId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboardCopy = rememberClipboardCopy()

    Scaffold(
        snackbarHost = { SnackbarHost(clipboardCopy.snackbarHostState) },
        topBar = {
            AlohomoraTopBar(
                title = state.error?.exceptionTypeName().orEmpty(),
                subtitle = state.error?.place,
                navigationIcon = {
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(Icons.ArrowLeft, contentDescription = "back")
                    }
                },
                actions = {
                    state.error?.let { error ->
                        AlohomoraIconButton(
                            onClick = {
                                clipboardCopy.copy(
                                    buildString {
                                        appendLine(error.reason ?: "Unknown exception")
                                        appendLine(error.place ?: "")
                                        appendLine()
                                        append(error.stackTrace ?: "")
                                    },
                                )
                            },
                        ) {
                            Icon(
                                Icons.Copy,
                                contentDescription = null,
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading || state.error == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                AlohomoraCircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
            }
        } else {
            val error = state.error ?: return@Scaffold

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = MaterialTheme.dimens.margin.xl),
                ) {

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

                    AlohomoraCodeBlock(
                        content = error.stackTrace ?: "No stack trace available",
                        isScrollable = false,
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxl))

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
