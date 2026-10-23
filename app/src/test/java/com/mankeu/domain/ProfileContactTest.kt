package com.mankeu.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileContactTest {

    @Test
    fun `normalizeWhatsAppPhone converts leading 0 to 62`() {
        assertEquals("628123456789", ProfileContactUtils.normalizeWhatsAppPhone("08123456789"))
        assertEquals("628123456789", ProfileContactUtils.normalizeWhatsAppPhone("0812-3456-789"))
        assertEquals("628123456789", ProfileContactUtils.normalizeWhatsAppPhone("+62 812 3456 789"))
        assertEquals("628123456789", ProfileContactUtils.normalizeWhatsAppPhone("8123456789"))
    }

    @Test
    fun `formatWhatsAppDisplay formats nicely`() {
        assertEquals("+62 8123-4567-89", ProfileContactUtils.formatWhatsAppDisplay("628123456789"))
        assertEquals("", ProfileContactUtils.formatWhatsAppDisplay(""))
    }

    @Test
    fun `normalizeTelegramContact handles username and chat id`() {
        val usernameRes = ProfileContactUtils.normalizeTelegramContact("@PetaniMaju")
        assertEquals("petanimaju", usernameRes.username)
        assertEquals("", usernameRes.chatId)
        assertEquals("@petanimaju", usernameRes.displayValue)

        val chatIdRes = ProfileContactUtils.normalizeTelegramContact("123456789")
        assertEquals("", chatIdRes.username)
        assertEquals("123456789", chatIdRes.chatId)
        assertEquals("123456789", chatIdRes.displayValue)

        val channelIdRes = ProfileContactUtils.normalizeTelegramContact("-100123456789")
        assertEquals("", channelIdRes.username)
        assertEquals("-100123456789", channelIdRes.chatId)
        assertEquals("-100123456789", channelIdRes.displayValue)

        val emptyRes = ProfileContactUtils.normalizeTelegramContact("   ")
        assertEquals("", emptyRes.username)
        assertEquals("", emptyRes.chatId)
        assertEquals("", emptyRes.displayValue)
    }
}
