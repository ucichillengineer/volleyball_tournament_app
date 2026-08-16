package com.familydays.app

import kotlin.js.JsName

@JsName("loadFamilyDaysEvents")
private external fun browserLoadEvents(onLoaded: (String?) -> Unit)

@JsName("saveFamilyDaysEvents")
private external fun browserSaveEvents(csv: String)

actual fun loadSavedEvents(onLoaded: (String?) -> Unit) = browserLoadEvents(onLoaded)
actual fun saveEvents(csv: String) = browserSaveEvents(csv)
