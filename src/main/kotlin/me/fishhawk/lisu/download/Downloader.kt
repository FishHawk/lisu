package me.fishhawk.lisu.download

import kotlinx.coroutines.*
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.library.Manga
import me.fishhawk.lisu.source.Source
import me.fishhawk.lisu.source.SourceManager

internal class Worker(
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
                    download(source, manga)
                    waitingMangas.remove(mangaId)
                } catch (throwable: Throwable) {
                    errorMangas[mangaId] = throwable
                }
            }
        }
    }
}

private suspend fun download(source: Source, manga: Manga) {
    val mangaDetail = source.getManga(manga.id)
    manga.takeIf { !it.hasMetadata() }?.updateMetadata(mangaDetail.metadataDetail)
    manga.takeIf { !it.hasCover() }?.let {
        mangaDetail.cover?.let { cover ->
            it.updateCover(source.getImage(cover))
        }
    }
}

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
