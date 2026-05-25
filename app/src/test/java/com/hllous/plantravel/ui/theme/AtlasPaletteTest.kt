package com.hllous.plantravel.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class AtlasPaletteTest {

    @Test fun `light primary is Atlas blue 2563EB`() =
        assertEquals(Color(0xFF2563EB), AtlasLightPrimary)

    @Test fun `dark primary is light Atlas blue 60A5FA`() =
        assertEquals(Color(0xFF60A5FA), AtlasDarkPrimary)

    @Test fun `light primaryContainer is DBEAFE`() =
        assertEquals(Color(0xFFDBEAFE), AtlasLightPrimaryContainer)

    @Test fun `dark primaryContainer is 1E3A6E`() =
        assertEquals(Color(0xFF1E3A6E), AtlasDarkPrimaryContainer)

    @Test fun `light background is F8FAFF`() =
        assertEquals(Color(0xFFF8FAFF), AtlasLightBackground)

    @Test fun `dark background is 0A0C14`() =
        assertEquals(Color(0xFF0A0C14), AtlasDarkBackground)

    @Test fun `light secondary is 7C3AED`() =
        assertEquals(Color(0xFF7C3AED), AtlasLightSecondary)

    @Test fun `dark secondary is A78BFA`() =
        assertEquals(Color(0xFFA78BFA), AtlasDarkSecondary)

    @Test fun `light surface is pure white`() =
        assertEquals(Color(0xFFFFFFFF), AtlasLightSurface)

    @Test fun `dark surface is 141A27`() =
        assertEquals(Color(0xFF141A27), AtlasDarkSurface)

    @Test fun `light outline is C4CEDF`() =
        assertEquals(Color(0xFFC4CEDF), AtlasLightOutline)

    @Test fun `dark outline is 384260`() =
        assertEquals(Color(0xFF384260), AtlasDarkOutline)
}
