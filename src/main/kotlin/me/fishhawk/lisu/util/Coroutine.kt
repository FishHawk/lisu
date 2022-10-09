package me.fishhawk.lisu.util

import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select

suspend inline fun <T, C : Iterable<T>> C.forEachParallel(
    limit: Int,
    crossinline action: suspend (T) -> Unit
) = coroutineScope {
    val executing = mutableListOf<Deferred<Unit>>()
    forEach { value ->
        executing.add(async { action(value) })
        if (executing.size >= limit)
            select { executing.onEach { it.onJoin { } } }
        executing.removeIf { it.isCompleted }
    }
    executing.awaitAll()
}

suspend fun <T> retry(
    times: Int,
    backoff: Long = 100,
    block: suspend () -> Result<T>
): Result<T> {
    repeat(times - 1) {
        val result = block()
        if (result.isSuccess) return result
        delay(backoff)
    }
    return block()
}
