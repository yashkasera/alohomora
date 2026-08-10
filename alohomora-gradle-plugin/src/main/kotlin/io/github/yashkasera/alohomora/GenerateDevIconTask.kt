package io.github.yashkasera.alohomora

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateDevIconTask : DefaultTask() {

    @get:Input
    @get:Optional
    abstract val baseIconName: Property<String>

    @get:Input
    @get:Optional
    abstract val bgRef: Property<String>

    @get:Input
    abstract val label: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val dir = outputDir.get().asFile
        dir.deleteRecursively()
        dir.mkdirs()
        val base = baseIconName.orNull

        val valuesDir = dir.resolve("values")
        valuesDir.mkdirs()
        valuesDir.resolve("alohomora_generated.xml").writeText(buildValuesXml(label.get()))

        if (base != null) {
            val background = bgRef.orNull ?: "@color/${base}_background"

            val drawableDir = dir.resolve("drawable")
            drawableDir.mkdirs()
            drawableDir.resolve("alohomora_composite_fg.xml")
                .writeText(buildCompositeForeground(base))

            val drawableV26Dir = dir.resolve("drawable-anydpi-v26")
            drawableV26Dir.mkdirs()
            drawableV26Dir.resolve("ic_launcher_alohomora.xml")
                .writeText(buildCompositeAdaptiveIcon(background))

            val legacyDrawableDir = dir.resolve("drawable")
            legacyDrawableDir.mkdirs()
            legacyDrawableDir.resolve("ic_launcher_alohomora.xml")
                .writeText(buildCompositeLegacyIcon(base))
        }
    }

    private fun buildValuesXml(label: String): String =
        """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="alohomora_label">${escapeXml(label)}</string>
</resources>
"""

    private fun buildCompositeForeground(base: String): String =
        """<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@drawable/${base}_foreground" />
    <item android:drawable="@drawable/alohomora_overlay" />
</layer-list>
"""

    private fun buildCompositeAdaptiveIcon(background: String): String =
        """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="$background" />
    <foreground android:drawable="@drawable/alohomora_composite_fg" />
</adaptive-icon>
"""

    private fun buildCompositeLegacyIcon(base: String): String =
        """<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@mipmap/${base}" />
    <item android:drawable="@drawable/alohomora_overlay" />
</layer-list>
"""

    private fun escapeXml(value: String): String =
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("'", "\\'")
}
