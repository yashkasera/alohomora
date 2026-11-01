package io.github.yashkasera.alohomora.device

import io.github.yashkasera.alohomora.AlohomoraImpl
import io.github.yashkasera.alohomora.data.datasource.local.ScreenDao
import io.github.yashkasera.alohomora.devtools.FeatureFlagStore
import io.github.yashkasera.alohomora.domain.repository.ErrorRepository
import io.github.yashkasera.alohomora.domain.repository.EventsRepository
import io.github.yashkasera.alohomora.domain.repository.SpanRepository
import io.github.yashkasera.alohomora.domain.repository.TrafficRepository
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.koin.core.Koin

/**
 * Resets the process-wide console state before each test.
 *
 * `Alohomora` is a singleton with no teardown path — `initInternal` returns early once
 * `koinApplication` is non-null, and nothing ever nulls it. The whole instrumentation run therefore
 * shares one Koin container, one Room database and one [FeatureFlagStore], and the database file
 * survives even between runs. So the unit of isolation is "wipe what the previous test wrote", not
 * "build a fresh library".
 *
 * Three things follow, none of them stylistic:
 *
 * - **Reset in `@Before`, never `@After`.** A test that fails mid-way skips its own cleanup, and
 *   the next test then inherits its rows. Cleaning on the way in is the only ordering that a
 *   failure cannot skip.
 * - **Never call `Alohomora.init()`.** `AlohomoraInitializer` already ran from the merged
 *   manifest's `androidx.startup` provider before the runner existed. A second call is a silent
 *   no-op, so writing one only creates the impression that tests control initialisation.
 * - **Shake-to-open is disarmed.** The accelerometer listener is installed unconditionally at init,
 *   and a device jostled mid-run would launch `DevToolsActivity` over whatever the test was
 *   asserting against.
 */
class ConsoleTestRule : TestRule {

    /** The library's own Koin container. Not the global one — Alohomora deliberately never uses it. */
    val koin: Koin
        get() = checkNotNull(AlohomoraImpl.koinApplication) {
            "Alohomora is not initialised. AlohomoraInitializer should have run from the test " +
                "APK's merged manifest — check that androidx.startup's InitializationProvider " +
                "survived manifest merging."
        }.koin

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                reset()
                base.evaluate()
            }
        }

    /** Empties every store the console reads from. */
    fun reset() {
        AlohomoraImpl.setShakeToOpenEnabled(false)
        AlohomoraImpl.clearReplayHandler()
        AlohomoraImpl.clearAppDatabaseOverrides()

        runBlocking {
            koin.get<TrafficRepository>().clearAll()
            koin.get<EventsRepository>().clearAll()
            koin.get<ErrorRepository>().clearAll()
            koin.get<SpanRepository>().clearAll()
            // No repository wraps ScreenDao, so this one goes through the DAO directly.
            koin.get<ScreenDao>().clearAll()
        }
        koin.get<FeatureFlagStore>().clear()
    }
}
