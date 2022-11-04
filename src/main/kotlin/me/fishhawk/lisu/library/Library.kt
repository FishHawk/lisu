package me.fishhawk.lisu.library

import me.fishhawk.lisu.library.model.MangaPage
import me.fishhawk.lisu.util.*
import java.nio.file.Path

class Library(private val path: Path) {
    val id = path.name

    fun listMangas(): List<MangaAccessor> {
        return path.listDirectory()
            .getOrDefault(emptyList())
            .sortedByDescending { it.getLastModifiedTime() }
            .map { MangaAccessor(it) }
    }

    fun search(key: String, keywords: String): MangaPage {
        val filters = Filter.fromKeywords(keywords)
        val list = listMangas()
            .asSequence()
            .let { seq ->
                if (key.isEmpty()) seq
                else seq.dropWhile { it.id != key }.drop(1)
            }
            .take(100)
            .filter { manga -> filters.all { it.isPass(manga.getSearchEntry()) } }
            .map { it.get() }
            .toList()
        return MangaPage(
            list = list,
            nextKey = list.lastOrNull()?.id,
        )
    }

    private fun getMangaPath(id: String): Result<Path> {
        return if (id.isFilename()) {
            Result.success(path.resolve(id))
        } else {
            Result.failure(LibraryException.MangaIllegalId(id))
        }
    }

    fun getManga(id: String): Result<MangaAccessor> {
        return getMangaPath(id)
            .andThen { path ->
                if (path.isDirectory()) {
                    Result.success(MangaAccessor(path))
                } else {
                    Result.failure(LibraryException.MangaNotFound(id))
                }
            }
    }

    fun createManga(id: String): Result<MangaAccessor> {
        return getMangaPath(id)
            .andThen { path ->
                if (path.isDirectory()) {
                    Result.success(MangaAccessor(path))
                } else {
                    path.createDirAll().map { MangaAccessor(path) }
                }
            }
    }

    fun deleteManga(id: String): Result<Unit> {
        return getMangaPath(id)
            .andThen { path ->
                if (path.isDirectory()) {
                    path.deleteDirAll()
                } else {
                    Result.failure(LibraryException.MangaNotFound(id))
                }
            }
    }
}
