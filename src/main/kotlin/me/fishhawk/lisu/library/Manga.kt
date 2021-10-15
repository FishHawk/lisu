package me.fishhawk.lisu.library

import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import me.fishhawk.lisu.model.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

// hack, see https://stackoverflow.com/questions/48448000/kotlins-extension-method-stream-tolist-is-missing
import kotlin.streams.toList

fun Path.listImageFiles(): List<Path> = Files.list(this).filter { it.isImageFile() }.toList()

fun Path.listDirectory(): List<Path> = Files.list(this).filter { it.isDirectory() }.toList()

private val imageExtensions = listOf("bmp", "jpeg", "jpg", "png", "gif", "webp")
fun Path.isImageFile() = isRegularFile() && extension.lowercase() in imageExtensions

@Serializable
data class SearchEntry(
    val title: String? = null,
    val authors: List<String>? = null,
    val tags: Map<String, List<String>>? = null
)


class Manga(private val path: Path) {
    private val metadataFile = path.resolve("metadata.json").toFile()

    fun getSearchEntry(): SearchEntry {
        return if (!metadataFile.exists()) SearchEntry(title = path.name)
        else try {
            Json { ignoreUnknownKeys = true }.decodeFromString(
                SearchEntry.serializer(),
                metadataFile.readText()
            ).let { if (it.title == null) it.copy(title = path.name) else it }
        } catch (e: SerializationException) {
            SearchEntry(title = path.name)
        }
    }

    fun get(): MangaDto {
        val metadata =
            if (!metadataFile.exists()) MetadataDto()
            else try {
                Json { ignoreUnknownKeys = true }.decodeFromString(
                    MetadataDto.serializer(),
                    metadataFile.readText()
                )
            } catch (e: SerializationException) {
                MetadataDto()
            }
        return MangaDto(
            providerId = path.parent.name,
            id = path.name,
            cover = null,
            updateTime = null,
            title = metadata.title,
            authors = metadata.authors,
            isFinished = metadata.isFinished
        )
    }

    fun getDetail(): MangaDetailDto {
        val metadata =
            if (!metadataFile.exists()) MetadataDetailDto()
            else try {
                Json { ignoreUnknownKeys = true }.decodeFromString(
                    MetadataDetailDto.serializer(),
                    metadataFile.readText()
                )
            } catch (e: SerializationException) {
                MetadataDetailDto()
            }
        val collections = detectCollections(path).ifEmpty { null }
        val chapters = if (collections != null) null else detectChapters(path).ifEmpty { null }
        val previews = if (chapters != null) null else detectPreviews(path).ifEmpty { null }
        return MangaDetailDto(
            providerId = path.parent.name,
            id = path.name,

            inLibrary = true,

            cover = null,
            updateTime = null,

            title = metadata.title,
            authors = metadata.authors,
            isFinished = metadata.isFinished,
            description = metadata.description,
            tags = metadata.tags,

            collections = collections,
            chapters = chapters,
            preview = previews
        )
    }


    fun getCover(): File? {
        val images = path.listImageFiles()
        val covers = images.filter { it.name == "cover" }.toList()
        val coverPath = covers.firstOrNull() ?: images.firstOrNull()
        return coverPath?.toFile()
    }

    fun updateCover(contentType: ContentType?, cover: ByteArray) {
        val ext =
            if (contentType == null || contentType.withoutParameters().match(ContentType.Image.Any)) "png"
            else contentType.fileExtensions().first()
        val coverFile = path.resolve("cover.$ext").toFile()
        coverFile.writeBytes(cover)
    }

    fun updateMetadata(metadata: MetadataDetailDto) {
        val json = Json.encodeToString(MetadataDetailDto.serializer(), metadata)
        metadataFile.writeText(json)
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
    return path.listImageFiles().map { it.name }.sortedWith(naturalOrder()).take(20)
}
