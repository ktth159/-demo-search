package com.demo.search.saga

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class SagaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendPaymentCompleted(event: PaymentCompletedEvent) {
        val payload = objectMapper.writeValueAsString(event)
        kafkaTemplate.send(SagaTopic.PAYMENT_COMPLETED, event.orderId, payload)
        log.debug("SAGA 이벤트 발행 → {} orderId={}", SagaTopic.PAYMENT_COMPLETED, event.orderId)
    }

    fun sendStockDecreaseFailed(event: StockDecreaseFailedEvent) {
        val payload = objectMapper.writeValueAsString(event)
        kafkaTemplate.send(SagaTopic.STOCK_DECREASE_FAILED, event.orderId, payload)
        log.debug("SAGA 이벤트 발행 → {} orderId={}", SagaTopic.STOCK_DECREASE_FAILED, event.orderId)
    }
}
