package com.familydays.app

import kotlin.js.JsName

@JsName("Date")
private external class BrowserDate {
    constructor()
    fun getMonth(): Int
    fun getDate(): Int
    fun getFullYear(): Int
}

actual fun todayMonth(): Int = BrowserDate().getMonth() + 1
actual fun todayDay(): Int = BrowserDate().getDate()
actual fun todayYear(): Int = BrowserDate().getFullYear()
