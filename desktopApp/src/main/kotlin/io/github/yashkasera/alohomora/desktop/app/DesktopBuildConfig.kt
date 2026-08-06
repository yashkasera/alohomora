package io.github.yashkasera.alohomora.desktop.app

import java.util.Properties

object DesktopBuildConfig {
    val version: String by lazy {
        val props = Properties()
        val stream = DesktopBuildConfig::class.java.classLoader
            .getResourceAsStream("alohomora-desktop-version.properties")
        if (stream != null) {
            props.load(stream)
            stream.close()
        }
        props.getProperty("version", "0.0.0")
    }
}
