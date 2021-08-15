package com.github.skgmn.startactivityx

enum class PermissionStatus {
    GRANTED,
    DENIED,
    DO_NOT_ASK_AGAIN;

    val granted: Boolean
        get() = this === GRANTED

    val denied: Boolean
        get() = this === DENIED || this === DO_NOT_ASK_AGAIN
}