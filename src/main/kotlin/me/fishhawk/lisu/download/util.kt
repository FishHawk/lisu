package me.fishhawk.lisu.download

import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select

internal suspend inline fun <T, C : Iterable<T>> C.forEachIndexedParallel(
    limit: Int,
    crossinline action: suspend (index: Int, T) -> Unit
) = coroutineScope {
    val executing = mutableListOf<Deferred<Unit>>()
    forEachIndexed { index, value ->
        executing.add(async { action(index, value) })
        if (executing.size >= limit)
            select<Unit> { executing.onEach { it.onJoin { } } }
        executing.removeIf { it.isCompleted }
    }
    executing.awaitAll()
}

internal suspend fun <T> retry(
    times: Int,
    backoff: Long = 100,
    block: suspend () -> T
): T {
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Throwable) {
        }
        delay(backoff)
    }
    return block()
}
