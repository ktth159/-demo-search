package com.demo.search.payment

import com.demo.search.order.OrderRepository
import com.demo.search.order.OrderStatus
import com.demo.search.saga.PaymentCompletedEvent
import com.demo.search.saga.SagaProducer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.Base64

@Service
class PaymentService(
    private val webClient: WebClient,
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val sagaProducer: SagaProducer,
    @Value("\${toss.payments.secret-key}") private val secretKey: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun confirm(request: PaymentConfirmRequest): PaymentConfirmResponse {
        // 1. 주문 조회 및 검증
        val order = orderRepository.findById(request.orderId)
            .orElseThrow { IllegalArgumentException("주문을 찾을 수 없습니다: ${request.orderId}") }

        // 2. 금액 검증 - 이중 결제 방지 핵심!
        //    프론트에서 금액을 조작해도 여기서 잡힘
        if (order.amount != request.amount) {
            throw IllegalArgumentException(
                "금액 불일치 — 예상: ${order.amount}원, 요청: ${request.amount}원"
            )
        }

        // 3. 멱등성 체크 - 같은 주문에 대해 중복 결제 방지
        paymentRepository.findByOrderId(request.orderId)?.let { existing ->
            log.warn("이미 처리된 결제 요청 orderId={}", request.orderId)
            return PaymentConfirmResponse.from(existing)
        }

        // 4. 토스페이먼츠 결제 승인 API 호출
        log.debug("토스페이먼츠 승인 요청 orderId={}, amount={}", request.orderId, request.amount)
        val pgResponse = callTossPaymentsConfirm(request)

        // 5. 결제 정보 저장
        val payment = paymentRepository.save(
            Payment(
                orderId = request.orderId,
                paymentKey = pgResponse.paymentKey,
                amount = pgResponse.totalAmount,
                status = PaymentStatus.PAID,
                method = pgResponse.method,
            )
        )

        // 6. 주문 상태 업데이트 PENDING → PAID
        order.status = OrderStatus.PAID
        orderRepository.save(order)

        // 7. SAGA 이벤트 발행 → 재고 서비스에게 차감 요청
        sagaProducer.sendPaymentCompleted(
            PaymentCompletedEvent(
                orderId = order.orderId,
                paymentKey = payment.paymentKey,
                productId = order.productId,
                amount = order.amount,
            )
        )

        log.debug("결제 완료 orderId={}, paymentKey={}", request.orderId, payment.paymentKey)
        return PaymentConfirmResponse.from(payment)
    }

    @Transactional
    fun cancel(paymentKey: String, reason: String): PaymentCancelResponse {
        // DB에서 결제 조회
        val payment = paymentRepository.findAll()
            .firstOrNull { it.paymentKey == paymentKey }
            ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentKey")

        // 토스페이먼츠 취소 API 호출
        val encoded = Base64.getEncoder().encodeToString("$secretKey:".toByteArray())
        val pgResponse = webClient.post()
            .uri("/v1/payments/$paymentKey/cancel")
            .header("Authorization", "Basic $encoded")
            .bodyValue(mapOf("cancelReason" to reason))
            .retrieve()
            .bodyToMono<Map<String, Any>>()
            .block() ?: throw RuntimeException("토스페이먼츠 취소 API 응답 없음")

        // 결제 상태 업데이트
        payment.status = PaymentStatus.CANCELLED
        paymentRepository.save(payment)

        // 주문 상태 업데이트
        orderRepository.findById(payment.orderId).ifPresent { order ->
            order.status = OrderStatus.CANCELLED
            orderRepository.save(order)
        }

        return PaymentCancelResponse(
            paymentKey = paymentKey,
            status = "CANCELLED",
            cancelReason = reason,
        )
    }

    private fun callTossPaymentsConfirm(request: PaymentConfirmRequest): TossPaymentsConfirmResponse {
        // Basic 인증: "시크릿키:" 를 Base64 인코딩
        val encoded = Base64.getEncoder().encodeToString("$secretKey:".toByteArray())

        return webClient.post()
            .uri("/v1/payments/confirm")
            .header("Authorization", "Basic $encoded")
            .bodyValue(
                mapOf(
                    "paymentKey" to request.paymentKey,
                    "orderId" to request.orderId,
                    "amount" to request.amount,
                )
            )
            .retrieve()
            .bodyToMono<TossPaymentsConfirmResponse>()
            .block() ?: throw RuntimeException("토스페이먼츠 API 응답 없음")
    }
}

// ── 요청/응답 DTO ────────────────────────────────────

data class PaymentConfirmRequest(
    val paymentKey: String,
    val orderId: String,
    val amount: Int,
)

data class PaymentConfirmResponse(
    val orderId: String,
    val paymentKey: String,
    val amount: Int,
    val status: String,
    val method: String?,
) {
    companion object {
        fun from(payment: Payment) = PaymentConfirmResponse(
            orderId = payment.orderId,
            paymentKey = payment.paymentKey,
            amount = payment.amount,
            status = payment.status.name,
            method = payment.method,
        )
    }
}

data class PaymentCancelResponse(
    val paymentKey: String,
    val status: String,
    val cancelReason: String,
)

// 토스페이먼츠 API 응답 모델
data class TossPaymentsConfirmResponse(
    val paymentKey: String,
    val orderId: String,
    val totalAmount: Int,
    val method: String?,
    val status: String,
)
