package com.mankeu.domain

data class NormalizedTelegram(
    val username: String,
    val chatId: String,
    val displayValue: String,
)

object ProfileContactUtils {
    /**
     * Normalizes WhatsApp phone number by removing non-digits and ensuring standard country code.
     * e.g., "08123456789" -> "628123456789", "+62 812-3456-789" -> "628123456789"
     */
    fun normalizeWhatsAppPhone(input: String): String {
        val digits = input.filter { it.isDigit() }
        return when {
            digits.startsWith("0") -> "62" + digits.substring(1)
            digits.startsWith("8") -> "628" + digits.substring(1)
            else -> digits
        }
    }

    /**
     * Formats WhatsApp phone for clean display.
     * e.g., "628123456789" -> "+62 812-3456-789"
     */
    fun formatWhatsAppDisplay(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        if (digits.isBlank()) return ""
        return if (digits.startsWith("62")) {
            val rest = digits.substring(2)
            "+62 " + rest.chunked(4).joinToString("-")
        } else if (digits.startsWith("0")) {
            digits.chunked(4).joinToString("-")
        } else {
            digits
        }
    }

    /**
     * Cleans telegram username (strips leading @ and lowercases).
     */
    fun normalizeTelegramUsername(input: String): String {
        return input.trim().removePrefix("@").lowercase()
    }

    /**
     * Normalizes a telegram contact string which can be a username or numeric chatId.
     */
    fun normalizeTelegramContact(input: String): NormalizedTelegram {
        val value = input.trim()
        val withoutAt = value.removePrefix("@")
        if (withoutAt.isBlank()) {
            return NormalizedTelegram(username = "", chatId = "", displayValue = "")
        }
        if (withoutAt.matches(Regex("^-?\\d+$"))) {
            return NormalizedTelegram(username = "", chatId = withoutAt, displayValue = withoutAt)
        }
        val username = normalizeTelegramUsername(withoutAt)
        return NormalizedTelegram(
            username = username,
            chatId = "",
            displayValue = if (username.isNotBlank()) "@$username" else "",
        )
    }
}
