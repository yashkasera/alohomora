package io.github.yashkasera.alohomora.ui.theme

import androidx.compose.material3.Shapes
import kotlin.test.Test
import kotlin.test.assertEquals

class AlohomoraShapesTest {

    /**
     * Pins the moment `AppTheme` started supplying `shapes`.
     *
     * Before that it supplied only `colorScheme` and `typography`, so all 60 `MaterialTheme.shapes.*`
     * reads resolved to Material's defaults. Introducing a scale therefore had to introduce *those*
     * values, or it would have silently restyled 60 call sites in the same commit that gave the design
     * system its first opinion about corners.
     *
     * This asserts that equality directly instead of asking someone to compare screenshots. It is the
     * one test to delete on the day the scale deliberately diverges — a failure here means "the radii
     * moved", which is either the point of your change or a bug in it.
     */
    @Test
    fun `alohomora shapes match the material defaults at introduction`() {
        assertEquals(Shapes(), AlohomoraShapes)
    }
}
