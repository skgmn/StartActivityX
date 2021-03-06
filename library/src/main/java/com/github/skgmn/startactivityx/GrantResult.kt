package com.github.skgmn.startactivityx

enum class GrantResult {
    ALREADY_GRANTED,
    JUST_GRANTED,
    DENIED,
    DO_NOT_ASK_AGAIN;

    val granted: Boolean
        get() = this === ALREADY_GRANTED || this === JUST_GRANTED

    val denied: Boolean
        get() = this === DENIED || this === DO_NOT_ASK_AGAIN
}