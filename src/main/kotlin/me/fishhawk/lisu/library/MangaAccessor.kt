package me.fishhawk.lisu.library

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import me.fishhawk.lisu.library.model.*
import me.fishhawk.lisu.util.*
import java.nio.file.Path

private val json = Json { ignoreUnknownKeys = true }

class MangaAccessor(val path: Path) {
    val id: String
        get() = path.name

    fun getSearchEntry(): SearchEntry {
        return parseMetadataAs(SearchEntry.serializer())
            ?.let { if (it.title == null) it.copy(title = id) else it }
            ?: SearchEntry(title = id)
    }

    fun get(): Manga {
        return parseMetadataAs(MangaMetadata.serializer())
            .let {
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
        return parseMetadataAs(MangaMetadata.serializer())
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
                MangaDetail(
                    id = id,

                    cover = null,
                    updateTime = path.getLastModifiedTime(),
                    title = it?.title,
                    authors = it?.authors ?: emptyList(),
                    isFinished = it?.isFinished,

                    description = it?.description,
                    tags = it?.tags ?: emptyMap(),

                    content = when {
                        collections != null -> MangaContent.Collections(collections)
                        chapters != null -> MangaContent.Chapters(chapters)
                        else -> MangaContent.SingleChapter(previews ?: emptyList())
                    },
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

    fun setMetadata(metadata: MangaMetadata): Result<Unit> {
        return runCatchingException { json.encodeToString(MangaMetadata.serializer(), metadata) }
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
    ): Result<Pair<Path, ChapterAccessor.Depth>> {
        return if (collectionId.isNotEmpty()) {
            path.resolveChild(collectionId)
                .then { it.resolveChild(chapterId) }
                .map { Pair(it, ChapterAccessor.Depth.Two) }
        } else if (chapterId.isNotEmpty()) {
            path.resolveChild(chapterId)
                .map { Pair(it, ChapterAccessor.Depth.One) }
        } else {
            Result.success(path)
                .map { Pair(it, ChapterAccessor.Depth.Zero) }
        }
    }

    fun getChapter(collectionId: String, chapterId: String): ChapterAccessor? {
        return getChapterPath(collectionId, chapterId)
            .getOrNull()
            ?.takeIf { it.first.isDirectory() }
            ?.let { ChapterAccessor(it.first, it.second) }
    }

    fun createChapter(collectionId: String, chapterId: String): Result<ChapterAccessor> {
        return getChapterPath(collectionId, chapterId)
            .onSuccess { it.first.createDirAll() }
            .map { ChapterAccessor(it.first, it.second) }
    }
}

private fun buildCollectionsFromMetadata(
    collectionsMetadata: Map<String, Map<String, ChapterMetadata>>
): Map<String, List<Chapter>> {
    return collectionsMetadata.mapValues { (_, chaptersMetadata) ->
        buildChaptersFromMetadata(chaptersMetadata)
    }
}

private fun buildChaptersFromMetadata(
    chaptersMetadata: Map<String, ChapterMetadata>
): List<Chapter> {
    return chaptersMetadata.map { (chapterId, metadata) ->
        Chapter(
            id = chapterId,
            name = metadata.name,
            title = metadata.title,
            updateTime = null,
        )
    }
}

private fun detectCollections(path: Path): Map<String, List<Chapter>> {
    return path
        .listDirectory()
        .getOrDefault(emptyList())
        .sortedWith(alphanumericOrder())
        .associate { it.name to detectChapters(it) }
        .filter { it.value.isNotEmpty() }
}

private fun detectChapters(path: Path): List<Chapter> {
    return path
        .listDirectory()
        .getOrDefault(emptyList())
        .sortedWith(alphanumericOrder())
        .map {
            val id = it.name
            Chapter(
                id = id,
                name = id.substringBefore(" "),
                title = id.substringAfter(" ").ifBlank { null },
                updateTime = it.getLastModifiedTime(),
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
