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

    private fun getLibraryPath(id: String): Result<Path> {
        return if (id.isFilename()) {
            Result.success(path.resolve(id))
        } else {
            Result.failure(LibraryException.LibraryIllegalId(id))
        }
    }

    fun getLibrary(id: String): Result<Library> {
        return getLibraryPath(id)
            .andThen { path ->
                if (path.isDirectory()) {
                    Result.success(Library(path))
                } else {
                    Result.failure(LibraryException.LibraryNotFound(id))
                }
            }
    }

    fun createLibrary(id: String): Result<Library> {
        return getLibraryPath(id)
            .andThen { path ->
                if (path.isDirectory()) {
                    Result.success(Library(path))
                } else {
                    path.createDirAll().fold(
                        onSuccess = { Result.success(Library(path)) },
                        onFailure = { Result.failure(LibraryException.LibraryCanNotCreate(id, it)) },
                    )
                }
            }
    }
}