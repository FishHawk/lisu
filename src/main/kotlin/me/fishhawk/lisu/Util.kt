package me.fishhawk.lisu

inline fun <R> runCatchingException(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    }
}

inline fun <T, R> T.runCatchingException(block: T.() -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    }
}

inline fun <R, T> Result<T>.mapCatchingException(transform: (value: T) -> R): Result<R> {
    return fold(
        onSuccess = { runCatchingException { transform(it) } },
        onFailure = { Result.failure(it) }
    )
}

inline fun <R, T> Result<T>.then(transform: (value: T) -> Result<R>): Result<R> {
    return fold(
        onSuccess = { transform(it) },
        onFailure = { Result.failure(it) }
    )
}