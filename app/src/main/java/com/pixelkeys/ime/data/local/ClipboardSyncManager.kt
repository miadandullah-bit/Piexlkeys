package com.pixelkeys.ime.data.local

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Bridges Android's system ClipboardManager with Pixelkeys' Room-backed clipboard history.
 * Captures clips when the IME or companion app has focus and deduplicates entries.
 */
class ClipboardSyncManager(
    private val context: Context,
    private val clipboardDao: ClipboardDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clipboardManager: ClipboardManager? =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    @Volatile
    var autoCaptureEnabled: Boolean = true

    private var listenerRefs = 0
    private val _lastSystemClip = MutableStateFlow<String?>(null)
    val lastSystemClip: StateFlow<String?> = _lastSystemClip.asStateFlow()

    private val primaryClipListener = ClipboardManager.OnPrimaryClipChangedListener {
        syncNow(forceCapture = false)
    }

    @Synchronized
    fun startListening() {
        if (listenerRefs == 0) {
            runCatching {
                clipboardManager?.addPrimaryClipChangedListener(primaryClipListener)
            }
        }
        listenerRefs++
        syncNow(forceCapture = false)
    }

    @Synchronized
    fun stopListening() {
        if (listenerRefs > 0) {
            listenerRefs--
            if (listenerRefs == 0) {
                runCatching {
                    clipboardManager?.removePrimaryClipChangedListener(primaryClipListener)
                }
            }
        }
    }

    fun readCurrentSystemClip(): String? {
        val cm = clipboardManager ?: return null
        val clip = runCatching { cm.primaryClip }.getOrNull() ?: return null
        if (clip.itemCount <= 0) return null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val isSensitive = clip.description.extras
                ?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE, false) == true
            if (isSensitive) return null
        }

        val text = runCatching {
            clip.getItemAt(0).coerceToText(context)?.toString()
        }.getOrNull()?.trim().orEmpty()

        if (text.isBlank()) return null
        return text.take(4000)
    }

    fun syncNow(forceCapture: Boolean = false) {
        val text = readCurrentSystemClip()
        _lastSystemClip.value = text
        if (text.isNullOrBlank()) return
        if (!autoCaptureEnabled && !forceCapture) return

        scope.launch {
            saveClipText(text)
        }
    }

    suspend fun saveClipText(rawText: String) {
        val text = rawText.trim().take(4000)
        if (text.isEmpty()) return
        val existing = clipboardDao.findByText(text)
        val now = System.currentTimeMillis()
        if (existing != null) {
            clipboardDao.touchItem(existing.id, now)
        } else {
            clipboardDao.insert(ClipboardItemEntity(text = text, timestamp = now))
            clipboardDao.trimUnpinned(50)
        }
    }
}
