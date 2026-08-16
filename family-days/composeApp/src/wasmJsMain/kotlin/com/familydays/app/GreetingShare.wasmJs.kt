package com.familydays.app

import kotlin.js.JsName

@JsName("shareFamilyDaysGreeting")
private external fun browserShareGreeting(greeting: String)

actual fun shareGreetingOnWhatsApp(greeting: String) = browserShareGreeting(greeting)
