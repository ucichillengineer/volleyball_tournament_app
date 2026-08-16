package com.familydays.app

data class Observance(val title: String, val greeting: String)

fun todayObservances(year: Int, month: Int, day: Int): List<Observance> {
    val observances = mutableListOf<Observance>()
    fun add(monthValue: Int, dayValue: Int, title: String, greeting: String) {
        if (month == monthValue && day == dayValue) observances += Observance(title, greeting)
    }

    add(1, 1, "New Year's Day", "Happy New Year! Wishing you peace, health, and happiness.")
    add(1, 26, "India Republic Day", "Happy Republic Day, India!")
    add(2, 14, "Valentine's Day", "Happy Valentine's Day! Celebrating love and togetherness.")
    add(7, 4, "U.S. Independence Day", "Happy Independence Day! Have a safe and joyful Fourth of July.")
    add(8, 15, "India Independence Day", "Happy Independence Day, India! Jai Hind!")
    add(10, 2, "Gandhi Jayanti", "Happy Gandhi Jayanti. May peace and truth guide us.")
    add(10, 31, "Halloween", "Happy Halloween! Have a fun and safe celebration.")
    add(12, 25, "Christmas", "Merry Christmas! Wishing you joy, peace, and warmth.")

    if (month == 5 && day == nthWeekdayOfMonth(year, 5, weekday = SUNDAY, occurrence = 2)) {
        observances += Observance("Mother's Day", "Happy Mother's Day! Thank you for your love and care.")
    }
    if (month == 6 && day == nthWeekdayOfMonth(year, 6, weekday = SUNDAY, occurrence = 3)) {
        observances += Observance("Father's Day", "Happy Father's Day! Thank you for your guidance and support.")
    }
    if (month == 11 && day == nthWeekdayOfMonth(year, 11, weekday = THURSDAY, occurrence = 4)) {
        observances += Observance("Thanksgiving", "Happy Thanksgiving! Grateful for family, friends, and good health.")
    }
    return observances
}

private const val SUNDAY = 0
private const val THURSDAY = 4

private fun nthWeekdayOfMonth(year: Int, month: Int, weekday: Int, occurrence: Int): Int {
    val firstWeekday = weekdayFor(year, month, 1)
    return 1 + ((weekday - firstWeekday + 7) % 7) + (occurrence - 1) * 7
}

/** Gregorian calendar weekday where Sunday is 0. */
private fun weekdayFor(year: Int, month: Int, day: Int): Int {
    val offsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val adjustedYear = if (month < 3) year - 1 else year
    return (adjustedYear + adjustedYear / 4 - adjustedYear / 100 + adjustedYear / 400 + offsets[month - 1] + day) % 7
}
