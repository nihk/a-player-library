package library.common

fun <T> T?.requireNotNull(): T {
    return requireNotNull(this)
}