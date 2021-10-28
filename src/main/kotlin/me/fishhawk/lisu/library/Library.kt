package me.fishhawk.lisu.library

import me.fishhawk.lisu.model.MangaDto
import java.nio.file.Path
import kotlin.io.path.*

class Library(private val path: Path) {
    fun search(page: Int, keywords: String): List<MangaDto> {
        val filters = keywords.split(';').mapNotNull { Filter.fromToken(it.trim()) }
        val pageSize = 100
        return path.listDirectory()
            .flatMap { it.listDirectory() }
            .sortedByDescending { it.getLastModifiedTime() }
            .asSequence()
            .map { Manga(it) }
            .filter { manga -> filters.all { it.isPass(manga.getSearchEntry()) } }
            .drop(pageSize * page)
            .take(pageSize)
            .map { it.get() }
            .toList()
    }

    fun getRandomManga(): Manga {
        return path.listDirectory()
            .flatMap { it.listDirectory() }
            .random()
            .let { Manga(it) }
    }

    fun hasManga(providerId: String, mangaId: String) = getManga(providerId, mangaId) != null

    fun createManga(providerId: String, mangaId: String): Manga? {
        val mangaPath = getMangaPath(providerId, mangaId)
        return if (mangaPath.exists()) null
        else Manga(mangaPath.createDirectories())
    }

    fun deleteManga(providerId: String, mangaId: String): Boolean {
        val mangaPath = getMangaPath(providerId, mangaId)
        return if (mangaPath.notExists()) false
        else {
            mangaPath.toFile().deleteRecursively()
            true
        }
    }

    fun getManga(providerId: String, mangaId: String): Manga? {
        val mangaPath = getMangaPath(providerId, mangaId)
        return if (mangaPath.exists()) Manga(mangaPath) else null
    }

    private fun getMangaPath(providerId: String, mangaId: String): Path {
        return path.resolve(providerId).resolve(mangaId)
    }

}

class Filter(
    private val key: String?,
    private val value: String,
    private val isExclusionMode: Boolean,
    private val isExactMode: Boolean
) {
    private fun isKeyMatch(key: String) = this.key == null || this.key == key

    private fun isIvsMatched(ivs: List<String>) =
        if (this.isExactMode) !ivs.contains(this.value)
        else ivs.any { it.contains(this.value) }

    fun isPass(entry: SearchEntry): Boolean {
        val iv1 = if (key == null) (entry.authors.orEmpty() + entry.title).filterNotNull() else emptyList()
        val iv2 = entry.tags?.flatMap { if (isKeyMatch(it.key)) it.value else emptyList() }.orEmpty()
        return isIvsMatched(iv1 + iv2) != isExclusionMode
    }

    companion object {
        fun fromToken(toke: String): Filter? {
            var token = toke

            if (token.isBlank()) return null

            val isExclusionMode = token.startsWith('-')
            if (isExclusionMode) token = token.substring(1)

            val isExactMode = token.endsWith('$');
            if (isExactMode) token = token.substring(0, token.length - 1)

            val splitPosition = token.indexOf(':')
            val key = if (splitPosition != -1) token.substring(0, splitPosition) else null
            val value = if (splitPosition != -1) token.substring(splitPosition + 1, token.length) else token
            return Filter(key, value, isExclusionMode, isExactMode)
        }
    }
}