package com.sketchbook.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sketchbook.app.data.AppSettings
import com.sketchbook.app.data.DocumentStore
import com.sketchbook.app.model.SketchDocument
import com.sketchbook.app.ui.about.AboutScreen
import com.sketchbook.app.ui.about.FeedbackScreen
import com.sketchbook.app.ui.editor.EditorScreen
import com.sketchbook.app.ui.gallery.GalleryScreen
import com.sketchbook.app.ui.theme.SketchbookTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = DocumentStore(applicationContext)
        val settings = AppSettings(applicationContext)
        setContent {
            var themePref by remember { mutableStateOf(settings.theme) }
            SketchbookTheme(theme = themePref) {
                RootScreen(
                    store = store,
                    settings = settings,
                    onThemeChanged = { themePref = it },
                )
            }
        }
    }
}

@Composable
fun RootScreen(
    store: DocumentStore,
    settings: AppSettings,
    onThemeChanged: (String) -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<SketchDocument?>(null) }

    val current = editing
    if (current != null) {
        // Full-screen editor (mirrors the iOS fullScreenCover).
        EditorScreen(
            initialDocument = current,
            store = store,
            settings = settings,
            onClose = { editing = null },
        )
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(
                    Triple("Sketches", Icons.Filled.GridView, 0),
                    Triple("Feedback", Icons.AutoMirrored.Filled.Chat, 1),
                    Triple("About", Icons.Filled.Info, 2),
                ).forEach { (label, icon, i) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            when (tab) {
                0 -> GalleryScreen(
                    store = store,
                    settings = settings,
                    onOpen = { editing = it },
                    onThemeChanged = onThemeChanged,
                )
                1 -> FeedbackScreen()
                else -> AboutScreen()
            }
        }
    }
}
