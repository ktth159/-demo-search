package com.demo.search.saga

import com.demo.search.order.OrderRepository
import com.demo.search.order.OrderStatus
import com.demo.search.payment.PaymentRepository
import com.demo.search.payment.PaymentStatus
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import java.util.Base64

// SAGA Step 3: 재고 차감 실패 → 보상 트랜잭션 (결제 취소)
@Component
class PaymentCompensationConsumer(
    private val webClient: WebClient,
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${toss.payments.secret-key}") private val secretKey: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [SagaTopic.STOCK_DECREASE_FAILED], groupId = "saga-compensation-group")
    @Transactional
    fun consume(payload: String) {
        val event = objectMapper.readValue(payload, StockDecreaseFailedEvent::class.java)
        log.warn("보상 트랜잭션 시작 — 결제 취소 orderId={}", event.orderId)

        try {
            // 1. 토스페이먼츠 결제 취소 API 호출
            val encoded = Base64.getEncoder().encodeToString("$secretKey:".toByteArray())
            webClient.post()
                .uri("/v1/payments/${event.paymentKey}/cancel")
                .header("Authorization", "Basic $encoded")
                .bodyValue(mapOf("cancelReason" to event.reason))
                .retrieve()
                .bodyToMono<Map<String, Any>>()
                .block()

            // 2. DB 결제 상태 → CANCELLED
            paymentRepository.findByOrderId(event.orderId)?.let {
                it.status = PaymentStatus.CANCELLED
                paymentRepository.save(it)
            }

            // 3. DB 주문 상태 → CANCELLED
            orderRepository.findById(event.orderId).ifPresent {
                it.status = OrderStatus.CANCELLED
                orderRepository.save(it)
            }

            log.warn("보상 트랜잭션 완료 — 결제 취소됨 orderId={}", event.orderId)

        } catch (e: Exception) {
            // 보상 트랜잭션 실패 → 실무에서는 DLQ(Dead Letter Queue)로 보내 수동 처리
            log.error("보상 트랜잭션 실패 orderId={}, error={}", event.orderId, e.message)
        }
    }
}
