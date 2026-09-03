package io.github.yashkasera.alohomora.device

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import io.github.yashkasera.alohomora.domain.repository.CacheRepository
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Cache
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags.Chrome
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Cache is the one console screen with no seedable store: `CacheRepositoryImpl` scans the app's
 * real `shared_prefs/` directory, so the fixture is a real `SharedPreferences` file written from
 * the test and torn down in [clearPreferences].
 *
 * Two consequences shape everything below.
 *
 * - **Every write must be followed by `refresh()`.** `getAllPreferences` — what `CacheViewModel`
 *   calls at init — rescans only while its `cachedPreferences` field is empty, and that field lives
 *   on a Koin singleton shared by the whole instrumentation run. Without the explicit refresh the
 *   second test in the class would render the first test's scan.
 * - **The store is not the only one on disk.** The host app under test may have its own
 *   `shared_prefs` files, so a count assertion can only be relative, and there is no reliable way
 *   to drive the "No Preferences Found" empty state. See the class report for what that omits.
 */
class CacheScreenTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Before
    fun clearPreferences() {
        preferences().edit().clear().commit()
        runBlocking { console.koin.get<CacheRepository>().refresh() }
    }

    @Test
    fun writtenPreferenceKeysAppearInTheList() {
        seedPreferences()

        compose.launchConsole(Routes.Cache)

        compose.onNodeWithTag(Cache.item(USER_ID_KEY)).assertIsDisplayed()
        compose.onNodeWithTag(Cache.item(THEME_KEY)).assertIsDisplayed()
        compose.onNodeWithTag(Cache.item(LOCALE_KEY)).assertIsDisplayed()
    }

    @Test
    fun searchNarrowsByKey() {
        seedPreferences()

        compose.launchConsole(Routes.Cache)
        compose.onTextFieldIn(Chrome.SEARCH).performTextInput("user_id")
        compose.waitForIdle()

        compose.onNodeWithTag(Cache.item(USER_ID_KEY)).assertIsDisplayed()
        compose.onNodeWithTag(Cache.item(THEME_KEY)).assertDoesNotExist()
    }

    @Test
    fun footerReportsTheKeyCount() {
        seedPreferences()

        compose.launchConsole(Routes.Cache)

        compose.onNode(hasText("keys (", substring = true) and inFooter())
            .assertIsDisplayed()
    }

    @Test
    fun footerReportsTheFilteredCountAgainstTheTotal() {
        seedPreferences()

        compose.launchConsole(Routes.Cache)
        compose.onTextFieldIn(Chrome.SEARCH).performTextInput("user_id")
        compose.waitForIdle()

        // Absolute totals are not assertable — the host app may have its own preference files —
        // so this pins the filtered half of "N of M keys (size)", which the fixture does control.
        compose.onNode(hasText("1 of ", substring = true) and inFooter())
            .assertIsDisplayed()
    }

    @Test
    fun noResultsStateWhenTheQueryMatchesNothing() {
        seedPreferences()

        compose.launchConsole(Routes.Cache)
        compose.onTextFieldIn(Chrome.SEARCH).performTextInput("no-such-preference-key")
        compose.waitForIdle()

        compose.onNodeWithTag(Chrome.EMPTY_STATE).assertIsDisplayed()
        compose.onNodeWithTag(Chrome.EMPTY_STATE_TITLE).assertTextContains("No Results")
    }

    /**
     * Keys are `aaa_`-prefixed because the scan sorts by key and the list is lazy: a host app store
     * sorting ahead of the fixture would push these below the fold and out of the semantics tree.
     */
    private fun seedPreferences() {
        preferences().edit()
            .putString(USER_ID_KEY, "42")
            .putString(THEME_KEY, "midnight")
            .putString(LOCALE_KEY, "en-IN")
            .commit()
        runBlocking { console.koin.get<CacheRepository>().refresh() }
    }

    private fun preferences() = InstrumentationRegistry.getInstrumentation()
        .targetContext
        .getSharedPreferences(STORE, Context.MODE_PRIVATE)

    private fun inFooter() = hasAnyAncestor(hasTestTag(Cache.FOOTER))

    private companion object {
        /**
         * Must not trip `CacheRepositoryImpl.isLikelyEncrypted` — a name containing "secure",
         * "vault", "enc_" and friends is reported as an encrypted store and its values render as
         * `[encrypted]`.
         */
        const val STORE = "aaa_alohomora_device_test_prefs"

        const val USER_ID_KEY = "aaa_alohomora_user_id"
        const val THEME_KEY = "aaa_alohomora_theme"
        const val LOCALE_KEY = "aaa_alohomora_locale"
    }
}
