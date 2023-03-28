package data

import com.mongodb.client.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
import org.litote.kmongo.*
import java.time.LocalDateTime
import java.util.UUID

@Serializable
enum class MangaReadMode {
    @SerialName("rtl")
    Rtl,

    @SerialName("ltr")
    Ltr,

    @SerialName("strip")
    Strip,
}

@Serializable
data class MongoManga(
    @Contextual @SerialName("_id") val id: ObjectId,
    @Contextual val updateAt: LocalDateTime,
    val title: String,
    val titleAlt: List<TitleAlt>,
    val authors: List<String>,
    val artists: List<String>,
    val tag: List<String>,
    val format: List<String>,
    val isFinished: Boolean,
    val description: String,
    val collections: List<Collection>,
    val readMode: MangaReadMode,
) {
    @Serializable
    data class TitleAlt(
        val title: String,
        val lang: String,
    )

    @Serializable
    data class Collection(
        val id: String,
        val label: String,
        val chapters: List<Chapter>,
    )

    @Serializable
    data class Chapter(
        val id: String,
        val readMode: MangaReadMode?,
        val label: String,
        val title: String,
        @Contextual val updateAt: LocalDateTime,
    )
}

class MongoMangaRepository(
    val mongo: MongoDataSource,
) {
    private val col
        get() = mongo.database.getCollection<MongoManga>("manga")

    suspend fun get(
        id: String,
    ): MongoManga? {
        return col.findOne(
            MongoManga::id eq ObjectId(id),
        )
    }

    suspend fun addCollection(
        id: String,
        label: String,
    ): MongoManga? {
        return col.findOneAndUpdate(
            MongoManga::id eq ObjectId(id),
            addToSet(
                MongoManga::collections, MongoManga.Collection(
                    id = UUID.randomUUID().toString(),
                    label = label,
                    chapters = emptyList(),
                )
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
    }

    suspend fun deleteCollection(
        id: String,
        collectionId: String,
    ): MongoManga? {
        return col.findOneAndUpdate(
            MongoManga::id eq ObjectId(id),
            pullByFilter(
                MongoManga::collections,
                and(
                    MongoManga::collections / MongoManga.Collection::id eq collectionId,
                    MongoManga::collections / MongoManga.Collection::chapters.pos(0) exists false,
                ),
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
    }

    suspend fun addChapter(
        id: String,
        collectionIndex: Int,
        chapterId: String,
        readMode: MangaReadMode?,
        label: String,
        title: String,
    ): MongoManga? {
        return col.findOneAndUpdate(
            MongoManga::id eq ObjectId(id),
            addToSet(
                MongoManga::collections.pos(collectionIndex) / MongoManga.Collection::chapters,
                MongoManga.Chapter(
                    id = chapterId,
                    readMode = readMode,
                    label = label,
                    title = title,
                    updateAt = LocalDateTime.now(),
                )
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER),
        )
    }
}