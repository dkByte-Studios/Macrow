package com.macrow.app.ui

import com.macrow.app.data.BackupException
import com.macrow.app.data.LookupException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** One place for messages that should reach the user as a snackbar, so no failure is silent. */
object UiErrors {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    fun say(message: String) {
        _events.tryEmit(message)
    }

    fun report(t: Throwable) {
        say(
            when (t) {
                is BackupException -> t.message ?: "Something went wrong with the backup."
                is LookupException -> t.message ?: "Couldn't look that up."
                else -> "Something went wrong. Your data is safe."
            },
        )
    }
}

val safeLaunch = CoroutineExceptionHandler { _, t -> UiErrors.report(t) }
