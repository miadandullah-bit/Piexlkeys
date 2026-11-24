package com.pixelkeys.ime.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CustomThemeEntity::class,
        ClipboardItemEntity::class,
        UserWordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PixelKeysDatabase : RoomDatabase() {
    abstract fun customThemeDao(): CustomThemeDao
    abstract fun clipboardDao(): ClipboardDao
    abstract fun userWordDao(): UserWordDao

    companion object {
        @Volatile
        private var INSTANCE: PixelKeysDatabase? = null

        fun getInstance(context: Context): PixelKeysDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PixelKeysDatabase::class.java,
                    "pixel_keys.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
