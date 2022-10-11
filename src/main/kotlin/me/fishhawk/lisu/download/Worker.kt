package me.fishhawk.lisu.download

import kotlinx.coroutines.*
import me.fishhawk.lisu.download.model.ChapterDownloadTask
import me.fishhawk.lisu.download.model.MangaDownloadTask
import me.fishhawk.lisu.library.ChapterAccessor
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.library.MangaAccessor
import me.fishhawk.lisu.library.model.MangaChapterMetadata
import me.fishhawk.lisu.library.model.MangaMetadata
import me.fishhawk.lisu.source.Source
import me.fishhawk.lisu.util.andThen
import me.fishhawk.lisu.util.forEachParallel
import me.fishhawk.lisu.util.retry
import me.fishhawk.lisu.util.safeRunCatching
import java.util.concurrent.atomic.AtomicInteger

class Worker(
    private val libraryManager: LibraryManager,
    private val source: Source,
    private val scope: CoroutineScope,
    private val tasks: MutableList<MangaDownloadTask>,
    private val notifyTasksChanged: () -> Unit,
) {
    val id
        get() = source.id

    private var downloadJob: Job = Job().apply { cancel() }
    private var mangaJob: Job? = null
    private var chapterJob: Job? = null

    private var currentMangaTask: MangaDownloadTask? = null
    private var currentChapterTask: ChapterDownloadTask? = null

    fun start() {
        if (!downloadJob.isActive) {
            downloadJob = scope.launch { download() }
        }
    }

    suspend fun cancel() {
        if (downloadJob.isActive) {
            downloadJob.cancelAndJoin()
        }
    }

    suspend fun cancelMangaTask(mangaId: String) {
        if (
            mangaJob?.isActive == true &&
            id == currentMangaTask?.providerId &&
            mangaId == currentMangaTask?.mangaId
        ) {
            mangaJob?.cancelAndJoin()
        }
    }

    suspend fun cancelChapterTask(
        mangaId: String,
        collectionId: String,
        chapterId: String,
    ) {
        if (
            mangaJob?.isActive == true &&
            id == currentMangaTask?.providerId &&
            mangaId == currentMangaTask?.mangaId &&
            chapterJob?.isActive == true &&
            collectionId == currentChapterTask?.collectionId &&
            chapterId == currentChapterTask?.chapterId
        ) {
            chapterJob?.cancelAndJoin()
        }
    }

    suspend fun createMangaDownloadTask(mangaId: String): Result<MangaDownloadTask?> {
        return safeRunCatching {
            val remoteMangaDetail = source.getManga(mangaId).getOrThrow()
            val localMangaAccessor = libraryManager.createLibrary(id)
                .andThen { it.createManga(mangaId) }
                .getOrThrow()

            if (!localMangaAccessor.hasMetadata()) {
                localMangaAccessor.setMetadata(MangaMetadata.fromMangaDetail(remoteMangaDetail))
            }
            if (!localMangaAccessor.hasCover()) {
                remoteMangaDetail.cover
                    ?.let { source.getImage(it) }
                    ?.getOrNull()
                    ?.let { localMangaAccessor.setCover(it) }
            }
            // TODO: Incremental updates to avoid remote corruption
            localMangaAccessor.setChapterMetadata(MangaChapterMetadata.fromMangaDetail(remoteMangaDetail))

            val chapterTasks = mutableListOf<ChapterDownloadTask>()
            remoteMangaDetail.collections.forEach { (collectionId, chapters) ->
                chapters
                    .filter { !it.isLocked }
                    .filter { chapter ->
                        localMangaAccessor.getChapter(collectionId, chapter.id).fold(
                            onSuccess = { !it.isFinished() },
                            onFailure = { true },
                        )
                    }
                    .forEach {
                        chapterTasks.add(
                            ChapterDownloadTask(
                                collectionId = collectionId,
                                chapterId = it.id,
                                name = it.name,
                                title = it.title,
                            )
                        )
                    }
            }

            chapterTasks
                .takeIf { it.isNotEmpty() }
                ?.let {
                    MangaDownloadTask(
                        providerId = id,
                        mangaId = mangaId,
                        cover = remoteMangaDetail.cover,
                        title = remoteMangaDetail.title,
                        chapterTasks = it,
                    )
                }
        }
    }

    private suspend fun download() {
        while (true) {
            val mangaTask = tasks
                .firstOrNull { task ->
                    task.providerId == id && task.chapterTasks.any { it.state is ChapterDownloadTask.State.Waiting }
                }
                ?: break

            log.info("Downloading manga ${mangaTask.providerId}/${mangaTask.mangaId}.")

            scope.launch {
                libraryManager
                    .getLibrary(mangaTask.providerId)
                    .andThen { it.getManga(mangaTask.mangaId) }
                    .onSuccess { mangaAccessor ->
                        try {
                            currentMangaTask = mangaTask
                            downloadManga(mangaAccessor, mangaTask)
                        } finally {
                            currentMangaTask = null
                        }
                        if (mangaTask.chapterTasks.isEmpty()) {
                            val removed = tasks.removeIf { mangaTask == it }
                            if (removed) notifyTasksChanged()
                        }
                    }
                    .onFailure {
                        log.info("Manga ${mangaTask.providerId}/${mangaTask.mangaId} not in library anymore.")
                        val removed = tasks.removeIf { mangaTask == it }
                        if (removed) notifyTasksChanged()
                    }
            }.let {
                mangaJob = it
                it.join()
            }
        }
    }

    private suspend fun downloadManga(
        mangaAccessor: MangaAccessor,
        mangaTask: MangaDownloadTask,
    ) {
        while (true) {
            val chapterTask = mangaTask.chapterTasks
                .firstOrNull { it.state == ChapterDownloadTask.State.Waiting }
                ?: break

            scope.launch {
                mangaAccessor
                    .createChapter(chapterTask.collectionId, chapterTask.chapterId)
                    .onFailure {
                        chapterTask.state = ChapterDownloadTask.State.Failed(
                            downloadedPageNumber = null,
                            totalPageNumber = null,
                            errorMessage = it.message ?: "can not create chapter folder",
                        )
                    }
                    .onSuccess { chapterAccessor ->
                        try {
                            currentChapterTask = chapterTask
                            downloadChapter(
                                mangaId = mangaAccessor.id,
                                chapterAccessor = chapterAccessor,
                                chapterTask = chapterTask
                            )
                            (chapterTask.state as? ChapterDownloadTask.State.Downloading)?.let { state ->
                                if (
                                    state.downloadedPageNumber != null &&
                                    state.totalPageNumber != null &&
                                    state.downloadedPageNumber == state.totalPageNumber
                                ) {
                                    chapterAccessor.setFinished()
                                    mangaTask.chapterTasks.removeIf { chapterTask === it }
                                    notifyTasksChanged()
                                } else {
                                    chapterTask.state = ChapterDownloadTask.State.Failed(
                                        downloadedPageNumber = state.downloadedPageNumber,
                                        totalPageNumber = state.totalPageNumber,
                                        errorMessage = "Error when downloading images.",
                                    )
                                    notifyTasksChanged()
                                }
                            }
                        } finally {
                            currentChapterTask = null
                        }
                    }
            }.let {
                chapterJob = it
                it.join()
            }
        }
    }

    private suspend fun downloadChapter(
        mangaId: String,
        chapterAccessor: ChapterAccessor,
        chapterTask: ChapterDownloadTask,
    ) {
        chapterTask.state = ChapterDownloadTask.State.Downloading(
            downloadedPageNumber = null,
            totalPageNumber = null,
        )
        notifyTasksChanged()
        source.getContent(mangaId, chapterAccessor.id)
            .onSuccess { images ->
                val existingImages = chapterAccessor.getContent() ?: emptyList()
                val unDownloadedImages = images.withIndex().filter { !existingImages.contains(it.index.toString()) }
                val downloadedPageNumber = AtomicInteger(images.size - unDownloadedImages.size)

                chapterTask.state = ChapterDownloadTask.State.Downloading(
                    downloadedPageNumber = downloadedPageNumber.get(),
                    totalPageNumber = images.size,
                )
                notifyTasksChanged()
                unDownloadedImages
                    .forEachParallel(3) { indexedUrl ->
                        retry(2) { source.getImage(indexedUrl.value) }
                            .andThen { chapterAccessor.setImage(indexedUrl.index.toString(), it) }
                            .onSuccess {
                                chapterTask.state = ChapterDownloadTask.State.Downloading(
                                    downloadedPageNumber = downloadedPageNumber.incrementAndGet(),
                                    totalPageNumber = images.size,
                                )
                                notifyTasksChanged()
                            }
                    }

            }
            .onFailure {
                chapterTask.state = ChapterDownloadTask.State.Failed(
                    downloadedPageNumber = null,
                    totalPageNumber = null,
                    errorMessage = "Error when get chapter content.",
                )
            }
    }
}