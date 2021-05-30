package player.common

class PlayerException(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)