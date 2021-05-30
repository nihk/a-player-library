package player.common

fun <T> T?.requireNotNull(): T {
    return requireNotNull(this)
}
