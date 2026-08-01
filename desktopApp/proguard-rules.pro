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

-dontnote **
-dontwarn **
