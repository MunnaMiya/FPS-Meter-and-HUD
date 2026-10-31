package com.growtotype.fpsmeterandhud

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.growtotype.fpsmeterandhud.model.HardwareSample
import com.growtotype.fpsmeterandhud.model.OverlaySettings
import com.growtotype.fpsmeterandhud.ui.components.parseSamplesJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FPS Meter & HUD", appName)
    }

    @Test
    fun `hardware sample ram calculation`() {
        val sample = HardwareSample(
            fps = 120f,
            ramUsedMb = 4096,
            ramTotalMb = 8192
        )
        assertEquals(50f, sample.ramPercent, 0.01f)
    }

    @Test
    fun `overlay settings defaults`() {
        val settings = OverlaySettings()
        assertTrue(settings.showFps)
        assertTrue(settings.opacity in 0.25f..1.0f)
        assertEquals(true, settings.isDarkTheme)
        assertEquals(false, settings.fpsOnlyMode)
        assertEquals(1.0f, settings.scale, 0.01f)
    }

    @Test
    fun `parse samples json works properly`() {
        val json = """[{"t":0,"f":60.0,"c":35.0,"g":40.0,"r":3200,"tm":42.0},{"t":1,"f":59.5,"c":38.0,"g":45.0,"r":3250,"tm":42.5}]"""
        val points = parseSamplesJson(json)
        assertEquals(2, points.size)
        assertEquals(60.0f, points[0].fps, 0.1f)
        assertEquals(59.5f, points[1].fps, 0.1f)
    }
}
