package me.fishhawk.lisu.download

import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import me.fishhawk.lisu.library.Chapter
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.library.Manga
import me.fishhawk.lisu.model.MangaDetailDto
import me.fishhawk.lisu.model.toMetadataDetail
import me.fishhawk.lisu.source.Source
import me.fishhawk.lisu.util.forEachIndexedParallel
import me.fishhawk.lisu.util.retry
import me.fishhawk.lisu.util.then
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("downloader")

class Worker(
    private val libraryManager: LibraryManager,
    private val source: Source,
    private val scope: CoroutineScope
) {
    val id
        get() = source.id

    @OptIn(ExperimentalCoroutinesApi::class)
    private val context = Dispatchers.IO.limitedParallelism(1)

    private var job: Job = scope.launch { }.apply { cancel() }

    private val waiting = mutableSetOf<String>()
    private val paused = mutableSetOf<String>()

    private fun isDownloading(mangaId: String) =
        mangaId == waiting.firstOrNull()

    private fun launchJob() {
        if (job.isActive) return
        job = scope.launch(context) {
            while (true) {
                val mangaId = waiting.firstOrNull() ?: break
                log.info("Downloading manga $id/$mangaId.")

                val manga = libraryManager.getLibrary(source.id)?.getManga(mangaId)
                if (manga == null) {
                    log.info("Manga $id/$mangaId not in library anymore.")
                    waiting.remove(mangaId)
                    continue
                }

                try {
                    val hasChapterError = withContext(Dispatchers.IO) {
                        download(source, manga)
                    }
                    if (hasChapterError) {
                        log.info("Download error for $id/$mangaId")
                        paused.add(mangaId)
                    }
                } catch (throwable: Throwable) {
                    paused.add(mangaId)
                    log.info("Unexpected download error: ${source.id} $mangaId $throwable")
                } finally {
                    waiting.remove(mangaId)
                }
            }
        }
    }

    private fun cancelJob() {
        if (job.isActive) job.cancel()
    }

    private fun restartJob() {
        cancelJob()
        launchJob()
    }

    suspend fun updateLibrary() = withContext(context) {
    }

    suspend fun start(mangaId: String) {
        if (paused.contains(mangaId)) {
            withContext(context) {
                paused.remove(mangaId)
                waiting.add(mangaId)
                launchJob()
            }
        }
    }

    suspend fun startAll() {
        withContext(context) {
            waiting.addAll(paused)
            paused.clear()
            launchJob()
        }
    }

    suspend fun pause(mangaId: String) {
        if (waiting.contains(mangaId)) {
            withContext(context) {
                val needRestart = isDownloading(mangaId)
                waiting.remove(mangaId)
                paused.add(mangaId)
                if (needRestart) restartJob()
            }
        }
    }

    suspend fun pauseAll() {
        withContext(context) {
            paused.addAll(waiting)
            waiting.clear()
            cancelJob()
        }
    }

    suspend fun add(mangaId: String) = withContext(context) {
        if (!paused.contains(mangaId) && !waiting.contains(mangaId)) {
            waiting.add(mangaId)
            if (mangaId != waiting.firstOrNull()) {
                // Prepare before download
                libraryManager.getLibrary(source.id)?.getManga(mangaId)?.let { manga ->
                    source.getManga(mangaId).onSuccess { detail ->
                        if (!manga.hasMetadata()) {
                            manga.setMetadata(detail.toMetadataDetail())
                        }
                        if (!manga.hasCover()) {
                            detail.cover
                                ?.let { source.getImage(it) }
                                ?.getOrNull()
                                ?.let { manga.setCover(it) }
                        }
                    }
                }
            }
            launchJob()
        }
    }

    suspend fun remove(mangaId: String) = withContext(context) {
        val needRestart = isDownloading(mangaId)
        waiting.remove(mangaId)
        paused.remove(mangaId)
        if (needRestart) restartJob()
    }
}

private suspend fun download(source: Source, manga: Manga): Boolean {
    var hasChapterError = false
    source.getManga(manga.id)
        .onSuccess { detail ->
            if (!manga.hasMetadata()) {
                manga.setMetadata(detail.toMetadataDetail())
            }
            if (!manga.hasCover()) {
                detail.cover
                    ?.let { source.getImage(it) }
                    ?.getOrNull()
                    ?.let { manga.setCover(it) }
            }
            hasChapterError = downloadChapters(source, manga, detail)
        }
        .onFailure { hasChapterError = true }
    return hasChapterError
}

private suspend fun downloadChapters(
    source: Source,
    manga: Manga,
    detailDto: MangaDetailDto
): Boolean {
    var hasChapterError = false

    val downloadInds =
        detailDto.collections?.flatMap { (collectionId, chapters) -> chapters.map { Pair(collectionId, it.id) } }
            ?: detailDto.chapters?.map { Pair("", it.id) }
            ?: detailDto.preview?.let { listOf(Pair("", "")) }

    downloadInds
        ?.mapNotNull { (collectionId, chapterId) ->
            val chapter = manga.getChapter(collectionId, chapterId)
                ?: manga.createChapter(collectionId, chapterId).getOrNull()
            if (chapter == null) hasChapterError = true
            chapter
        }
        ?.filter { !it.isFinished() }
        ?.map { chapter ->
            if (!downloadImages(source, manga, chapter)) {
                chapter.setFinished()
            } else {
                hasChapterError = true
            }
        }
    return hasChapterError
}

private suspend fun downloadImages(
    source: Source,
    manga: Manga,
    chapter: Chapter,
): Boolean {
    var hasImageError = false
    source.getContent(manga.id, chapter.id)
        .onSuccess { images ->
            val existingImages = chapter.getContent() ?: emptyList()
            images.filterIndexed { index, _ -> existingImages.contains(index.toString()) }
                .forEachIndexedParallel(5) { index, url ->
                    retry(3) { source.getImage(url) }
                        .then { chapter.setImage(index.toString(), it) }
                        .onFailure { hasImageError = true }
                }
        }
        .onFailure { hasImageError = true }
    return hasImageError
}
