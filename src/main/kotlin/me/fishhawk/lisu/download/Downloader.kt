package me.fishhawk.lisu.download

import dev.inmo.krontab.builder.buildSchedule
import dev.inmo.krontab.doInfinity
import dev.inmo.krontab.doInfinityTz
import dev.inmo.krontab.utils.Minutes
import kotlinx.coroutines.*
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.source.SourceManager
import java.util.TimeZone

class Downloader(
    private val libraryManager: LibraryManager,
    sourceManager: SourceManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val workers =
        sourceManager.listSources()
            .map { Worker(libraryManager, it, scope) }
            .associateBy { it.id }

    private val updater = buildSchedule(
        offset = TimeZone.getDefault().rawOffset / 1000 / 60
    ) {
        hours { at(4) }
    }

    init {
        scope.launch {
            updateLibrary()
            updater.doInfinityTz {
                updateLibrary()
            }
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