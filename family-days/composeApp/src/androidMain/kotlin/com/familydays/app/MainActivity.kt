package com.familydays.app

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        setContent { FamilyDaysApp() }
    }

    companion object {
        var appContext: Context? = null
            private set
    }
}

actual fun todayMonth(): Int = LocalDate.now().monthValue
actual fun todayDay(): Int = LocalDate.now().dayOfMonth
actual fun todayYear(): Int = LocalDate.now().year
