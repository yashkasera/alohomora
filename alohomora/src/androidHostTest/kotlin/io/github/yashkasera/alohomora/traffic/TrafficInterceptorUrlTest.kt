package io.github.yashkasera.alohomora.traffic

import io.github.yashkasera.alohomora.AlohomoraImpl
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.data.datasource.local.TrafficDao
import io.github.yashkasera.alohomora.replay.toReplayRequest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * The stored URL is what replay and curl send back, so it must be the encoded URL that went on the
 * wire. It used to be stored URL-decoded, and a replay then sent a different request: `%2B` became
 * `+` (a space to the server), `%26` split one parameter into two, `%2F` changed the path.
 */
class TrafficInterceptorUrlTest {

    /** Captures the first trace TrafficCollector writes; nothing else is exercised. */
    private class RecordingTrafficDao : TrafficDao {
        val inserted = CompletableFuture<TrafficEntry>()

        override suspend fun insert(item: TrafficEntry): Long {
            inserted.complete(item)
            return 1
        }

        override suspend fun update(item: TrafficEntry): Int = 1
        override fun getById(id: String): Flow<TrafficEntry?> = emptyFlow()
        override suspend fun clearAll() = Unit
        override suspend fun delete(entity: TrafficEntry) = Unit
        override fun list(query: String?, method: String?, page: Int, pageSize: Int) =
            emptyFlow<List<TrafficEntry>>()
        override fun observeReplayOf(sourceId: String): Flow<TrafficEntry?> = emptyFlow()
        override suspend fun getLatest(limit: Int): List<TrafficEntry> = emptyList()
        override fun observeLatest(limit: Int): Flow<List<TrafficEntry>> = emptyFlow()
        override fun count(): Flow<Long> = emptyFlow()
        override fun countMatching(query: String, method: String): Flow<Long> = emptyFlow()
        override suspend fun markAsViewed(id: String) = Unit
        override fun observeUnviewedFailed(limit: Int): Flow<List<TrafficEntry>> = emptyFlow()
    }

    private val dao = RecordingTrafficDao()

    // AlohomoraImpl is process-global, so the container is swapped in for this class only and
    // the previous one restored, whatever it was. Through reflection because the setter is
    // private, and widening it for a test would let any library code replace the container.
    private val containerField = AlohomoraImpl::class.java.getDeclaredField("koinApplication")
        .apply { isAccessible = true }
    private var previous: KoinApplication? = null

    @BeforeTest
    fun installDao() {
        previous = AlohomoraImpl.koinApplication
        containerField.set(null, koinApplication { modules(module { single<TrafficDao> { dao } }) })
    }

    @AfterTest
    fun restore() {
        containerField.set(null, previous)
    }

    @Test
    fun `stored URL keeps its percent escapes and replays unchanged`() {
        var sentUrl: String? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(TrafficInterceptor(TrafficCollector(showNotification = false)))
            // Stands in for the network, recording the URL the request actually carried.
            .addInterceptor { chain ->
                sentUrl = chain.request().url.toString()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody())
                    .build()
            }
            .build()

        val url = "https://example.com/files/a%2Fb?q=1%2B1&tag=x%26y&sig=abc%3D%3D"
        client.newCall(Request.Builder().url(url).build()).execute().close()

        val entry = dao.inserted.get(5, TimeUnit.SECONDS)
        assertEquals(url, sentUrl)
        assertEquals(url, entry.url)
        assertEquals(url, entry.toReplayRequest()?.url)
    }
}
