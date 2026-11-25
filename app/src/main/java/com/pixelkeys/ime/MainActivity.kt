package com.pixelkeys.ime

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pixelkeys.ime.data.prefs.KeyboardPreferences
import com.pixelkeys.ime.ime.PixelKeysImeService
import com.pixelkeys.ime.theme.BuiltInThemes
import com.pixelkeys.ime.ui.SettingsCategory
import com.pixelkeys.ime.ui.SettingsScreen
import com.pixelkeys.ime.ui.StudioPalette
import com.pixelkeys.ime.ui.ThemesScreen
import com.pixelkeys.ime.ui.TrialScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as PixelKeysApp
        app.clipboardSyncManager.startListening()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PixelKeysStudioScreen(app)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            (application as PixelKeysApp).clipboardSyncManager.syncNow()
        }
    }

    override fun onDestroy() {
        (application as PixelKeysApp).clipboardSyncManager.stopListening()
        super.onDestroy()
    }
}

@Composable
fun PixelKeysStudioScreen(app: PixelKeysApp) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val prefs by app.preferencesRepository.preferencesFlow.collectAsState(initial = KeyboardPreferences())
    val customThemes by app.database.customThemeDao().observeAll().collectAsState(initial = emptyList())
    val clipboardItems by app.database.clipboardDao().observeAll().collectAsState(initial = emptyList())

    LaunchedEffect(prefs.clipboardAutoCapture) {
        app.clipboardSyncManager.autoCaptureEnabled = prefs.clipboardAutoCapture
    }

    var imeEnabled by remember { mutableStateOf(isImeEnabled(context)) }
    var imeSelected by remember { mutableStateOf(isImeSelected(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                imeEnabled = isImeEnabled(context)
                imeSelected = isImeSelected(context)
                app.clipboardSyncManager.syncNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val dynamicYou = BuiltInThemes.resolveDynamicYou(context)
    val activeTheme = when (prefs.activeThemeId) {
        "dynamic_you" -> dynamicYou
        else -> BuiltInThemes.allBuiltIns.firstOrNull { it.id == prefs.activeThemeId }
            ?: customThemes.firstOrNull { it.id == prefs.activeThemeId }?.toThemeSpec()
            ?: BuiltInThemes.PixelBlock
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var settingsCategory by remember { mutableStateOf(SettingsCategory.ALL) }

    Scaffold(
        containerColor = StudioPalette.Background,
        bottomBar = {
            NavigationBar(containerColor = StudioPalette.Surface) {
                val navColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = activeTheme.accentColor,
                    selectedTextColor = activeTheme.accentColor,
                    indicatorColor = activeTheme.accentColor.copy(alpha = 0.16f),
                    unselectedIconColor = StudioPalette.TextSecondary,
                    unselectedTextColor = StudioPalette.TextSecondary
                )
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Outlined.Keyboard, contentDescription = "Trial") },
                    label = { Text("Trial") },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.Palette, contentDescription = "Themes") },
                    label = { Text("Themes") },
                    colors = navColors
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Outlined.Tune, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = navColors
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> TrialScreen(
                    app = app,
                    prefs = prefs,
                    theme = activeTheme,
                    clipboardItems = clipboardItems,
                    imeEnabled = imeEnabled,
                    imeSelected = imeSelected,
                    onOpenThemes = { selectedTab = 1 },
                    onOpenSettings = {
                        settingsCategory = SettingsCategory.ALL
                        selectedTab = 2
                    }
                )
                1 -> ThemesScreen(
                    app = app,
                    prefs = prefs,
                    activeTheme = activeTheme,
                    dynamicYou = dynamicYou,
                    customThemes = customThemes,
                    onSelectTheme = { id ->
                        scope.launch { app.preferencesRepository.setThemeId(id) }
                    },
                    onOpenAiSettings = {
                        settingsCategory = SettingsCategory.AI_SERVICE
                        selectedTab = 2
                    }
                )
                else -> SettingsScreen(
                    app = app,
                    prefs = prefs,
                    theme = activeTheme,
                    clipboardCount = clipboardItems.size,
                    imeEnabled = imeEnabled,
                    imeSelected = imeSelected,
                    selectedCategory = settingsCategory,
                    onCategoryChange = { settingsCategory = it }
                )
            }
        }
    }
}

private fun isImeEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
    return imm.enabledInputMethodList.any { it.packageName == context.packageName }
}

private fun isImeSelected(context: Context): Boolean {
    val defaultIme = runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    }.getOrNull() ?: return false
    val expected = ComponentName(context, PixelKeysImeService::class.java).flattenToShortString()
    return defaultIme == expected || defaultIme.startsWith("${context.packageName}/")
}
