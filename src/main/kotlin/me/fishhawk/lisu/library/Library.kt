package me.fishhawk.lisu.library

import me.fishhawk.lisu.model.MangaDto
import me.fishhawk.lisu.source.Board
import me.fishhawk.lisu.source.BoardModel
import me.fishhawk.lisu.util.*
import java.nio.file.Path

fun <T> Sequence<T>.page(page: Int, pageSize: Int): Sequence<T> =
    drop(pageSize * page).take(pageSize)

class Library(private val path: Path) {
    val id = path.name

    fun listMangas(): List<Manga> {
        return path.listDirectory()
            .getOrDefault(emptyList())
            .sortedByDescending { it.getLastModifiedTime() }
            .map { Manga(it) }
    }

    fun search(page: Int, keywords: String): List<MangaDto> {
        val filters = Filter.fromKeywords(keywords)
        return listMangas()
            .asSequence()
            .filter { manga -> filters.all { it.isPass(manga.getSearchEntry()) } }
            .page(page = page, pageSize = 100)
            .map { it.get() }
            .toList()
    }

    fun getBoard(boardId: String, page: Int): List<MangaDto>? {
        return when (boardId) {
            Board.Latest.id -> search(page, "")
            else -> null
        }
    }

    private fun getMangaPath(id: String): Result<Path> {
        return path.resolveChild(id)
    }

    fun createManga(id: String): Result<Manga> {
        return getMangaPath(id)
            .then(Path::createDirAll)
            .map { Manga(it) }
    }

    fun deleteManga(id: String): Result<Unit> {
        return getMangaPath(id)
            .then(Path::deleteDirAll)
    }

    fun getManga(id: String): Manga? {
        return getMangaPath(id)
            .getOrNull()
            ?.takeIf { it.isDirectory() }
            ?.let { Manga(it) }
    }

    companion object {
        const val lang = "local"
        val boardModels: Map<String, BoardModel> =
            mapOf(Board.Latest.id to emptyMap())
    }
}
