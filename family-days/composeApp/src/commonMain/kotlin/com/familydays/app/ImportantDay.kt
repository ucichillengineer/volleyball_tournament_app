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
