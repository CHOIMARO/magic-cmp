package com.company.magiccmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform