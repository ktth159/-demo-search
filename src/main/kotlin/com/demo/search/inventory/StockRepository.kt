package com.demo.search.inventory

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import jakarta.persistence.LockModeType

interface StockRepository : JpaRepository<Stock, Long> {
    fun findByProductId(productId: Long): Stock?

    // 비관적 락: SAGA에서 재고 차감 시 동시성 보호
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockByProductId(productId: Long): Stock?
}
