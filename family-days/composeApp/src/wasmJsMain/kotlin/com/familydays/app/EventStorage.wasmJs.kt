package com.familydays.app

import kotlin.js.JsName

@JsName("loadFamilyDaysEvents")
private external fun browserLoadEvents(onLoaded: (String?) -> Unit)

@JsName("saveFamilyDaysEvents")
private external fun browserSaveEvents(csv: String)

@JsName("downloadFamilyDaysCsv")
private external fun browserDownloadCsv(csv: String)

actual fun loadSavedEvents(onLoaded: (String?) -> Unit) = browserLoadEvents(onLoaded)
actual fun saveEvents(csv: String) = browserSaveEvents(csv)
actual fun exportEventsCsv(csv: String) = browserDownloadCsv(csv)
