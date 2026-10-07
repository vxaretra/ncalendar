package data

import kotlin.random.Random

fun generateId(): String =
    Random.nextLong().toString(16) + "-" + Random.nextLong().toString(16)
