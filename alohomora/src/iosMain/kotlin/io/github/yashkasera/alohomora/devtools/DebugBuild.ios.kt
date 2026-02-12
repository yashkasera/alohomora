package io.github.yashkasera.alohomora.devtools

import kotlin.native.Platform

internal actual val isDebugBuild: Boolean = Platform.isDebugBinary
