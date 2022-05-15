package me.fishhawk.lisu.library

import me.fishhawk.lisu.model.MangaDto
import me.fishhawk.lisu.util.*
import java.nio.file.Path

class LibraryManager(private val path: Path) {
    fun listLibraries() =
        path.listDirectory()
            .getOrDefault(emptyList())
            .map { Library(it) }

    private fun listMangas() =
        listLibraries()
            .flatMap { it.listMangas() }

    fun search(page: Int, keywords: String): List<MangaDto> {
        val filters = Filter.fromKeywords(keywords)
        return listMangas()
            .sortedByDescending { it.path.getLastModifiedTime() }
            .asSequence()
            .filter { manga -> filters.all { it.isPass(manga.getSearchEntry()) } }
            .page(page = page, pageSize = 100)
            .map { it.get() }
            .toList()
    }

    fun getRandomManga() =
        listMangas().random()

    private fun getLibraryPath(id: String) =
        path.resolveChild(id)

    fun getLibrary(id: String) =
        getLibraryPath(id)
            .getOrNull()
            ?.takeIf { it.isDirectory() }
            ?.let { Library(it) }

    fun createLibrary(id: String) =
        getLibraryPath(id)
            .then(Path::createDir)
            .map { Library(it) }
}