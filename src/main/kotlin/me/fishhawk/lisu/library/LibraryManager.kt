package me.fishhawk.lisu.library

import me.fishhawk.lisu.model.MangaDto
import java.nio.file.Path
import kotlin.io.path.*

class LibraryManager(private val path: Path) {
    fun listMangaNeedUpdate(): List<Manga> {
        return path.listDirectory()
            .flatMap { it.listDirectory() }
            .map { Manga(it) }
            .filter { it.get().isFinished != true }
    }

    fun search(page: Int, keywords: String): List<MangaDto> {
        val filters = Filter.fromKeywords(keywords)
        val pageSize = 100
        return path.listDirectory()
            .flatMap { it.listDirectory() }
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

    fun getRandomManga(): Manga {
        return path.listDirectory()
            .flatMap { it.listDirectory() }
            .random()
            .let { Manga(it) }
    }

    fun listLibraries(): List<Library> {
        return path.listDirectory()
            .map { Library(it) }
    }

    fun getLibrary(id: String): Library? {
        return getLibraryPath(id)
            .takeIf { it.isDirectory() }
            ?.let { Library(it) }
    }

    fun getOrCreateLibrary(id: String): Library? =
        getLibrary(id) ?: createLibrary(id)

    private fun createLibrary(id: String): Library? {
        return getLibraryPath(id)
            .takeIf { it.notExists() }
            ?.let { Library(it.createDirectory()) }
    }

    private fun getLibraryPath(id: String): Path {
        return path.resolve(id)
    }
}