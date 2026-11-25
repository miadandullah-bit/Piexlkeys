package com.pixelkeys.ime.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.pixelkeys.ime.PixelKeysApp
import com.pixelkeys.ime.data.prefs.KeyboardPreferences
import com.pixelkeys.ime.engine.VoiceTypingController
import com.pixelkeys.ime.keyboard.KeyboardController
import com.pixelkeys.ime.keyboard.PixelKeysBoard
import com.pixelkeys.ime.theme.BuiltInThemes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PixelKeysImeService : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var keyboardController: KeyboardController
    private lateinit var voiceController: VoiceTypingController

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        val app = application as PixelKeysApp
        app.clipboardSyncManager.startListening()

        val adapter = InputConnectionTargetAdapter(
            inputConnectionProvider = { currentInputConnection },
            editorInfoProvider = { currentInputEditorInfo }
        )
        keyboardController = KeyboardController(
            scope = serviceScope,
            prefsRepo = app.preferencesRepository,
            hapticController = app.hapticController,
            target = adapter
        )
        voiceController = VoiceTypingController(this) { spoken ->
            keyboardController.commitTextDirectly(spoken)
        }

        serviceScope.launch {
            app.preferencesRepository.preferencesFlow.collect { prefs ->
                keyboardController.updatePreferences(prefs)
                app.clipboardSyncManager.autoCaptureEnabled = prefs.clipboardAutoCapture
            }
        }
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        keyboardController.resetSession()
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        keyboardController.resetSession()
        (application as PixelKeysApp).clipboardSyncManager.syncNow()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        currentInputConnection?.finishComposingText()
        keyboardController.resetSession()
        voiceController.stopListening()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onCreateInputView(): View {
        val decorView = window?.window?.decorView
        decorView?.let {
            it.setViewTreeLifecycleOwner(this)
            it.setViewTreeViewModelStoreOwner(this)
            it.setViewTreeSavedStateRegistryOwner(this)
        }

        val app = application as PixelKeysApp
        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PixelKeysImeService)
            setViewTreeViewModelStoreOwner(this@PixelKeysImeService)
            setViewTreeSavedStateRegistryOwner(this@PixelKeysImeService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val prefs by app.preferencesRepository.preferencesFlow.collectAsState(initial = KeyboardPreferences())
                val customThemes by app.database.customThemeDao().observeAll().collectAsState(initial = emptyList())
                val clipboardItems by app.database.clipboardDao().observeAll().collectAsState(initial = emptyList())

                val dynamicYou = BuiltInThemes.resolveDynamicYou(this@PixelKeysImeService)
                val theme = when (prefs.activeThemeId) {
                    "dynamic_you" -> dynamicYou
                    else -> BuiltInThemes.allBuiltIns.firstOrNull { it.id == prefs.activeThemeId }
                        ?: customThemes.firstOrNull { it.id == prefs.activeThemeId }?.toThemeSpec()
                        ?: BuiltInThemes.PixelBlock
                }

                PixelKeysBoard(
                    controller = keyboardController,
                    prefs = prefs,
                    prefsRepo = app.preferencesRepository,
                    theme = theme,
                    clipboardItems = clipboardItems,
                    clipboardDao = app.database.clipboardDao(),
                    clipboardSyncManager = app.clipboardSyncManager,
                    voiceController = voiceController
                )
            }
        }
    }

    override fun onDestroy() {
        (application as PixelKeysApp).clipboardSyncManager.stopListening()
        voiceController.stopListening()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        super.onDestroy()
    }
}
