package com.github.skgmn.startactivityx

enum class AcquirePermissionsResult {
    ALREADY_GRANTED,
    JUST_GRANTED,
    DENIED;

    val granted: Boolean
        get() = this === ALREADY_GRANTED || this === JUST_GRANTED

    val denied: Boolean
        get() = this === DENIED
}
