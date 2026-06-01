package com.demo.search.saga

import com.demo.search.inventory.StockService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

// SAGA Step 2: 결제 완료 이벤트 수신 → 재고 차감
// 재고 부족 시 보상 이벤트 발행
@Component
class StockSagaConsumer(
    private val stockService: StockService,
    private val sagaProducer: SagaProducer,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = [SagaTopic.PAYMENT_COMPLETED], groupId = "saga-stock-group")
    fun consume(payload: String) {
        val event = objectMapper.readValue(payload, PaymentCompletedEvent::class.java)
        log.debug("SAGA 재고 차감 시작 orderId={}, productId={}", event.orderId, event.productId)

        // productId 없으면 재고 차감 스킵 (단순 결제만 하는 경우)
        val productId = event.productId ?: run {
            log.debug("productId 없음, 재고 차감 스킵 orderId={}", event.orderId)
            return
        }

        // 재고 차감 시도
        val success = stockService.decrease(productId)

        if (!success) {
            // 재고 부족 → 보상 트랜잭션: 결제 취소 이벤트 발행
            log.warn("재고 부족 → 결제 취소 이벤트 발행 orderId={}", event.orderId)
            sagaProducer.sendStockDecreaseFailed(
                StockDecreaseFailedEvent(
                    orderId = event.orderId,
                    paymentKey = event.paymentKey,
                    reason = "재고 부족으로 인한 자동 결제 취소",
                )
            )
        } else {
            log.debug("SAGA 재고 차감 완료 orderId={}", event.orderId)
        }
    }
}
