package me.fishhawk.lisu.library

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.fishhawk.lisu.library.model.*
import me.fishhawk.lisu.util.*
import java.nio.file.Path

private val json = Json { ignoreUnknownKeys = true }

private inline fun <reified T> Path.deserializeAs(): Result<T> {
    return readText().andThen { safeRunCatching { json.decodeFromString(it) } }
}

private inline fun <reified T> Path.serialize(item: T): Result<Unit> {
    return safeRunCatching { json.encodeToString(item) }
        .andThen { writeText(it) }
}

class MangaAccessor(val path: Path) {
    val id: String
        get() = path.name

    fun getSearchEntry(): SearchEntry {
        return metadataPath.deserializeAs<SearchEntry>()
            .getOrNull()
            ?.let { if (it.title == null) it.copy(title = id) else it }
            ?: SearchEntry(title = id)
    }

    fun get(): Manga {
        return getMetadata().getOrNull().let {
            Manga(
                id = id,

                cover = null,
                updateTime = path.getLastModifiedTime(),
                title = it?.title,
                authors = it?.authors ?: emptyList(),
                isFinished = it?.isFinished,
            )
        }
    }

    fun getDetail(): MangaDetail {
        val metadata = getMetadata().getOrNull()
        val chapterMetadata = getChapterMetadata().getOrNull()
        return MangaDetail(
            id = id,

            cover = null,
            updateTime = path.getLastModifiedTime(),
            title = metadata?.title,
            authors = metadata?.authors ?: emptyList(),
            isFinished = metadata?.isFinished,

            description = metadata?.description,
            tags = metadata?.tags ?: emptyMap(),

            collections = detectCollections(path, chapterMetadata),
            chapterPreviews = detectPreviews(path),
        )
    }

    private val chapterMetadataPath get() = path.resolve("chapterMetadata.json")
    fun hasChapterMetadata(): Boolean = chapterMetadataPath.isRegularFile()
    fun setChapterMetadata(metadata: MangaChapterMetadata): Result<Unit> = chapterMetadataPath.serialize(metadata)
    private fun getChapterMetadata(): Result<MangaChapterMetadata> = chapterMetadataPath.deserializeAs()

    private val metadataPath get() = path.resolve("metadata.json")
    fun hasMetadata(): Boolean = metadataPath.isRegularFile()
    fun setMetadata(metadata: MangaMetadata): Result<Unit> = metadataPath.serialize(metadata)
    private fun getMetadata(): Result<MangaMetadata> = metadataPath.deserializeAs()

    fun hasCover(): Boolean {
        return path.listImageFiles()
            .getOrDefault(emptyList())
            .isEmpty()
    }

    fun getCover(): Image? {
        return path.listImageFiles()
            .getOrNull()
            ?.let { covers ->
                covers.firstOrNull { it.nameWithoutExtension == "cover" }
                    ?: covers.sortedAlphanumeric().firstOrNull()
            }
            ?.toImage()
    }

    fun setCover(cover: Image): Result<Unit> {
        path.listImageFiles()
            .getOrNull()
            ?.filter { it.nameWithoutExtension == "cover" }
            ?.onEach { it.delete() }
        return path.resolve("cover.${cover.ext}")
            .outputStream()
            .map { cover.stream.copyTo(it).discard() }
    }

    private fun getChapterPath(
        collectionId: String,
        chapterId: String
    ): Result<Pair<Path, ChapterAccessor.Depth>> {
        return if (collectionId.isNotBlank()) {
            if (collectionId.isFilename() && chapterId.isFilename()) {
                Result.success(Pair(path.resolve(collectionId).resolve(chapterId), ChapterAccessor.Depth.Two))
            } else {
                Result.failure(LibraryException.ChapterIllegalId(collectionId, chapterId))
            }
        } else if (chapterId.isNotBlank()) {
            if (chapterId.isFilename()) {
                Result.success(Pair(path.resolve(chapterId), ChapterAccessor.Depth.One))
            } else {
                Result.failure(LibraryException.ChapterIllegalId(collectionId, chapterId))
            }
        } else {
            Result.success(Pair(path, ChapterAccessor.Depth.Zero))
        }
    }

    fun getChapter(collectionId: String, chapterId: String): Result<ChapterAccessor> {
        return getChapterPath(collectionId, chapterId)
            .andThen { (path, depth) ->
                if (path.isDirectory()) {
                    Result.success(ChapterAccessor(path, depth))
                } else {
                    Result.failure(LibraryException.ChapterNotFound(collectionId, chapterId))
                }
            }
    }

    fun createChapter(collectionId: String, chapterId: String): Result<ChapterAccessor> {
        return getChapterPath(collectionId, chapterId)
            .andThen { (path, depth) ->
                if (path.isDirectory()) {
                    Result.success(ChapterAccessor(path, depth))
                } else {
                    path.createDirAll().map {
                        ChapterAccessor(path, depth).apply { setUnfinished() }
                    }
                }
            }
    }
}

private fun detectCollections(
    path: Path,
    chapterMetadata: MangaChapterMetadata?,
): Map<String, List<Chapter>> {
    fun detectChapters(collectionId: String, path: Path): List<Chapter> {
        return path
            .listDirectory()
            .getOrDefault(emptyList())
            .sortedWith(alphanumericOrder())
            .map {
                val chapterId = it.name
                val metadata = chapterMetadata?.collections?.get(collectionId)?.get(chapterId)
                if (metadata == null) {
                    Chapter(
                        id = chapterId,
                        name = chapterId.substringBefore(" "),
                        title = chapterId.substringAfter(" ").ifBlank { null },
                        updateTime = it.getLastModifiedTime(),
                    )
                } else {
                    Chapter(
                        id = chapterId,
                        name = metadata.name,
                        title = metadata.title,
                        updateTime = it.getLastModifiedTime(),
                    )
                }
            }
    }

    val depth2 = path
        .listDirectory()
        .getOrDefault(emptyList())
        .sortedWith(alphanumericOrder())
        .associate { it.name to detectChapters(it.name, it) }
        .filter { it.value.isNotEmpty() }
    if (depth2.isNotEmpty()) return depth2

    val depth1 = detectChapters("", path)
    if (depth1.isNotEmpty()) return mapOf("" to depth1)

    if (detectPreviews(path).isEmpty())
        return emptyMap()

    val depth0 = Chapter(id = "")
    return mapOf("" to listOf(depth0))
}

private fun detectPreviews(path: Path): List<String> {
    return path
        .listImageFiles()
        .getOrDefault(emptyList())
        .map { it.name }
        .sortedWith(naturalOrder())
        .take(20)
}
