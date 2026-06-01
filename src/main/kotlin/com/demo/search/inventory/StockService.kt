package com.demo.search.inventory

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StockService(private val stockRepository: StockRepository) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 재고 등록 (테스트용)
    @Transactional
    fun initialize(productId: Long, quantity: Int): Stock {
        stockRepository.findByProductId(productId)?.let {
            it.quantity = quantity
            return stockRepository.save(it)
        }
        return stockRepository.save(Stock(productId = productId, quantity = quantity))
    }

    // 재고 차감 - SAGA StockConsumer에서 호출
    // 성공: true, 재고 부족: false
    @Transactional
    fun decrease(productId: Long, quantity: Int = 1): Boolean {
        val stock = stockRepository.findWithLockByProductId(productId)
            ?: run {
                log.warn("재고 정보 없음 productId={}", productId)
                return false
            }

        if (stock.quantity < quantity) {
            log.warn("재고 부족 productId={}, 현재={}, 요청={}", productId, stock.quantity, quantity)
            return false
        }

        stock.quantity -= quantity
        stockRepository.save(stock)
        log.debug("재고 차감 완료 productId={}, 남은재고={}", productId, stock.quantity)
        return true
    }

    // 재고 복원 - 보상 트랜잭션에서 호출
    @Transactional
    fun restore(productId: Long, quantity: Int = 1) {
        val stock = stockRepository.findByProductId(productId) ?: return
        stock.quantity += quantity
        stockRepository.save(stock)
        log.debug("재고 복원 완료 productId={}, 남은재고={}", productId, stock.quantity)
    }

    // 재고 조회
    fun getStock(productId: Long): Int {
        return stockRepository.findByProductId(productId)?.quantity ?: 0
    }
}
