package com.example

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val sessionId: String,
    val title: String,
    val updatedAt: String,
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val text: String,
    val isUser: Boolean,
    val modeName: String = StudioMode.CHAT.name,
    val attachedImagePath: String? = null,
    val generatedImagePath: String? = null,
    val styleBadge: String? = null,
    val songTitle: String? = null,
    val songGenre: String? = null,
    val songBpm: String? = null,
    val songChords: String? = null,
    val songVocalLyrics: String? = null,
    val videoTitle: String? = null,
    val videoResolution: String? = null,
    val videoAspectRatio: String? = null,
    val videoVeoModel: String? = null,
    val videoOperationStatus: String? = null,
    val timestamp: String,
    val createdAtEpoch: Long = System.currentTimeMillis()
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val date: String,
    val createdAtEpoch: Long = System.currentTimeMillis()
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAtEpoch ASC")
    fun getMessagesForSessionFlow(sessionId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAtEpoch ASC")
    suspend fun getMessagesForSessionOnce(sessionId: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages ORDER BY createdAtEpoch DESC")
    fun getAllMessagesFlow(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("SELECT * FROM chat_sessions ORDER BY updatedAtEpoch DESC")
    fun getAllSessionsFlow(): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("SELECT * FROM memories ORDER BY createdAtEpoch DESC")
    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT COUNT(*) FROM memories")
    suspend fun getMemoriesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String)
}

@Database(
    entities = [ChatMessageEntity::class, ChatSessionEntity::class, MemoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SuraiyaDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: SuraiyaDatabase? = null

        fun getDatabase(context: Context): SuraiyaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SuraiyaDatabase::class.java,
                    "suraiya_chat_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ChatRepository(
    private val context: Context,
    private val chatDao: ChatDao
) {
    val allSessionsFlow: Flow<List<ChatSessionEntity>> = chatDao.getAllSessionsFlow()

    val allMemoriesFlow: Flow<List<MemoryItem>> = chatDao.getAllMemoriesFlow().map { list ->
        list.map { entity ->
            MemoryItem(
                id = entity.id,
                title = entity.title,
                content = entity.content,
                date = entity.date
            )
        }
    }

    val allGalleryItemsFlow: Flow<List<GalleryItem>> = chatDao.getAllMessagesFlow().map { list ->
        list.mapNotNull { entity ->
            if (entity.isUser) return@mapNotNull null
            val mode = runCatching { StudioMode.valueOf(entity.modeName) }.getOrDefault(StudioMode.CHAT)
            val genBmp = StudioMediaHelper.loadBitmapFromPath(entity.generatedImagePath)
            when {
                mode == StudioMode.PRO_IMAGE_REMAKE && genBmp != null -> GalleryItem(
                    id = entity.id,
                    title = "প্রো রিমেক (${entity.styleBadge ?: "HD"})",
                    category = "PRO_REMAKE",
                    bitmap = genBmp,
                    description = entity.text,
                    timestamp = entity.timestamp
                )
                mode == StudioMode.IMAGE_GEN && genBmp != null -> GalleryItem(
                    id = entity.id,
                    title = entity.text.take(28),
                    category = "AI_IMAGE",
                    bitmap = genBmp,
                    description = entity.text,
                    timestamp = entity.timestamp
                )
                mode == StudioMode.VIDEO_GEN && (genBmp != null || entity.videoTitle != null) -> GalleryItem(
                    id = entity.id,
                    title = "ভিডিও: ${entity.videoTitle ?: entity.text.take(24)}",
                    category = "VIDEO",
                    bitmap = genBmp,
                    description = entity.text,
                    timestamp = entity.timestamp
                )
                mode == StudioMode.MUSIC_GEN && entity.songTitle != null -> GalleryItem(
                    id = entity.id,
                    title = "🎵 গান: ${entity.songTitle}",
                    category = "MUSIC",
                    bitmap = null,
                    description = entity.text,
                    timestamp = entity.timestamp
                )
                else -> null
            }
        }
    }

    suspend fun getMessagesForSession(sessionId: String): List<ChatMessage> {
        return chatDao.getMessagesForSessionOnce(sessionId).map { it.toDomain() }
    }

    suspend fun saveMessage(sessionId: String, message: ChatMessage, sessionTitleHint: String? = null) {
        val entity = message.toEntity(context, sessionId)
        chatDao.insertMessage(entity)
        if (!sessionTitleHint.isNullOrBlank()) {
            chatDao.insertSession(
                ChatSessionEntity(
                    sessionId = sessionId,
                    title = sessionTitleHint.take(40),
                    updatedAt = message.timestamp,
                    updatedAtEpoch = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun upsertSession(sessionId: String, title: String, updatedAt: String) {
        chatDao.insertSession(
            ChatSessionEntity(
                sessionId = sessionId,
                title = title.take(40),
                updatedAt = updatedAt,
                updatedAtEpoch = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSessionAndMessages(sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSession(sessionId)
    }

    suspend fun ensureDefaultMemories() {
        if (chatDao.getMemoriesCount() == 0) {
            chatDao.insertMemory(
                MemoryEntity(
                    id = "default_mem_1",
                    title = "পছন্দের স্টাইল",
                    content = "ব্যবহারকারী প্রফেশনাল ডিজাইন, সিনেমাটিক ইমেজ এবং মিষ্টি বাংলা কথোপকথন পছন্দ করেন।",
                    date = "সংরক্ষিত"
                )
            )
            chatDao.insertMemory(
                MemoryEntity(
                    id = "default_mem_2",
                    title = "সুরাইয়া প্রো স্টুডিও",
                    content = "গ্যালারির ছবি আপলোড করলে সেটি স্টুডিও এইচডি ও লাক্সারি পোর্ট্রেট স্টাইলে প্রফেশনাল করে দেওয়া হয়।",
                    date = "সংরক্ষিত"
                )
            )
        }
    }

    suspend fun insertMemory(item: MemoryItem) {
        chatDao.insertMemory(
            MemoryEntity(
                id = item.id,
                title = item.title,
                content = item.content,
                date = item.date
            )
        )
    }

    suspend fun deleteMemory(id: String) {
        chatDao.deleteMemoryById(id)
    }

    private fun ChatMessage.toEntity(context: Context, sessionId: String): ChatMessageEntity {
        val attachedPath = attachedBitmap?.let {
            StudioMediaHelper.saveBitmapToInternalStorage(context, it, "user_${id.take(6)}")
        }
        val generatedPath = generatedBitmap?.let {
            StudioMediaHelper.saveBitmapToInternalStorage(context, it, "ai_${id.take(6)}")
        }
        return ChatMessageEntity(
            id = id,
            sessionId = sessionId,
            text = text,
            isUser = isUser,
            modeName = mode.name,
            attachedImagePath = attachedPath,
            generatedImagePath = generatedPath,
            styleBadge = styleBadge,
            songTitle = songMetadata?.title,
            songGenre = songMetadata?.genre,
            songBpm = songMetadata?.bpm,
            songChords = songMetadata?.chords,
            songVocalLyrics = songMetadata?.vocalLyrics,
            videoTitle = videoMetadata?.title,
            videoResolution = videoMetadata?.resolution,
            videoAspectRatio = videoMetadata?.aspectRatio,
            videoVeoModel = videoMetadata?.veoModel,
            videoOperationStatus = videoMetadata?.operationStatus,
            timestamp = timestamp,
            createdAtEpoch = System.currentTimeMillis()
        )
    }

    private fun ChatMessageEntity.toDomain(): ChatMessage {
        val studioMode = runCatching { StudioMode.valueOf(modeName) }.getOrDefault(StudioMode.CHAT)
        val attachedBmp = StudioMediaHelper.loadBitmapFromPath(attachedImagePath)
        val generatedBmp = StudioMediaHelper.loadBitmapFromPath(generatedImagePath)
        val songMeta = if (songTitle != null) {
            SongMetadata(
                title = songTitle,
                genre = songGenre ?: "রোমান্টিক মেলোডি",
                bpm = songBpm ?: "96 BPM",
                chords = songChords ?: "Cmaj7 • Am7 • Fmaj7 • G7",
                vocalLyrics = songVocalLyrics ?: text
            )
        } else null

        val videoMeta = if (videoTitle != null) {
            VideoMetadata(
                title = videoTitle,
                resolution = videoResolution ?: "1080p",
                aspectRatio = videoAspectRatio ?: "16:9",
                veoModel = videoVeoModel ?: "veo-3.1-fast-generate-preview",
                operationStatus = videoOperationStatus ?: "স্টুডিও প্রিভিউ রেডি"
            )
        } else null

        return ChatMessage(
            id = id,
            text = text,
            isUser = isUser,
            isLoading = false,
            mode = studioMode,
            attachedBitmap = attachedBmp,
            generatedBitmap = generatedBmp,
            styleBadge = styleBadge,
            songMetadata = songMeta,
            videoMetadata = videoMeta,
            timestamp = timestamp
        )
    }
}
