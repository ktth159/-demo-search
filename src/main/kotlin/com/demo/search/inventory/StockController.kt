package com.demo.search.inventory

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/stocks")
class StockController(private val stockService: StockService) {

    // 재고 초기화 (테스트용)
    @PostMapping
    fun initialize(@RequestBody request: StockInitRequest): ResponseEntity<StockResponse> {
        val stock = stockService.initialize(request.productId, request.quantity)
        return ResponseEntity.ok(StockResponse(stock.productId, stock.quantity))
    }

    // 재고 조회
    @GetMapping("/{productId}")
    fun getStock(@PathVariable productId: Long): ResponseEntity<StockResponse> {
        return ResponseEntity.ok(StockResponse(productId, stockService.getStock(productId)))
    }
}

data class StockInitRequest(val productId: Long, val quantity: Int)
data class StockResponse(val productId: Long, val quantity: Int)
