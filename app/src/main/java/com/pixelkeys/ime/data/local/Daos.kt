package com.pixelkeys.ime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomThemeDao {
    @Query("SELECT * FROM custom_themes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CustomThemeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(theme: CustomThemeEntity)

    @Query("DELETE FROM custom_themes WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard_items ORDER BY isPinned DESC, timestamp DESC LIMIT 50")
    fun observeAll(): Flow<List<ClipboardItemEntity>>

    @Query("SELECT * FROM clipboard_items WHERE text = :text LIMIT 1")
    suspend fun findByText(text: String): ClipboardItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardItemEntity)

    @Query("UPDATE clipboard_items SET timestamp = :timestamp WHERE id = :id")
    suspend fun touchItem(id: Long, timestamp: Long)

    @Query("UPDATE clipboard_items SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM clipboard_items WHERE isPinned = 0")
    suspend fun clearUnpinned()

    @Query(
        "DELETE FROM clipboard_items WHERE isPinned = 0 AND id NOT IN (" +
            "SELECT id FROM clipboard_items WHERE isPinned = 0 ORDER BY timestamp DESC LIMIT :limit" +
            ")"
    )
    suspend fun trimUnpinned(limit: Int)
}

@Dao
interface UserWordDao {
    @Query("SELECT * FROM user_words WHERE language = :language ORDER BY frequency DESC, lastUsedAt DESC LIMIT 200")
    suspend fun getTopWords(language: String): List<UserWordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(word: UserWordEntity)
}
