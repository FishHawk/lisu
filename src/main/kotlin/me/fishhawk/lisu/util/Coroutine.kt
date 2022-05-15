package me.fishhawk.lisu.util

import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select

suspend inline fun <T, C : Iterable<T>> C.forEachIndexedParallel(
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
