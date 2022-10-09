package me.fishhawk.lisu.download

import dev.inmo.krontab.builder.buildSchedule
import dev.inmo.krontab.doInfinityTz
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import me.fishhawk.lisu.download.model.ChapterDownloadTask
import me.fishhawk.lisu.download.model.MangaDownloadTask
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.source.SourceManager
import java.util.*

class Downloader(
    private val libraryManager: LibraryManager,
    sourceManager: SourceManager
) {
    @OptIn(DelicateCoroutinesApi::class)
    private val context = newSingleThreadContext("Downloader")
    private val scope = CoroutineScope(context)

    private val tasks = mutableListOf<MangaDownloadTask>()

    private val tasksChangedChannel = Channel<Unit>(capacity = 1)
    private val notifyTasksChanged: () -> Unit = { tasksChangedChannel.trySend(Unit) }

    private val tasksFlow = tasksChangedChannel
        .receiveAsFlow()
        .map {
            tasks.map { mangaTask ->
                val chapterTasks = mangaTask.chapterTasks.map { it.copy() }.toMutableList()
                mangaTask.copy(chapterTasks = chapterTasks)
            }
        }
        .shareIn(scope, SharingStarted.WhileSubscribed())

    private val workers =
        sourceManager.listSources()
            .map {
                Worker(
                    libraryManager = libraryManager,
                    source = it,
                    scope = scope,
                    tasks = tasks,
                    notifyTasksChanged = notifyTasksChanged,
                )
            }
            .associateBy { it.id }

    private val updater = buildSchedule(
        offset = TimeZone.getDefault().rawOffset / 1000 / 60
    ) {
        hours { at(4) }
        minutes { at(0) }
        seconds { at(0) }
    }

    init {
        scope.launch {
            updateAll()
            updater.doInfinityTz {
                updateAll()
            }
        }
    }

    private suspend fun updateAll() {
        workers.forEach { (providerId, worker) ->
            libraryManager
                .getLibrary(providerId).getOrNull()
                ?.listMangas()
                ?.filter { it.get().isFinished != true }
                ?.map { it.id }
                ?.forEach { mangaId -> add(providerId, mangaId) }
        }
    }

    fun observeTasks(): Flow<List<MangaDownloadTask>> {
        tasksChangedChannel.trySend(Unit)
        return tasksFlow
    }

    suspend fun startAllTasks() = withContext(context) {
        tasks.forEach { mangaDownload ->
            mangaDownload
                .chapterTasks
                .filter { it.state is ChapterDownloadTask.State.Failed }
                .forEach { it.state = ChapterDownloadTask.State.Waiting }
        }
        workers.values.forEach { it.start() }
    }

    suspend fun startMangaTask(
        providerId: String,
        mangaId: String,
    ) = withContext(context) {
        val worker = workers[providerId] ?: return@withContext
        tasks.find { it.providerId == providerId && it.mangaId == mangaId }
            ?.chapterTasks
            ?.filter { it.state is ChapterDownloadTask.State.Failed }
            ?.forEach { it.state = ChapterDownloadTask.State.Waiting }
            ?.let { worker.start() }
    }

    suspend fun startChapterTask(
        providerId: String,
        mangaId: String,
        collectionId: String,
        chapterId: String,
    ) = withContext(context) {
        val worker = workers[providerId] ?: return@withContext
        tasks.find { it.providerId == providerId && it.mangaId == mangaId }
            ?.chapterTasks
            ?.find { it.collectionId == collectionId && it.chapterId == chapterId }
            ?.takeIf { it.state is ChapterDownloadTask.State.Failed }
            ?.let { it.state = ChapterDownloadTask.State.Waiting }
            ?.let { worker.start() }
    }

    suspend fun cancelAllTasks() = withContext(context) {
        tasks.clear()
        workers.values.forEach { it.cancel() }
    }

    suspend fun cancelMangaTask(
        providerId: String,
        mangaId: String,
    ) = withContext(context) {
        val worker = workers[providerId] ?: return@withContext
        tasks.removeIf { it.providerId == providerId && it.mangaId == mangaId }
        worker.cancelMangaTask(mangaId)
    }

    suspend fun cancelChapterTask(
        providerId: String,
        mangaId: String,
        collectionId: String,
        chapterId: String,
    ) = withContext(context) {
        val worker = workers[providerId] ?: return@withContext
        tasks.find { it.providerId == providerId && it.mangaId == mangaId }
            ?.chapterTasks
            ?.removeIf { it.collectionId == collectionId && it.chapterId == chapterId }
        worker.cancelChapterTask(mangaId, collectionId, chapterId)
    }

    suspend fun add(
        providerId: String,
        mangaId: String,
    ) = withContext(context) {
        val worker = workers[providerId] ?: return@withContext
        worker.createMangaDownloadTask(mangaId)?.let { task ->
            if (tasks.none { it.providerId == providerId && it.mangaId == mangaId }) {
                tasks.add(task)
                worker.start()
            }
        }
    }
}