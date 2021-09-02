package com.github.skgmn.startactivityx

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
internal fun LifecycleOwner.isStarted(): Flow<Boolean> {
    return callbackFlow {
        send(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        val observer = LifecycleEventObserver { _, _ ->
            trySend(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        }
        lifecycle.addObserver(observer)
        awaitClose {
            lifecycle.removeObserver(observer)
        }
    }
        .buffer(Channel.Factory.CONFLATED)
        .flowOn(Dispatchers.Main.immediate)
        .distinctUntilChanged()
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun LifecycleOwner.watchLifecycleEvent(): Flow<Lifecycle.Event> {
    return callbackFlow {
        val observer = LifecycleEventObserver { _, event ->
            trySend(event)
        }
        lifecycle.addObserver(observer)
        awaitClose {
            lifecycle.removeObserver(observer)
        }
    }
        .buffer(Channel.Factory.UNLIMITED)
        .flowOn(Dispatchers.Main.immediate)
}

