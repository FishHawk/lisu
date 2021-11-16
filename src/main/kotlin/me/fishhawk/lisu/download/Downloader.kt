package me.fishhawk.lisu.download

import dev.inmo.krontab.builder.buildSchedule
import dev.inmo.krontab.doInfinity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import me.fishhawk.lisu.library.Chapter
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.library.Manga
import me.fishhawk.lisu.model.MangaDetailDto
import me.fishhawk.lisu.source.Source
import me.fishhawk.lisu.source.SourceManager
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("downloader")

@OptIn(DelicateCoroutinesApi::class)
class Downloader(
    private val libraryManager: LibraryManager,
    sourceManager: SourceManager
) {
    private val context = newSingleThreadContext("downloader")

    private val workers =
        sourceManager.listSources()
            .map { Worker(libraryManager, it) }
            .associateBy { it.id }

    private val updater = buildSchedule { hours { at(4) } }

    init {
        GlobalScope.launch {
            updateAll() // Update all mangas at startup
            updater.doInfinity { updateAll() }
        }
    }

    private suspend fun updateAll() = withContext(context) {
        libraryManager.listMangaNeedUpdate().map { add(it.providerId, it.id) }
    }

    suspend fun pause() = withContext(context) {
        workers.values.map { it.pause() }
    }

    suspend fun start() = withContext(context) {
        workers.values.map { it.start() }
    }

    suspend fun add(providerId: String, mangaId: String) = withContext(context) {
        workers[providerId]?.add(mangaId)
    }

    suspend fun remove(providerId: String, mangaId: String) = withContext(context) {
        workers[providerId]?.remove(mangaId)
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

    fun remove(mangaId: String) {
        waitingMangas.remove(mangaId)
        errorMangas.remove(mangaId)
    }

    suspend fun pause() {
        currentJob?.cancelAndJoin()
    }

    suspend fun start() = coroutineScope {
        currentJob = launch {
            while (true) {
                val mangaId = waitingMangas.firstOrNull() ?: break
                log.info("Downloading manga: ${source.id} $mangaId")
                val manga = libraryManager.getLibrary(source.id)?.getManga(mangaId)
                if (manga == null) {
                    waitingMangas.remove(mangaId)
                    break
                }
                try {
                    val hasChapterError = download(source, manga)
                    if (hasChapterError) {
                        errorMangas[mangaId] = Error("ChapterError")
                        log.info("Downloading error: ${source.id} $mangaId ChapterError")
                    }
                } catch (throwable: Throwable) {
                    errorMangas[mangaId] = throwable
                    log.info("Downloading error: ${source.id} $mangaId $throwable")
                } finally {
                    waitingMangas.remove(mangaId)
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