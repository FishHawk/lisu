package me.fishhawk.lisu.library

import me.fishhawk.lisu.model.MangaDto
import me.fishhawk.lisu.source.Board
import me.fishhawk.lisu.source.BoardModel
import java.nio.file.Path
import kotlin.io.path.*

class Library(private val path: Path) {
    val id = path.name

    fun search(page: Int, keywords: String): List<MangaDto> {
        val filters = Filter.fromKeywords(keywords)
        val pageSize = 100
        return path.listDirectory()
            .sortedByDescending { it.getLastModifiedTime() }
            .asSequence()
            .map { Manga(it) }
            .filter { manga ->
                filters.all {
                    it.isPass(manga.getSearchEntry())
                }
            }
            .drop(pageSize * page)
            .take(pageSize)
            .map { it.get() }
            .toList()
    }

    fun getBoard(boardId: String, page: Int): List<MangaDto> =
        when (boardId) {
            Board.Latest.id -> search(page, "")
            else -> null
        } ?: throw Error("board not found")

    fun createManga(id: String): Manga? {
        return getMangaPath(id)
            .takeIf { it.notExists() }
            ?.let { Manga(it.createDirectories()) }
    }

    fun deleteManga(id: String): Boolean {
        return getMangaPath(id)
            .takeIf { it.isDirectory() }
            ?.let {
                it.toFile().deleteRecursively()
                true
            } ?: false
    }

    fun getManga(id: String): Manga? {
        return getMangaPath(id)
            .takeIf { it.isDirectory() }
            ?.let { Manga(it) }
    }

    private fun getMangaPath(id: String): Path {
        return path.resolve(id)
    }

    companion object {
        const val lang = "local"
        val boardModels: Map<String, BoardModel> =
            mapOf(Board.Latest.id to emptyMap())
    }
}
