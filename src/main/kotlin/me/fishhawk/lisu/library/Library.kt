package me.fishhawk.lisu.library

import me.fishhawk.lisu.library.model.Manga
import me.fishhawk.lisu.util.*
import java.nio.file.Path

fun <T> Sequence<T>.page(page: Int, pageSize: Int): Sequence<T> =
    drop(pageSize * page).take(pageSize)

class Library(private val path: Path) {
    val id = path.name

    fun listMangas(): List<MangaAccessor> {
        return path.listDirectory()
            .getOrDefault(emptyList())
            .sortedByDescending { it.getLastModifiedTime() }
            .map { MangaAccessor(it) }
    }

    fun search(page: Int, keywords: String): List<Manga> {
        val filters = Filter.fromKeywords(keywords)
        return listMangas()
            .asSequence()
            .filter { manga -> filters.all { it.isPass(manga.getSearchEntry()) } }
            .page(page = page, pageSize = 100)
            .map { it.get() }
            .toList()
    }

    private fun getMangaPath(id: String): Result<Path> {
        return path.resolveChild(id)
    }

    fun createManga(id: String): Result<MangaAccessor> {
        return getMangaPath(id)
            .then(Path::createDirAll)
            .map { MangaAccessor(it) }
    }

    fun deleteManga(id: String): Result<Unit> {
        return getMangaPath(id)
            .then(Path::deleteDirAll)
    }

    fun getManga(id: String): MangaAccessor? {
        return getMangaPath(id)
            .getOrNull()
            ?.takeIf { it.isDirectory() }
            ?.let { MangaAccessor(it) }
    }
}
