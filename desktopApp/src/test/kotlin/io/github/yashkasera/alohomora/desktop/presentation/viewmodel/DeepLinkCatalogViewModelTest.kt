package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.desktop.data.config.ConfigStoreFacade
import io.github.yashkasera.alohomora.desktop.data.config.LocalConfigStore
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class DeepLinkCatalogViewModelTest {

    private val baseDir: File = Files.createTempDirectory("alohomora-catalog-vm-test").toFile()
    private val store = ConfigStoreFacade(LocalConfigStore(baseDir))
    private val vm = DeepLinkCatalogViewModel(store)

    @AfterTest
    fun tearDown() {
        vm.close()
        baseDir.deleteRecursively()
    }

    @Test
    fun `createFromUrl promotes a built url into an editable draft`() {
        vm.createFromUrl("fampay://kyc/verify/123?step=docs")

        val draft = vm.uiState.value.editorDraft
        assertNotNull(draft)
        assertEquals("fampay://kyc/verify/123?step=docs", draft.uriTemplate)
        assertEquals("kyc", draft.module)
        assertEquals("123", draft.name)
        assertEquals(listOf("fampay://kyc/verify/123?step=docs"), draft.examples)
    }

    @Test
    fun `createFromUrl persists the definition to the local catalog`() = runBlocking {
        vm.createFromUrl("app://cards/home")

        // The save runs on the VM's IO scope; give it a moment, then read the store directly.
        repeat(20) {
            if (store.list(ConfigKind.DeepLinks, ConfigScope.LOCAL).isNotEmpty()) return@repeat
            delay(25)
        }
        val saved = store.list(ConfigKind.DeepLinks, ConfigScope.LOCAL).map { it.value }
        assertTrue(saved.any { it.uriTemplate == "app://cards/home" && it.module == "cards" })
    }
}
