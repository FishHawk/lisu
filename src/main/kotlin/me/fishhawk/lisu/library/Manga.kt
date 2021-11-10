package me.fishhawk.lisu.library

import io.ktor.http.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.fishhawk.lisu.model.*
import java.nio.file.Path
import kotlin.io.path.*

class Manga(private val path: Path) {
    val providerId = path.parent.name
    val id = path.name

    fun getSearchEntry(): SearchEntry {
        if (!metadataFile.exists()) return SearchEntry(title = id)
        return try {
            Json { ignoreUnknownKeys = true }.decodeFromString(
                SearchEntry.serializer(),
                metadataFile.readText()
            ).let { if (it.title == null) it.copy(title = id) else it }
        } catch (e: SerializationException) {
            SearchEntry(title = id)
        }
    }

    fun get(): MangaDto {
        val metadata = getMetadata()
        return MangaDto(
            providerId = providerId,
            id = id,
            cover = null,
            updateTime = null,
            title = metadata?.title,
            authors = metadata?.authors,
            isFinished = metadata?.isFinished
        )
    }

    fun getDetail(): MangaDetailDto {
        val metadata = getMetadataDetail()
        val collections = detectCollections(path).ifEmpty { null }
        val chapters = if (collections != null) null else detectChapters(path).ifEmpty { null }
        val previews = if (chapters != null) null else detectPreviews(path).ifEmpty { null }
        return MangaDetailDto(
            providerId = providerId,
            id = id,

            inLibrary = true,

            cover = null,
            updateTime = null,

            title = metadata?.title,
            authors = metadata?.authors,
            isFinished = metadata?.isFinished,
            description = metadata?.description,
            tags = metadata?.tags,

            collections = collections,
            chapters = chapters,
            preview = previews
        )
    }

    private val metadataFile = path.resolve("metadata.json").toFile()

    fun hasMetadata(): Boolean {
        return metadataFile.exists()
    }

    private fun getMetadata(): MetadataDto? {
        if (!metadataFile.exists()) return null
        return try {
            Json { ignoreUnknownKeys = true }.decodeFromString(
                MetadataDto.serializer(),
                metadataFile.readText()
            )
        } catch (e: SerializationException) {
            null
        }
    }

    private fun getMetadataDetail(): MetadataDetailDto? {
        if (!metadataFile.exists()) return null
        return try {
            Json { ignoreUnknownKeys = true }.decodeFromString(
                MetadataDetailDto.serializer(),
                metadataFile.readText()
            )
        } catch (e: SerializationException) {
            null
        }
    }

    fun updateMetadata(metadata: MetadataDetailDto) {
        val json = Json.encodeToString(MetadataDetailDto.serializer(), metadata)
        metadataFile.writeText(json)
    }

    fun hasCover(): Boolean {
        val images = path.listImageFiles()
        val covers = images.filter { it.name == "cover" }.toList()
        return covers.isNotEmpty()
    }

    fun getCover(): Image? {
        val images = path.listImageFiles()
        val covers = images.filter { it.name == "cover" }.toList()
        val coverPath = covers.firstOrNull() ?: images.firstOrNull()
        return coverPath?.toImage()
    }

    fun updateCover(cover: Image) {
        val ext = cover.mime.let {
            if (it == null || it.withoutParameters().match(ContentType.Image.Any)) "png"
            else it.fileExtensions().first()
        }
        val coverFile = path.resolve("cover.$ext").toFile()
        cover.stream.copyTo(coverFile.outputStream())
    }

    fun getChapter(collectionId: String, chapterId: String): Chapter? {
        val chapterPath = path.resolve(collectionId.ifBlank { "" }).resolve(chapterId.ifBlank { "" })
        return if (chapterPath.exists()) Chapter(chapterPath) else null
    }
}

private fun detectCollections(path: Path): Map<String, List<ChapterDto>> {
    return path
        .listDirectory()
        .sortedWith(alphanumericOrder())
        .associate { it.name to detectChapters(it) }
        .filter { it.value.isNotEmpty() }
}

private fun detectChapters(path: Path): List<ChapterDto> {
    return path
        .listDirectory()
        .sortedWith(alphanumericOrder())
        .map {
            ChapterDto(
                id = it.name,
                name = it.name,
                title = it.name,
                updateTime = it.getLastModifiedTime().toMillis()
            )
        }
}

private fun detectPreviews(path: Path): List<String> {
    return path
        .listImageFiles()
        .map { it.name }
        .sortedWith(naturalOrder())
        .take(20)
}
