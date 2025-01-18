package com.example.googlesignintest

data class Stock (
    val symbol: String,
    val name: String,
    val price: Double,
    val tags: List<String>
)