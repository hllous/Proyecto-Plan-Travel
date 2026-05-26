package com.hllous.plantravel.ui.utils

import androidx.compose.ui.graphics.Color

fun memberInitial(name: String): String = name.trim().firstOrNull()?.uppercase() ?: "?"

fun displayInitials(name: String): String = name
    .split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }
    .ifEmpty { "?" }

fun memberColor(memberId: String): Color {
    val palette = listOf(
        Color(0xFF1D4ED8),
        Color(0xFF7C3AED),
        Color(0xFF0D9488),
        Color(0xFFEA580C),
        Color(0xFFBE123C),
        Color(0xFF4338CA),
        Color(0xFF15803D),
        Color(0xFFB45309)
    )
    val index = ((memberId.hashCode() % palette.size) + palette.size) % palette.size
    return palette[index]
}

fun formatCurrency(cents: Long): String = "$${"%.2f".format(cents / 100.0)}"

fun isItemFullyAssigned(assignedQuantity: Int, totalQuantity: Int): Boolean =
    assignedQuantity >= totalQuantity

fun greetingForHour(hour: Int): String = when (hour) {
    in 6..11 -> "Buenos días"
    in 12..19 -> "Buenas tardes"
    else -> "Buenas noches"
}

fun formatExpiry(expiresAtMillis: Long): String {
    val diff = expiresAtMillis - System.currentTimeMillis()
    return when {
        diff <= 0 -> "Vencido"
        diff < 60 * 60 * 1000L -> "Vence en menos de 1 hora"
        diff < 24 * 60 * 60 * 1000L -> {
            val hours = diff / (60 * 60 * 1000L)
            "Vence en $hours hora${if (hours != 1L) "s" else ""}"
        }
        diff < 48 * 60 * 60 * 1000L -> "Vence mañana"
        else -> {
            val days = diff / (24 * 60 * 60 * 1000L)
            "Vence en $days días"
        }
    }
}
