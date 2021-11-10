package me.fishhawk.lisu.library

import me.fishhawk.lisu.model.MangaDto
import java.nio.file.Path
import kotlin.io.path.*

class Library(private val path: Path) {
    fun search(page: Int, keywords: String): List<MangaDto> {
        val filters = keywords.split(';').mapNotNull { Filter.fromToken(it.trim()) }
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

    fun hasManga(id: String) = getManga(id) != null

    fun createManga(id: String): Manga? {
        val mangaPath = getMangaPath(id)
        return if (mangaPath.isDirectory()) null
        else Manga(mangaPath.createDirectories())
    }

    fun deleteManga(id: String): Boolean {
        val mangaPath = getMangaPath(id)
        return if (mangaPath.isDirectory()) false
        else {
            mangaPath.toFile().deleteRecursively()
            true
        }
    }

    fun getManga(id: String): Manga? {
        val mangaPath = getMangaPath(id)
        return if (mangaPath.isDirectory()) Manga(mangaPath)
        else null
    }

    private fun getMangaPath(id: String): Path {
        return path.resolve(id)
    }
}

class LibraryManager(private val path: Path) {
    fun search(page: Int, keywords: String): List<MangaDto> {
        val filters = keywords.split(';').mapNotNull { Filter.fromToken(it.trim()) }
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

    fun getLibrary(id: String): Library? {
        return path.resolve(id).let {
            if (it.notExists()) it.createDirectory()
            if (it.isDirectory()) Library(it) else null
        }
    }
}