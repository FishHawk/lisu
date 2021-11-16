package me.fishhawk.lisu.download

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import me.fishhawk.lisu.library.Chapter
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.library.Manga
import me.fishhawk.lisu.model.MangaDetailDto
import me.fishhawk.lisu.source.Source
import me.fishhawk.lisu.source.SourceManager

class Downloader(
    libraryManager: LibraryManager,
    sourceManager: SourceManager
) {
    private val context = newSingleThreadContext("downloader")

    private val workers =
        sourceManager.listSources()
            .map { Worker(libraryManager, it) }
            .associateBy { it.id }

    suspend fun pause() = withContext(context) {
        workers.values.map { it.pause() }
    }

    suspend fun start() = withContext(context) {
        workers.values.map { it.start() }
    }

    suspend fun add(providerId: String, mangaId: String) = withContext(context) {
        workers[providerId]?.add(mangaId)
    }
}

private class Worker(
    private val libraryManager: LibraryManager,
    private val source: Source
) {
    val id = source.id

    private var currentJob: Job? = null
    private val waitingMangas = mutableSetOf<String>()
    private val errorMangas = mutableMapOf<String, Throwable>()

    suspend fun add(mangaId: String) {
        waitingMangas.add(mangaId)
        errorMangas.remove(mangaId)
        if (currentJob == null) start()
    }

    suspend fun pause() {
        currentJob?.cancelAndJoin()
    }

    suspend fun start() = coroutineScope {
        currentJob = launch {
            while (true) {
                val mangaId = waitingMangas.firstOrNull() ?: break
                val manga = libraryManager.getLibrary(source.id)?.getManga(mangaId)
                if (manga == null) {
                    waitingMangas.remove(mangaId)
                    break
                }
                try {
                    val hasChapterError = download(source, manga)
                    if (hasChapterError) errorMangas[mangaId] = Error("chapter error")
                    waitingMangas.remove(mangaId)
                } catch (throwable: Throwable) {
                    errorMangas[mangaId] = throwable
                }
            }
        }
    }
}

private suspend fun download(source: Source, manga: Manga): Boolean {
    val detailDto = source.getManga(manga.id)
    manga.takeIf { !it.hasMetadata() }?.updateMetadata(detailDto.metadataDetail)
    manga.takeIf { !it.hasCover() }?.let {
        detailDto.cover?.let { cover ->
            it.updateCover(source.getImage(cover))
        }
    }
    return downloadChapters(source, manga, detailDto)
}

private suspend fun downloadChapters(
    source: Source,
    manga: Manga,
    detailDto: MangaDetailDto
): Boolean {
    val chapterIndList =
        detailDto.collections?.flatMap { (collectionId, chapters) -> chapters.map { Pair(collectionId, it.id) } }
            ?: detailDto.chapters?.map { Pair("", it.id) }
            ?: detailDto.preview?.let { listOf(Pair("", "")) }

    var hasChapterError = false
    chapterIndList
        ?.asFlow()
        ?.map { (collectionId, chapterId) -> manga.getOrCreateChapter(collectionId, chapterId)!! }
        ?.filter { it.unfinished }
        ?.collect { chapter ->
            try {
                if (!downloadImages(source, manga, chapter))
                    chapter.unfinished = false
                else hasChapterError = true
            } catch (e: Throwable) {
                hasChapterError = true
            }
        }
    return hasChapterError
}

private suspend fun downloadImages(source: Source, manga: Manga, chapter: Chapter): Boolean {
    var hasImageError = false
    val existingImages = chapter.getContent()
    source.getContent(manga.id, chapter.collectionId, chapter.chapterId)
        .filterIndexed { index, _ -> existingImages.none { it == index.toString() } }
        .forEachIndexedParallel(5) { index, url ->
            try {
                val image = retry(3) { source.getImage(url) }
                chapter.setImage(index.toString(), image)
            } catch (e: Throwable) {
                hasImageError = true
            }
        }
    return hasImageError
}