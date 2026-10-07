package com.example.ncalendar

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform