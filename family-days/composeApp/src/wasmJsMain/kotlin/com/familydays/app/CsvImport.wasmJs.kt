package com.familydays.app

import kotlin.js.JsName

@JsName("chooseFamilyDaysCsv")
private external fun browserChooseCsv(onSelected: (String) -> Unit)

actual fun chooseCsvFile(onSelected: (String) -> Unit) = browserChooseCsv(onSelected)
