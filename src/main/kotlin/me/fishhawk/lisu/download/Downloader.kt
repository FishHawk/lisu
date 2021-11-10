package me.fishhawk.lisu.download

import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.coroutines.*
import me.fishhawk.lisu.library.Library
import me.fishhawk.lisu.library.Manga
import me.fishhawk.lisu.model.MetadataDetailDto
import me.fishhawk.lisu.provider.Provider
import me.fishhawk.lisu.provider.ProviderManager

class Worker(
    private val library: Library,
    private val provider: Provider
) {
    private var currentJob: Job? = null
    private val waitingQueue = mutableSetOf<String>()
    private val errorQueue = mutableMapOf<String, Throwable>()

    suspend fun add(mangaId: String) {
        waitingQueue.add(mangaId)
        errorQueue.remove(mangaId)
        if (currentJob == null) start()
    }

    suspend fun pause() {
        currentJob?.cancelAndJoin()
    }

    suspend fun start() = coroutineScope {
        currentJob = launch {
            while (true) {
                val mangaId = waitingQueue.firstOrNull() ?: break
                try {
                    val manga = library.getManga(provider.id, mangaId)!!
                    download(provider, manga)
                    waitingQueue.remove(mangaId)
                } catch (throwable: Throwable) {
                    errorQueue[mangaId] = throwable
                }
            }
        }
    }
}


private suspend fun download(provider: Provider, manga: Manga) {
    val mangaDetail = provider.getManga(manga.id)
    mangaDetail.cover?.let {
        if (!manga.hasCover()) {
            val cover = provider.getImage(it)
            manga.updateCover(cover)
        }
    }
    if (!manga.hasMetadata()) {
        manga.updateMetadata(
            MetadataDetailDto(
                title = mangaDetail.title,
                authors = mangaDetail.authors,
                isFinished = mangaDetail.isFinished,
                description = mangaDetail.description,
                tags = mangaDetail.tags
            )
        )
    }
}


class Downloader(
    library: Library,
    manager: ProviderManager
) {
    private val context = newSingleThreadContext("downloader")

    private val workers = manager.providers.mapValues { Worker(library, it.value) }

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
