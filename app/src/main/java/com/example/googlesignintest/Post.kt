package com.example.googlesignintest


data class Post(
        val id: Int,
        val title: String,
        val content: String,
        val stockSymbol: String,
        val author: String,
        val timestamp: Long
    )