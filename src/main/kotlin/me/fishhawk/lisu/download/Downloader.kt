package me.fishhawk.lisu.download

import dev.inmo.krontab.builder.buildSchedule
import dev.inmo.krontab.doInfinity
import kotlinx.coroutines.*
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.source.SourceManager

class Downloader(
    private val libraryManager: LibraryManager,
    sourceManager: SourceManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val workers =
        sourceManager.listSources()
            .map { Worker(libraryManager, it, scope) }
            .associateBy { it.id }

    private val updater = buildSchedule { hours { at(4) } }

    init {
        scope.launch {
            updateLibrary()
            updater.doInfinity { updateLibrary() }
        }
    }

    private suspend fun updateLibrary() {
        workers.values.forEach { it.updateLibrary() }
    }

    suspend fun startAll() {
        workers.values.forEach { it.startAll() }
    }

    suspend fun start(providerId: String, mangaId: String) {
        workers[providerId]?.start(mangaId)
    }

    suspend fun pauseAll() {
        workers.values.forEach { it.pauseAll() }
    }

    suspend fun pause(providerId: String, mangaId: String) {
        workers[providerId]?.pause(mangaId)
    }

    suspend fun add(providerId: String, mangaId: String) {
        workers[providerId]?.add(mangaId)
    }

    suspend fun remove(providerId: String, mangaId: String) {
        workers[providerId]?.remove(mangaId)
    }
}