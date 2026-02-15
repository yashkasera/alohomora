package io.github.yashkasera.alohomora.presentation.ui.screens.apilog.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.yashkasera.alohomora.presentation.ui.components.icons.ArrowLeft
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Icons
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Share
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ApiLogDetailsScreen(callId: String, onBackClick: () -> Unit = {}) {
    val viewModel = koinViewModel<ApiLogDetailsViewModel> { parametersOf(callId) }
    val state by viewModel.state.collectAsState()
    val call = state.call

    if (call == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Log not found.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        return
    }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            AlohomoraTopBar(
                title = "API Request",
                subtitle = null,
                navigationIcon = {
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.ArrowLeft,
                            contentDescription = "back",
                        )
                    }
                },
                actions = {
                    AlohomoraIconButton(
                        onClick = {
                            // TODO: Implement share
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Share,
                            contentDescription = "Share",
                        )
                    }
                },
            )
        },
    ) { padding ->
        ApiLogDetailsContent(
            call = call,
            modifier = Modifier.padding(padding),
        )
    }
}
