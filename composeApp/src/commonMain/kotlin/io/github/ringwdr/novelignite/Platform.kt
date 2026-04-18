package io.github.ringwdr.novelignite

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform