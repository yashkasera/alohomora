package io.github.yashkasera.alohomora

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

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

    @get:Input
    @get:Optional
    abstract val variantBarText: Property<String>

    @get:Input
    @get:Optional
    abstract val variantBarPosition: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val dir = outputDir.get().asFile
        dir.deleteRecursively()
        dir.mkdirs()
        val base = baseIconName.orNull
        val barText = variantBarText.orNull
        val position = variantBarPosition.orNull
            ?.let { runCatching { DevIconVariantBarPosition.valueOf(it) }.getOrNull() }

        val valuesDir = dir.resolve("values")
        valuesDir.mkdirs()
        valuesDir.resolve("alohomora_generated.xml").writeText(buildValuesXml(label.get()))

        if (base != null) {
            val background = bgRef.orNull ?: "@color/${base}_background"
            val hasBar = barText != null && position != null && position != DevIconVariantBarPosition.NONE

            val drawableDir = dir.resolve("drawable")
            drawableDir.mkdirs()

            if (hasBar) {
                generateVariantBarPng(barText!!, position!!, dir)
            }

            drawableDir.resolve("alohomora_composite_fg.xml")
                .writeText(buildCompositeForeground(base, hasBar))

            val drawableV26Dir = dir.resolve("drawable-anydpi-v26")
            drawableV26Dir.mkdirs()
            drawableV26Dir.resolve("ic_launcher_alohomora.xml")
                .writeText(buildCompositeAdaptiveIcon(background))

            drawableDir.resolve("ic_launcher_alohomora.xml")
                .writeText(buildCompositeLegacyIcon(base, hasBar))
        }
    }

    private fun generateVariantBarPng(text: String, position: DevIconVariantBarPosition, resDir: File) {
        DENSITY_BUCKETS.forEach { (qualifier, scale) ->
            val size = (VIEWPORT * scale).toInt()
            val image = renderRibbon(text.uppercase(), position, size, scale)
            val dir = resDir.resolve(qualifier)
            dir.mkdirs()
            ImageIO.write(image, "PNG", dir.resolve("alohomora_variant_bar.png"))
        }
    }

    private fun renderRibbon(
        text: String,
        position: DevIconVariantBarPosition,
        size: Int,
        densityScale: Float,
    ): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
            setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        }

        g.scale(densityScale.toDouble(), densityScale.toDouble())

        when (position) {
            DevIconVariantBarPosition.TOP, DevIconVariantBarPosition.BOTTOM -> {
                val isTop = position == DevIconVariantBarPosition.TOP
                drawHorizontalBar(g, text, isTop)
            }
            else -> {
                val (cx, cy, angleDeg, textOffset) = cornerParams(position)
                drawDiagonalRibbon(g, text, cx, cy, angleDeg, textOffset)
            }
        }

        g.dispose()
        return image
    }

    private fun drawHorizontalBar(g: Graphics2D, text: String, isTop: Boolean) {
        val barY = if (isTop) 0.0 else 76.0
        val textCy = if (isTop) 16.0 else 92.0

        // Shadow
        g.color = SHADOW_DARK
        g.fill(Rectangle2D.Double(0.0, barY + 1.0, VIEWPORT.toDouble(), BAR_HEIGHT.toDouble()))
        g.color = SHADOW_LIGHT
        g.fill(Rectangle2D.Double(0.0, barY + 2.5, VIEWPORT.toDouble(), BAR_HEIGHT.toDouble()))

        // Bar
        g.color = BAR_COLOR
        g.fill(Rectangle2D.Double(0.0, barY, VIEWPORT.toDouble(), BAR_HEIGHT.toDouble()))

        drawCenteredText(g, text, VIEWPORT / 2.0, textCy)
    }

    private fun drawDiagonalRibbon(
        g: Graphics2D, text: String,
        cx: Float, cy: Float, angleDeg: Float, textOffset: Float,
    ) {
        val saved = g.transform

        g.translate(cx.toDouble(), cy.toDouble())
        g.rotate(Math.toRadians(angleDeg.toDouble()))

        val rw = RIBBON_LENGTH.toDouble()
        val rh = RIBBON_WIDTH.toDouble()

        // Shadow
        g.color = SHADOW_DARK
        g.fill(Rectangle2D.Double(-rw / 2, -rh / 2 + 1.0, rw, rh))
        g.color = SHADOW_LIGHT
        g.fill(Rectangle2D.Double(-rw / 2, -rh / 2 + 2.5, rw, rh))

        // Ribbon
        g.color = BAR_COLOR
        g.fill(Rectangle2D.Double(-rw / 2, -rh / 2, rw, rh))

        drawCenteredText(g, text, textOffset.toDouble(), 0.0)

        g.transform = saved
    }

    private fun drawCenteredText(g: Graphics2D, text: String, cx: Double, cy: Double) {
        g.color = Color.WHITE
        g.font = TEXT_FONT
        val fm = g.fontMetrics
        val x = cx - fm.stringWidth(text) / 2.0
        val y = cy + (fm.ascent - fm.descent) / 2.0
        g.drawString(text, x.toFloat(), y.toFloat())
    }

    private data class CornerSpec(
        val cx: Float, val cy: Float, val angleDeg: Float,
        val textOffset: Float,
    )

    private fun cornerParams(position: DevIconVariantBarPosition): CornerSpec = when (position) {
        DevIconVariantBarPosition.TOP_RIGHT -> CornerSpec(68f, 40f, 45f, textOffset = 3f)
        DevIconVariantBarPosition.TOP_LEFT -> CornerSpec(40f, 40f, -45f, textOffset = -3f)
        DevIconVariantBarPosition.BOTTOM_LEFT -> CornerSpec(40f, 68f, 45f, textOffset = -3f)
        DevIconVariantBarPosition.BOTTOM_RIGHT -> CornerSpec(68f, 68f, -45f, textOffset = 3f)
        else -> error("Not a corner position")
    }

    private fun buildValuesXml(label: String): String =
        """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="alohomora_label">${escapeXml(label)}</string>
</resources>
"""

    private fun buildCompositeForeground(base: String, hasBar: Boolean): String {
        val barItem = if (hasBar) "\n    <item android:drawable=\"@drawable/alohomora_variant_bar\" />" else ""
        return """<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@drawable/${base}_foreground" />
    <item android:drawable="@drawable/alohomora_overlay" />$barItem
</layer-list>
"""
    }

    private fun buildCompositeAdaptiveIcon(background: String): String =
        """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="$background" />
    <foreground android:drawable="@drawable/alohomora_composite_fg" />
</adaptive-icon>
"""

    private fun buildCompositeLegacyIcon(base: String, hasBar: Boolean): String {
        val barItem = if (hasBar) "\n    <item android:drawable=\"@drawable/alohomora_variant_bar\" />" else ""
        return """<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@mipmap/${base}" />
    <item android:drawable="@drawable/alohomora_overlay" />$barItem
</layer-list>
"""
    }

    private fun escapeXml(value: String): String =
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("'", "\\'")

    companion object {
        private const val VIEWPORT = 108f
        private const val RIBBON_LENGTH = 120f
        private const val RIBBON_WIDTH = 20f
        private const val BAR_HEIGHT = 32f

        private val BAR_COLOR = Color(0, 0, 0, 230)
        private val SHADOW_DARK = Color(0, 0, 0, 50)
        private val SHADOW_LIGHT = Color(0, 0, 0, 25)
        private val TEXT_FONT = Font("SansSerif", Font.BOLD, 10)

        private val DENSITY_BUCKETS = listOf(
            "drawable-mdpi" to 1f,
            "drawable-hdpi" to 1.5f,
            "drawable-xhdpi" to 2f,
            "drawable-xxhdpi" to 3f,
            "drawable-xxxhdpi" to 4f,
        )
    }
}
