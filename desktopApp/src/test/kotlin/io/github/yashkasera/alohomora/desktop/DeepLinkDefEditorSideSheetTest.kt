package io.github.yashkasera.alohomora.desktop

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import io.github.yashkasera.alohomora.common.deeplink.DeepLinkDef
import io.github.yashkasera.alohomora.common.deeplink.DeepLinkParam
import io.github.yashkasera.alohomora.common.deeplink.ParamType
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.DeepLinkDefEditorSideSheet
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import kotlin.test.Test

/**
 * The editor packs fields, a params list, an examples list, and a fire form into one LazyColumn — the
 * combination a unit test cannot reach. This guards that they lay out together (unique keys) and that
 * the fire form's [io.github.yashkasera.alohomora.common.deeplink.buildUri] renders without throwing.
 */
@OptIn(ExperimentalTestApi::class)
class DeepLinkDefEditorSideSheetTest {

    private val draft = DeepLinkDef(
        id = "d1",
        name = "KYC verify",
        module = "kyc",
        uriTemplate = "app://kyc/verify/{userId}?step={step}",
        params = listOf(
            DeepLinkParam(name = "userId", type = ParamType.UUID),
            DeepLinkParam(name = "step", type = ParamType.ENUM, allowedValues = listOf("intro", "docs")),
        ),
        examples = listOf("app://kyc/verify/123e4567-e89b-12d3-a456-426614174000?step=docs"),
    )

    @Test
    fun `renders fields params examples and the fire form together`() = runComposeUiTest {
        setContent {
            AppTheme {
                DeepLinkDefEditorSideSheet(
                    draft = draft,
                    validationErrors = emptyList(),
                    onNameChange = {},
                    onModuleChange = {},
                    onFlowChange = {},
                    onDescriptionChange = {},
                    onUriTemplateChange = {},
                    onParamsChange = {},
                    onExamplesChange = {},
                    onFire = {},
                    onDismiss = {},
                )
            }
        }

        onNodeWithText("Edit deep link").assertIsDisplayed()
    }
}
