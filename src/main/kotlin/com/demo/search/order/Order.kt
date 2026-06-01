package com.demo.search.order

import jakarta.persistence.*

@Entity
@Table(name = "orders")
class Order(
    @Id
    val orderId: String,            // 클라이언트가 생성한 UUID (토스페이먼츠 orderId)

    val productName: String,
    val amount: Int,                // 결제 금액 (원)
    val productId: Long? = null,    // 재고 차감 대상 상품 ID (SAGA용)

    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.PENDING,
)

enum class OrderStatus {
    PENDING,    // 주문 생성, 결제 대기
    PAID,       // 결제 완료
    CANCELLED,  // 취소
}
