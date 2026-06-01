package com.demo.search.saga

// Kafka 토픽 상수
object SagaTopic {
    const val PAYMENT_COMPLETED = "payment.completed"       // 결제 완료 → 재고 차감
    const val STOCK_DECREASE_FAILED = "stock.decrease.failed" // 재고 부족 → 결제 취소
}

// 결제 완료 이벤트 (PaymentService → StockSagaConsumer)
data class PaymentCompletedEvent(
    val orderId: String,
    val paymentKey: String,
    val productId: Long?,   // null이면 재고 차감 스킵
    val amount: Int,
)

// 재고 차감 실패 이벤트 (StockSagaConsumer → PaymentCompensationConsumer)
data class StockDecreaseFailedEvent(
    val orderId: String,
    val paymentKey: String,
    val reason: String,
)
