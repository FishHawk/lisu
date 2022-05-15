package me.fishhawk.lisu.library

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import me.fishhawk.lisu.model.*
import me.fishhawk.lisu.util.*
import java.nio.file.Path

private val json = Json { ignoreUnknownKeys = true }

class Manga(val path: Path) {
    val providerId: String
        get() = path.parent.name

    val id: String
        get() = path.name

    fun getSearchEntry(): SearchEntry {
        return parseMetadataAs(SearchEntry.serializer())
            ?.let { if (it.title == null) it.copy(title = id) else it }
            ?: SearchEntry(title = id)
    }

    fun get(): MangaDto {
        return parseMetadataAs(MangaMetadataDto.serializer())
            .let {
                MangaDto(
                    providerId = providerId,
                    id = id,
                    cover = null,
                    updateTime = path.getLastModifiedTime().toInstant().epochSecond,
                    title = it?.title,
                    authors = it?.authors,
                    isFinished = it?.isFinished
                )
            }
    }

    fun getDetail(): MangaDetailDto {
        return parseMetadataAs(MangaMetadataDto.serializer())
            .let {
                val collections =
                    if (it?.collections != null) buildCollectionsFromMetadata(it.collections)
                    else if (it?.chapters == null) detectCollections(path).ifEmpty { null }
                    else null
                val chapters =
                    if (collections != null) null
                    else if (it?.chapters == null) detectChapters(path).ifEmpty { null }
                    else buildChaptersFromMetadata(it.chapters)
                val previews =
                    if (collections != null || chapters != null) null
                    else detectPreviews(path).ifEmpty { null }
                MangaDetailDto(
                    providerId = providerId,
                    id = id,

                    inLibrary = true,

                    cover = null,
                    updateTime = path.getLastModifiedTime().toInstant().epochSecond,

                    title = it?.title,
                    authors = it?.authors,
                    isFinished = it?.isFinished,
                    description = it?.description,
                    tags = it?.tags,
                    collections = collections,
                    chapters = chapters,
                    preview = previews
                )
            }
    }

    private val metadataPath
        get() = path.resolve("metadata.json")

    fun hasMetadata(): Boolean {
        return metadataPath.isRegularFile()
    }

    private fun <T> parseMetadataAs(serializer: KSerializer<T>): T? {
        return metadataPath.readText()
            .mapCatchingException { json.decodeFromString(serializer, it) }
            .getOrNull()
    }

    fun setMetadata(metadata: MangaMetadataDto): Result<Unit> {
        return runCatchingException { json.encodeToString(MangaMetadataDto.serializer(), metadata) }
            .then(metadataPath::writeText)
    }

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
    ): Result<Pair<Path, Chapter.Depth>> {
        return if (collectionId.isNotEmpty()) {
            path.resolveChild(collectionId)
                .then { it.resolveChild(chapterId) }
                .map { Pair(it, Chapter.Depth.Two) }
        } else if (chapterId.isNotEmpty()) {
            path.resolveChild(chapterId)
                .map { Pair(it, Chapter.Depth.One) }
        } else {
            Result.success(path)
                .map { Pair(it, Chapter.Depth.Zero) }
        }
    }

    fun getChapter(collectionId: String, chapterId: String): Chapter? {
        return getChapterPath(collectionId, chapterId)
            .getOrNull()
            ?.takeIf { it.first.isDirectory() }
            ?.let { Chapter(it.first, it.second) }
    }

    fun createChapter(collectionId: String, chapterId: String): Result<Chapter> {
        return getChapterPath(collectionId, chapterId)
            .onSuccess { it.first.createDirAll() }
            .map { Chapter(it.first, it.second) }
    }
}

private fun buildCollectionsFromMetadata(
    collectionsMetadata: Map<String, Map<String, ChapterMetadataDto>>
): Map<String, List<ChapterDto>> {
    return collectionsMetadata.mapValues { (_, chaptersMetadata) ->
        buildChaptersFromMetadata(chaptersMetadata)
    }
}

private fun buildChaptersFromMetadata(
    chaptersMetadata: Map<String, ChapterMetadataDto>
): List<ChapterDto> {
    return chaptersMetadata.map { (chapterId, metadata) ->
        ChapterDto(
            id = chapterId,
            name = metadata.name,
            title = metadata.title
        )
    }
}

private fun detectCollections(path: Path): Map<String, List<ChapterDto>> {
    return path
        .listDirectory()
        .getOrDefault(emptyList())
        .sortedWith(alphanumericOrder())
        .associate { it.name to detectChapters(it) }
        .filter { it.value.isNotEmpty() }
}

private fun detectChapters(path: Path): List<ChapterDto> {
    return path
        .listDirectory()
        .getOrDefault(emptyList())
        .sortedWith(alphanumericOrder())
        .map {
            val id = it.name
            ChapterDto(
                id = id,
                name = id.substringBefore(" "),
                title = id.substringAfter(" ").ifBlank { null },
//                updateTime = it.getLastModifiedTime().toInstant().epochSecond
            )
        }
}

private fun detectPreviews(path: Path): List<String> {
    return path
        .listImageFiles()
        .getOrDefault(emptyList())
        .map { it.name }
        .sortedWith(naturalOrder())
        .take(20)
}
