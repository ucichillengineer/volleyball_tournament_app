package com.familydays.app

enum class EventType(val label: String) {
    BIRTHDAY("Birthday"),
    ANNIVERSARY("Anniversary")
}

data class ImportantDay(
    val id: String,
    val name: String,
    val initial: String,
    val month: Int,
    val day: Int,
    val year: Int?,
    val type: EventType,
    val greetingTemplate: String? = null,
    val needsReview: Boolean = false
) {
    init {
        require(month in 1..12)
        require(day in 1..31)
    }

    val displayName: String
        get() = "$name $initial.".trim()
}

fun ImportantDay.daysUntil(todayMonth: Int, todayDay: Int): Int {
    val daysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    fun ordinal(month: Int, day: Int) = daysInMonth.take(month - 1).sum() + day
    val difference = ordinal(month, day) - ordinal(todayMonth, todayDay)
    return if (difference >= 0) difference else difference + 365
}

fun ImportantDay.greeting(currentYear: Int): String {
    val years = year?.let { (currentYear - it).coerceAtLeast(0) }
    val defaultGreeting = when (type) {
        EventType.BIRTHDAY -> if (years == null) "Happy Birthday, $name!" else "Happy $years${ordinalSuffix(years)} Birthday, $name!"
        EventType.ANNIVERSARY -> if (years == null) "Happy Anniversary, $name!" else "Happy $years${ordinalSuffix(years)} Anniversary, $name!"
    }
    return greetingTemplate
        ?.replace("{name}", name)
        ?.replace("{years}", years?.toString().orEmpty())
        ?.replace("{event}", type.label.lowercase())
        ?.takeIf { it.isNotBlank() }
        ?: defaultGreeting
}

private fun ordinalSuffix(number: Int): String = when (number % 100) {
    11, 12, 13 -> "th"
    else -> when (number % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}
