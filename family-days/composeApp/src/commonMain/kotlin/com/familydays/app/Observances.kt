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
    add(6, 19, "Juneteenth", "Happy Juneteenth! Honoring freedom and celebrating community.")
    add(11, 11, "Veterans Day", "Thank you to all veterans for your service and sacrifice.")

    // U.S. federal and family observances.
    addWeekday(year, month, day, 1, MONDAY, 3, "Martin Luther King Jr. Day", "Honoring Dr. Martin Luther King Jr. and his legacy of justice and service.", observances)
    addWeekday(year, month, day, 2, MONDAY, 3, "Presidents' Day", "Happy Presidents' Day!", observances)
    if (month == 5 && day == nthWeekdayOfMonth(year, 5, weekday = SUNDAY, occurrence = 2)) {
        observances += Observance("Mother's Day", "Happy Mother's Day! Thank you for your love and care.")
    }
    if (month == 5 && day == lastWeekdayOfMonth(year, 5, MONDAY)) {
        observances += Observance("Memorial Day", "Honoring and remembering those who gave their lives in service.")
    }
    if (month == 6 && day == nthWeekdayOfMonth(year, 6, weekday = SUNDAY, occurrence = 3)) {
        observances += Observance("Father's Day", "Happy Father's Day! Thank you for your guidance and support.")
    }
    addWeekday(year, month, day, 9, MONDAY, 1, "Labor Day", "Happy Labor Day! Wishing you a restful day.", observances)
    if (month == 11 && day == nthWeekdayOfMonth(year, 11, weekday = THURSDAY, occurrence = 4)) {
        observances += Observance("Thanksgiving", "Happy Thanksgiving! Grateful for family, friends, and good health.")
    }

    // Lunar dates vary by region. These are the major 2026 India/New Delhi observances.
    if (year == 2026) {
        HINDU_FESTIVALS_2026
            .filter { it.month == month && it.day == day }
            .forEach { observances += Observance(it.name, it.greeting) }
    }
    return observances
}

private const val SUNDAY = 0
private const val MONDAY = 1
private const val THURSDAY = 4

private fun addWeekday(
    year: Int, month: Int, day: Int, targetMonth: Int, weekday: Int, occurrence: Int,
    title: String, greeting: String, observances: MutableList<Observance>
) {
    if (month == targetMonth && day == nthWeekdayOfMonth(year, targetMonth, weekday, occurrence)) {
        observances += Observance(title, greeting)
    }
}

private fun nthWeekdayOfMonth(year: Int, month: Int, weekday: Int, occurrence: Int): Int {
    val firstWeekday = weekdayFor(year, month, 1)
    return 1 + ((weekday - firstWeekday + 7) % 7) + (occurrence - 1) * 7
}

private fun lastWeekdayOfMonth(year: Int, month: Int, weekday: Int): Int {
    val days = intArrayOf(31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val lastDay = days[month - 1]
    return lastDay - ((weekdayFor(year, month, lastDay) - weekday + 7) % 7)
}

private fun isLeapYear(year: Int) = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

/** Gregorian calendar weekday where Sunday is 0. */
private fun weekdayFor(year: Int, month: Int, day: Int): Int {
    val offsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val adjustedYear = if (month < 3) year - 1 else year
    return (adjustedYear + adjustedYear / 4 - adjustedYear / 100 + adjustedYear / 400 + offsets[month - 1] + day) % 7
}

private data class HinduFestival(val month: Int, val day: Int, val name: String, val greeting: String)

private val HINDU_FESTIVALS_2026 = listOf(
    HinduFestival(1, 14, "Makar Sankranti / Pongal", "Happy Makar Sankranti and Pongal! May the harvest bring abundance and joy."),
    HinduFestival(2, 15, "Maha Shivaratri", "Happy Maha Shivaratri! Om Namah Shivaya."),
    HinduFestival(3, 4, "Holi", "Happy Holi! Wishing you a colorful celebration filled with joy."),
    HinduFestival(3, 19, "Ugadi / Gudi Padwa", "Happy Ugadi! Wishing you a prosperous and joyful new year."),
    HinduFestival(3, 27, "Rama Navami", "Happy Sri Rama Navami! May Lord Rama bless your home with peace."),
    HinduFestival(4, 2, "Hanuman Jayanti", "Happy Hanuman Jayanti! Jai Hanuman."),
    HinduFestival(4, 20, "Akshaya Tritiya", "Happy Akshaya Tritiya! May your prosperity be everlasting."),
    HinduFestival(8, 28, "Raksha Bandhan", "Happy Raksha Bandhan! Celebrating the bond of love and protection."),
    HinduFestival(9, 4, "Krishna Janmashtami", "Happy Krishna Janmashtami! Jai Shri Krishna."),
    HinduFestival(9, 14, "Ganesh Chaturthi", "Happy Ganesh Chaturthi! Ganpati Bappa Morya."),
    HinduFestival(10, 12, "Navratri", "Happy Navratri! May Maa Durga bless you with strength and joy."),
    HinduFestival(10, 20, "Dussehra / Vijayadashami", "Happy Dussehra! May good always triumph over evil."),
    HinduFestival(11, 8, "Diwali / Deepavali", "Happy Diwali! May your life shine with joy, prosperity, and peace.")
)
