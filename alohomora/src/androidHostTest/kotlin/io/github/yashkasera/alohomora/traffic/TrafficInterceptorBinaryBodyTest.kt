package io.github.yashkasera.alohomora.traffic

import io.github.yashkasera.alohomora.AlohomoraImpl
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.data.datasource.local.TrafficDao
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSink
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module

/**
 * A request body that is not text is kept as bytes so replay can send it back unchanged, and
 * keeping it must not change what the app's request puts on the wire.
 */
class TrafficInterceptorBinaryBodyTest {

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

    // As in TrafficInterceptorUrlTest: AlohomoraImpl is process-global, so the container is
    // swapped in for this class only and the previous one restored.
    private val containerField = AlohomoraImpl::class.java.getDeclaredField("koinApplication")
        .apply { isAccessible = true }
    private var previous: KoinApplication? = null

    /** The bytes the stand-in network received, to compare against what was captured. */
    private var sentBytes: ByteArray? = null

    @BeforeTest
    fun installDao() {
        previous = AlohomoraImpl.koinApplication
        containerField.set(null, koinApplication { modules(module { single<TrafficDao> { dao } }) })
    }

    @AfterTest
    fun restore() {
        containerField.set(null, previous)
    }

    private fun send(body: RequestBody): TrafficEntry {
        val client = OkHttpClient.Builder()
            .addInterceptor(TrafficInterceptor(TrafficCollector(showNotification = false)))
            .addInterceptor { chain ->
                sentBytes = chain.request().body?.let { Buffer().also(it::writeTo).readByteArray() }
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody())
                    .build()
            }
            .build()
        client.newCall(Request.Builder().url("https://example.com/upload").post(body).build()).execute().close()
        return dao.inserted.get(5, TimeUnit.SECONDS)
    }

    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0xFF.toByte())

    @Test
    fun `a binary body is kept byte for byte`() {
        val entry = send(png.toRequestBody("application/octet-stream".toMediaType()))

        assertEquals(TrafficEntry.UNABLE_PARSE_MESSAGE, entry.requestBody)
        assertContentEquals(png, Base64.decode(entry.requestBodyBase64!!))
        assertContentEquals(png, sentBytes)
    }

    @Test
    fun `a multipart body with a binary part is kept byte for byte`() {
        val multipart = MultipartBody.Builder("boundary")
            .setType(MultipartBody.FORM)
            .addFormDataPart("caption", "chai")
            .addFormDataPart("file", "a.png", png.toRequestBody("image/png".toMediaType()))
            .build()

        val entry = send(multipart)

        assertEquals("multipart/form-data; boundary=boundary", entry.requestContentType)
        // Compared with what went on the wire, not with a re-render: the network must have
        // received the whole body even though capture wrote it first.
        assertContentEquals(sentBytes, Base64.decode(entry.requestBodyBase64!!))
        assertEquals(multipart.contentLength(), sentBytes?.size?.toLong())
    }

    @Test
    fun `a one shot binary body is not read`() {
        val oneShot = object : RequestBody() {
            override fun contentType() = "application/octet-stream".toMediaType()
            override fun isOneShot() = true
            override fun writeTo(sink: BufferedSink) {
                sink.write(png)
            }
        }

        val entry = send(oneShot)

        assertEquals(TrafficEntry.UNABLE_PARSE_MESSAGE, entry.requestBody)
        assertNull(entry.requestBodyBase64)
        assertContentEquals(png, sentBytes)
    }
}
