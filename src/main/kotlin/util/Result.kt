package util

inline fun <R> safeRunCatching(block: () -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    }
}

inline fun <T, R> T.safeRunCatching(block: T.() -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    }
}

inline fun <T> Result<T>.mapFailure(transform: (value: Throwable) -> Throwable): Result<T> {
    return fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(transform(it)) }
    )
}

inline fun <R, T> Result<T>.andThen(transform: (value: T) -> Result<R>): Result<R> {
    return fold(
        onSuccess = { transform(it) },
        onFailure = { Result.failure(it) }
    )
}

inline fun <T> Result<T>.orElse(transform: (value: Throwable) -> Result<T>): Result<T> {
    return fold(
        onSuccess = { Result.success(it) },
        onFailure = { transform(it) }
    )
}
