package io.github.yashkasera.alohomora.showcaseApp

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Cache
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Chrome
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The showcase app's preferences, surfaced on the console's Cache screen.
 *
 * `PreferencesDataSource` is plain `SharedPreferences` — `android_sample_prefs` — not DataStore, and
 * that matters: `CacheRepositoryImpl` scans the XML files in `shared_prefs` and has a TODO where
 * DataStore reading would go, so a DataStore-backed app would show nothing here at all. The store
 * this app uses is the one the scan can see.
 *
 * Two constraints shape the fixture, neither cosmetic:
 *
 * - **The scan happens once per process.** `getAllPreferences()` rescans only while its
 *   `cachedPreferences` field is empty, that field lives on a Koin singleton shared by the whole
 *   instrumentation run, and `CacheRepository.refresh()` is `internal` to `:alohomora` with no UI
 *   affordance behind it. So whatever is on disk the first time *any* test opens this screen is what
 *   every later test sees. Everything asserted below is therefore written in `@Before`, identically,
 *   by every test in the class.
 * - **`commit()`, not `apply()`.** The app writes with `apply()`, which returns before the XML hits
 *   disk, and the scan reads the file rather than the in-memory map. The commit below is a barrier
 *   for the writes the UI just made, not a substitute for them.
 *
 * Counts are not assertable: the host app has other preference files (Alohomora's own among them),
 * so only the presence of a specific key is under this test's control.
 *
 * No network.
 */
@RunWith(AndroidJUnit4::class)
class CacheE2ETest : ShowcaseE2ETest() {

    @Before
    fun writePreferencesThroughTheApp() {
        compose.launchShowcaseApp()

        compose.onNodeWithTag(ShowcaseTestTags.USERNAME).performTextInput(USERNAME_VALUE)
        compose.onNodeWithTag(ShowcaseTestTags.AUTO_REFRESH).performClick()
        compose.waitForIdle()

        // Types must match what `PreferencesDataSource` reads back — a String key rewritten as a
        // Boolean would make the app throw on its next read, not this test.
        preferences().edit()
            .putString(KEY_USERNAME, USERNAME_VALUE)
            .putBoolean(KEY_AUTO_REFRESH, true)
            .commit()
    }

    @Test
    fun aPreferenceWrittenByTheAppAppearsInTheCacheScreen() {
        openCache()
        compose.onTextFieldIn(Chrome.SEARCH).performTextInput(KEY_AUTO_REFRESH)
        compose.waitForIdle()

        compose.onNodeWithTag(Cache.item(KEY_AUTO_REFRESH)).assertIsDisplayed()
    }

    @Test
    fun searchNarrowsToTheUsernameKey() {
        openCache()
        compose.onTextFieldIn(Chrome.SEARCH).performTextInput(KEY_USERNAME)
        compose.waitForIdle()

        compose.onNodeWithTag(Cache.item(KEY_USERNAME)).assertIsDisplayed()
        compose.onNodeWithTag(Cache.item(KEY_AUTO_REFRESH)).assertDoesNotExist()
    }

    /**
     * Waits on the list, not on a row. The scan runs on `Dispatchers.IO` so composition goes idle
     * well before it lands, and the list is sorted by key across every store on disk — a row is only
     * reliably in the semantics tree once a search has narrowed the list to it.
     */
    private fun openCache() {
        compose.openConsole()
        compose.openModule("Cache")
        compose.awaitTag(Cache.LIST)
    }

    private fun preferences() = InstrumentationRegistry.getInstrumentation()
        .targetContext
        .getSharedPreferences(STORE, Context.MODE_PRIVATE)

    private companion object {
        /** The private constants inside `PreferencesDataSource`, mirrored. */
        const val STORE = "android_sample_prefs"
        const val KEY_USERNAME = "username"
        const val KEY_AUTO_REFRESH = "auto_refresh"

        const val USERNAME_VALUE = "alohomora-e2e"
    }
}
