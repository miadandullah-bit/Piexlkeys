package com.pixelkeys.ime

import android.app.Application
import com.pixelkeys.ime.data.local.PixelKeysDatabase
import com.pixelkeys.ime.data.prefs.PreferencesRepository
import com.pixelkeys.ime.haptics.HapticController

class PixelKeysApp : Application() {
    val database: PixelKeysDatabase by lazy { PixelKeysDatabase.getInstance(this) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(this) }
    val hapticController: HapticController by lazy { HapticController(this) }
}
