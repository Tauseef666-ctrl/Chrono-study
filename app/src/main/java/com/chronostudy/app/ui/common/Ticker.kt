package com.chronostudy.app.ui.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits immediately and then every [intervalMillis]. Used to drive time-based UI. */
fun ticker(intervalMillis: Long): Flow<Long> = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(intervalMillis)
    }
}
