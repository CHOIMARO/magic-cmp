package com.company.magic

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform