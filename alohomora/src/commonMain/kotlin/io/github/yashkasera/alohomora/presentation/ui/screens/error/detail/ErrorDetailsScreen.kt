package io.github.yashkasera.alohomora.presentation.ui.screens.error.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.exceptionTypeName
import io.github.yashkasera.alohomora.presentation.ui.components.rememberClipboardCopy
import io.github.yashkasera.alohomora.ui.components.AlohomoraCircularProgressIndicator
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.CircleAlert
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens
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
                    AlohomoraIconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(AlohomoraTestTags.Chrome.BACK),
                    ) {
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
                            modifier = Modifier.testTag(AlohomoraTestTags.ErrorDetails.COPY),
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
            ErrorDetailsContent(
                error = error,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .testTag(AlohomoraTestTags.ErrorDetails.ROOT),
            )
        }
    }
}

@Composable
private fun ErrorDetailsContent(
    error: Error,
    modifier: Modifier = Modifier,
) {
    val errorTint = MaterialTheme.colorScheme.error

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.margin.xl),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

        Box(
            modifier = Modifier
                .size(MaterialTheme.dimens.icon.xl)
                .background(errorTint.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.CircleAlert,
                contentDescription = null,
                tint = errorTint,
                modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
            )
        }

        AlohomoraCodeBlock(
            content = error.stackTrace ?: "No stack trace available",
            modifier = Modifier.testTag(AlohomoraTestTags.ErrorDetails.STACK_TRACE),
            isScrollable = false,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))
    }
}

@Preview
@Composable
private fun ErrorDetailsContentPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ErrorDetailsContent(
                error = Error(
                    id = 1,
                    reason = "java.lang.NullPointerException: Attempt to invoke method on null reference",
                    place = "com.example.app.MainActivity.onCreate(MainActivity.kt:42)",
                    stackTrace = """java.lang.NullPointerException: Attempt to invoke method on null reference
    at com.example.app.MainActivity.onCreate(MainActivity.kt:42)
    at android.app.Activity.performCreate(Activity.java:8290)
    at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1417)
    at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:3626)
    at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:3782)""",
                    time = 1724234567000L,
                ),
            )
        }
    }
}

@Preview
@Composable
private fun ErrorDetailsNoStackTracePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ErrorDetailsContent(
                error = Error(
                    id = 2,
                    reason = "kotlin.KotlinNullPointerException",
                    place = null,
                    stackTrace = null,
                    time = 1724234600000L,
                ),
            )
        }
    }
}
