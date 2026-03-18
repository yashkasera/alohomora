-ignorewarnings
-dontobfuscate
-dontoptimize
-dontshrink

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keep class io.github.yashkasera.alohomora.** { *; }
-keep class androidx.compose.** { *; }
-keep class org.jetbrains.compose.** { *; }
-keep class io.ktor.** { *; }
-keep class kotlinx.** { *; }
-keep class org.jetbrains.pty4j.** { *; }
-keep class com.sun.jna.** { *; }
-keep class org.slf4j.** { *; }
-keep class ch.qos.logback.** { *; }

-dontnote **
-dontwarn **
