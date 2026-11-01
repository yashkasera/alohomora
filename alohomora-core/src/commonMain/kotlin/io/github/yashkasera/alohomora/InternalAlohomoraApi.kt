package io.github.yashkasera.alohomora

/**
 * Marks a declaration that is public only so the `alohomora` module can reach it in
 * `alohomora-core`.
 *
 * Kotlin `internal` is scoped to one Gradle module, and the console in `alohomora` still drives
 * capture state that now lives in core. This is the cross-module substitute: excluded from the API
 * dumps through `apiValidation.nonPublicMarkers`, and a compile error for any consumer who has not
 * opted in. Pair it with `@HiddenFromObjC` so the declaration stays out of `AlohomoraKit.h` too.
 */
@RequiresOptIn(
    message = "Alohomora internal API. It may change or disappear in any release.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
)
annotation class InternalAlohomoraApi
