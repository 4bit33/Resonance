package com.resonance.player.core.common

/**
 * Explicit result type used across repository / use-case boundaries.
 *
 * UI layers render [Result] directly into Loading / Content / Error states.
 * Unchecked exceptions must never reach Composables; they are mapped to
 * [AppError] at the data-layer boundary.
 */
sealed interface Result<out T> {
    data class Success<T>(val value: T) : Result<T>
    data class Failure(val error: AppError) : Result<Nothing>
    data object Loading : Result<Nothing>
}

fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(value))
    is Result.Failure -> this
    Result.Loading -> Result.Loading
}

fun <T, R> Result<T>.fold(onFailure: (AppError) -> R, onSuccess: (T) -> R): R = when (this) {
    is Result.Success -> onSuccess(value)
    is Result.Failure -> onFailure(error)
    Result.Loading -> onFailure(AppError.Unknown(reason = "Result is still loading"))
}

fun <T> Result<T>.getOrNull(): T? = (this as? Result.Success)?.value

fun <T> Result<T>.errorOrNull(): AppError? = (this as? Result.Failure)?.error
