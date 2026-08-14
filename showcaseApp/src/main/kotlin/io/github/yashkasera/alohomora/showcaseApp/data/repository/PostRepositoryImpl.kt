package io.github.yashkasera.alohomora.showcaseApp.data.repository

import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.showcaseApp.data.api.PostsApi
import io.github.yashkasera.alohomora.showcaseApp.data.db.PostDao
import io.github.yashkasera.alohomora.showcaseApp.data.db.PostEntity
import io.github.yashkasera.alohomora.showcaseApp.domain.model.Post
import io.github.yashkasera.alohomora.showcaseApp.domain.repository.PostRepository
import io.github.yashkasera.alohomora.showcaseApp.domain.repository.PreferencesRepository
import io.github.yashkasera.alohomora.showcaseApp.tracing.traced
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PostRepositoryImpl(
    private val api: PostsApi,
    private val postDao: PostDao,
    private val preferencesRepository: PreferencesRepository,
    private val tracer: Tracer,
) : PostRepository {

    override fun observePosts(): Flow<List<Post>> {
        return postDao.observePosts().map { entities ->
            entities.map { entity ->
                Post(
                    id = entity.id,
                    userId = entity.userId,
                    title = entity.title,
                    body = entity.body,
                )
            }
        }
    }

    /**
     * Instrumented with the app's own OpenTelemetry tracer, which is the only kind of tracing
     * Alohomora supports: the spans below are exported to an OTLP collector or to the Traces console
     * by nothing more than which `SpanProcessor` is registered. Alohomora sees them because
     * `AlohomoraSpanExporter` is one of them.
     *
     * Deliberately more than one span deep. A flat list of siblings tells you which calls happened;
     * the nesting is what tells you the author lookup is part of the fetch phase rather than
     * something the screen did afterwards, and that is what a waterfall is read for.
     */
    override suspend fun refreshPosts() {
        Alohomora.recordEvent("posts_refresh_start")
        try {
            // Context.root() rather than Context.current(): current() would adopt whatever span
            // happens to be on this thread's ThreadLocal — a leaked scope from unrelated work — and
            // graft this refresh onto a trace it has nothing to do with.
            tracer.traced("posts.refresh", Context.root()) { _, refresh ->
                val posts = tracer.traced("posts.fetch_remote", refresh) { _, fetch ->
                    val dtos = tracer.traced("GET /posts", fetch, SpanKind.CLIENT) { span, _ ->
                        api.fetchPosts().also { fetched ->
                            span.setAttribute("http.request.method", "GET")
                            // An event rather than an attribute: the count is only known once the
                            // body is parsed, so its position inside the span is information.
                            span.addEvent(
                                "posts.parsed",
                                Attributes.builder()
                                    .put("post.count", fetched.size.toLong())
                                    .build(),
                            )
                        }
                    }
                    tracer.traced("GET /posts/{id}/author", fetch, SpanKind.CLIENT) { span, _ ->
                        val status = api.fetchAuthorStatus(dtos.first().id)
                        span.setAttribute("http.response.status_code", status.toLong())
                        // Set by hand because nothing threw: a 404 that the client returned normally
                        // is a successful HTTP exchange as far as Ktor is concerned, and only the
                        // caller knows it was not the outcome it wanted.
                        if (status >= 400) span.setStatus(StatusCode.ERROR, "HTTP $status")
                    }
                    dtos
                }

                val now = System.currentTimeMillis()
                tracer.traced("db.replace_all", refresh) { span, _ ->
                    span.setAttribute("post.count", posts.size.toLong())
                    postDao.replaceAll(
                        posts.map { dto ->
                            PostEntity(
                                id = dto.id,
                                userId = dto.userId,
                                title = dto.title,
                                body = dto.body,
                                updatedAtEpochMillis = now,
                            )
                        },
                    )
                }

                preferencesRepository.updateLastRefreshEpochMillis(now)
                Alohomora.recordEvent(
                    "posts_refresh_success",
                    mapOf("count" to posts.size.toString()),
                )
            }
        } catch (e: Exception) {
            Alohomora.recordEvent(
                "posts_refresh_failure",
                mapOf("error" to (e.message ?: "unknown")),
            )
            throw e
        }
    }
}
