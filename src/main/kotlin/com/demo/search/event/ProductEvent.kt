package com.demo.search.event

data class ProductEvent(
    val id: Long,
    val name: String,
    val description: String,
    val price: Int,
    val category: String,
)
