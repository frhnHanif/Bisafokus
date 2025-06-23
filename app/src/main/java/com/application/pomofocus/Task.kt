package com.application.pomofocus // Sesuaikan dengan nama paket root Anda (contoh: com.application.pomofocus)

data class Task(
    val id: String,
    val text: String,
    var isCompleted: Boolean = false
)