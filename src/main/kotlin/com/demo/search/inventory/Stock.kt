package com.demo.search.inventory

import jakarta.persistence.*

@Entity
@Table(name = "stocks")
class Stock(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true)
    val productId: Long,

    var quantity: Int,              // 현재 재고 수량

    @Version
    var version: Long = 0,          // 낙관적 락 - 동시 수정 방지
)
