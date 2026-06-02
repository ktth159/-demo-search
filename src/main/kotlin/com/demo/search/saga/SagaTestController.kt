package com.demo.search.saga

import com.demo.search.order.Order
import com.demo.search.order.OrderRepository
import com.demo.search.order.OrderStatus
import com.demo.search.payment.Payment
import com.demo.search.payment.PaymentRepository
import com.demo.search.payment.PaymentStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * SAGA 테스트 전용 컨트롤러
 *
 * 실제 토스페이먼츠 결제창을 거치지 않고, "결제 완료된 주문"을 시뮬레이션하여
 * SAGA 흐름(재고 차감 → 실패 시 보상 트랜잭션)을 검증한다.
 *
 * 실 서비스에서는 PaymentService.confirm()이 결제 성공 후 동일한 이벤트를 발행한다.
 */
@RestController
@RequestMapping("/test/saga")
class SagaTestController(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val sagaProducer: SagaProducer,
) {
    @PostMapping("/checkout")
    fun simulateCheckout(@RequestBody request: SagaTestRequest): ResponseEntity<Map<String, String>> {
        val orderId = UUID.randomUUID().toString()
        val mockPaymentKey = "mock_${UUID.randomUUID()}"

        // 1. 결제 완료 상태의 주문/결제 레코드 생성 (결제 성공 직후 상태를 재현)
        orderRepository.save(
            Order(
                orderId = orderId,
                productName = request.productName,
                amount = request.amount,
                productId = request.productId,
                status = OrderStatus.PAID,
            )
        )
        paymentRepository.save(
            Payment(
                orderId = orderId,
                paymentKey = mockPaymentKey,
                amount = request.amount,
                status = PaymentStatus.PAID,
                method = "테스트카드",
            )
        )

        // 2. SAGA 트리거: payment.completed 이벤트 발행
        //    → StockSagaConsumer가 재고 차감 시도
        //    → 재고 부족 시 보상 트랜잭션으로 결제 취소
        sagaProducer.sendPaymentCompleted(
            PaymentCompletedEvent(
                orderId = orderId,
                paymentKey = mockPaymentKey,
                productId = request.productId,
                amount = request.amount,
            )
        )

        return ResponseEntity.ok(
            mapOf(
                "orderId" to orderId,
                "message" to "SAGA 시작됨. /orders/$orderId 로 최종 상태를 확인하세요.",
            )
        )
    }
}

data class SagaTestRequest(
    val productName: String,
    val amount: Int,
    val productId: Long,
)
