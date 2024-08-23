package org.unknownplace.manga

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.unknownplace.manga.ui.theme.MangaTheme
import uniffi.manga.Logger
import uniffi.manga.Manga
import uniffi.manga.initLogger

const val TAG = "MainActivity"
const val LOGGER_TAG = "Core"

class DebugLogger(): Logger {
    override fun log(text: String) {
        Log.d(LOGGER_TAG, text)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SharedContext.setContext(applicationContext)
        initLogger(DebugLogger())

        enableEdgeToEdge()
        setContent {
            MangaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MangaGraph(this)
                }
            }
        }
    }
}

fun Activity.lockScreenOrientationToLandscape() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
}

fun Activity.unlockScreenOrientation() {
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
}