package com.mankeu.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MotionPreferenceTest {
    @Test
    fun motionPreferenceRespectsSystemScale() {
        assertEquals(MankeuMotion.Off, MankeuMotion.resolve(MotionMode.FULL, 0f))
        assertEquals(MankeuMotion.Reduced, MankeuMotion.resolve(MotionMode.FULL, 0.5f))
        assertEquals(MankeuMotion.Off, MankeuMotion.resolve(MotionMode.OFF, 1f))
        assertFalse(MankeuMotion.Reduced.expressive)
        assertFalse(MankeuMotion.Off.expressive)
    }
}
