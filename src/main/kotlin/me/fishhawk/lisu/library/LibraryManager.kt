package me.fishhawk.lisu.library

import me.fishhawk.lisu.library.model.Manga
import me.fishhawk.lisu.util.*
import java.nio.file.Path

class LibraryManager(private val path: Path) {
    fun listLibraries() =
        path.listDirectory()
            .getOrDefault(emptyList())
            .map { Library(it) }

    private fun listMangas() =
        listLibraries()
            .flatMap { library ->
                library.listMangas().map {
                    library.id to it
                }
            }

    fun search(page: Int, keywords: String): List<Pair<String, Manga>> {
        val filters = Filter.fromKeywords(keywords)
        return listMangas()
            .sortedByDescending { it.second.path.getLastModifiedTime() }
            .asSequence()
            .filter { (_, manga) -> filters.all { it.isPass(manga.getSearchEntry()) } }
            .page(page = page, pageSize = 100)
            .map { (libraryId, manga) -> libraryId to manga.get() }
            .toList()
    }

    fun getRandomManga() =
        listMangas()
            .random()
            .let { (libraryId, manga) -> libraryId to manga.get() }

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