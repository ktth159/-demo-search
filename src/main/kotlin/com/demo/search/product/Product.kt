package com.demo.search.product

import jakarta.persistence.*

@Entity
@Table(name = "products")
class Product(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val name: String,
    val description: String,
    val price: Int,
    val category: String,
)
