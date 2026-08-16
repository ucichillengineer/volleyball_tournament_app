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
        EventType.ANNIVERSARY -> anniversaryGreeting(name, years)
    }
    return greetingTemplate
        ?.replace("{name}", name)
        ?.replace("{years}", years?.toString().orEmpty())
        ?.replace("{event}", type.label.lowercase())
        ?.takeIf { it.isNotBlank() }
        ?: defaultGreeting
}

private fun anniversaryGreeting(name: String, years: Int?): String {
    if (years == null) return "Happy Anniversary, $name!"
    val milestone = anniversaryMilestones[years]
    val celebration = when (years) {
        50 -> "Golden Jubilee"
        60 -> "Diamond Jubilee"
        else -> milestone?.let { "$it Anniversary" } ?: "${years}${ordinalSuffix(years)} Anniversary"
    }
    return "Happy $celebration, $name! Celebrating $years wonderful years together."
}

private val anniversaryMilestones = mapOf(
    1 to "Paper", 2 to "Cotton", 3 to "Leather", 4 to "Fruit and Flowers",
    5 to "Wood", 6 to "Iron", 7 to "Wool", 8 to "Bronze", 9 to "Pottery",
    10 to "Tin", 11 to "Steel", 12 to "Silk", 13 to "Lace", 14 to "Ivory",
    15 to "Crystal", 20 to "China", 25 to "Silver", 30 to "Pearl",
    35 to "Coral", 40 to "Ruby", 45 to "Sapphire", 55 to "Emerald"
)

private fun ordinalSuffix(number: Int): String = when (number % 100) {
    11, 12, 13 -> "th"
    else -> when (number % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}
