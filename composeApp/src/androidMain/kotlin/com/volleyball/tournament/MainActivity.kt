package com.volleyball.tournament

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.volleyball.tournament.data.initAndroidStorage
import com.volleyball.tournament.ui.VolleyballApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initAndroidStorage(this)
        initAndroidShare(this)
        setContent {
            VolleyballApp()
        }
    }
}
