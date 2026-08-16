package com.familydays.app

import android.os.Bundle
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private var csvCallback: ((String) -> Unit)? = null
    private var csvToExport: String? = null
    private val csvPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val csv = uri?.let {
            contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() }
        }
        if (csv != null) csvCallback?.invoke(csv)
        csvCallback = null
    }
    private val csvExporter = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val csv = csvToExport
        if (uri != null && csv != null) {
            contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer -> writer.write(csv) }
        }
        csvToExport = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        currentActivity = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
        }
        setContent { FamilyDaysApp() }
    }

    fun selectCsv(onSelected: (String) -> Unit) {
        csvCallback = onSelected
        // Samsung's Downloads provider often reports CSV files as application/octet-stream.
        csvPicker.launch(arrayOf("*/*"))
    }

    fun exportCsv(csv: String) {
        csvToExport = csv
        csvExporter.launch("family-days-events.csv")
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 100
        var appContext: Context? = null
            private set
        var currentActivity: MainActivity? = null
            private set
    }
}

actual fun todayMonth(): Int = LocalDate.now().monthValue
actual fun todayDay(): Int = LocalDate.now().dayOfMonth
actual fun todayYear(): Int = LocalDate.now().year
