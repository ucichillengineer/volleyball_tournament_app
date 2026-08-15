package com.familydays.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FamilyDaysApp() }
    }
}

actual fun todayMonth(): Int = LocalDate.now().monthValue
actual fun todayDay(): Int = LocalDate.now().dayOfMonth
