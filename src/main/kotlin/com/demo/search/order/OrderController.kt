package com.demo.search.order

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/orders")
class OrderController(private val orderRepository: OrderRepository) {

    // 주문 생성 → 결제 전 첫 단계
    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): ResponseEntity<OrderResponse> {
        val order = Order(
            orderId = UUID.randomUUID().toString(),  // 토스페이먼츠에 넘길 orderId
            productName = request.productName,
            amount = request.amount,
        )
        orderRepository.save(order)
        return ResponseEntity.ok(OrderResponse.from(order))
    }

    // 주문 조회
    @GetMapping("/{orderId}")
    fun getOrder(@PathVariable orderId: String): ResponseEntity<OrderResponse> {
        val order = orderRepository.findById(orderId)
            .orElseThrow { RuntimeException("주문을 찾을 수 없습니다: $orderId") }
        return ResponseEntity.ok(OrderResponse.from(order))
    }
}

data class CreateOrderRequest(
    val productName: String,
    val amount: Int,
)

data class OrderResponse(
    val orderId: String,
    val productName: String,
    val amount: Int,
    val status: String,
) {
    companion object {
        fun from(order: Order) = OrderResponse(
            orderId = order.orderId,
            productName = order.productName,
            amount = order.amount,
            status = order.status.name,
        )
    }
}
