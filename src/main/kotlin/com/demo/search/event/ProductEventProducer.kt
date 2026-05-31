package com.demo.search.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class ProductEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun send(event: ProductEvent) {
        val payload = objectMapper.writeValueAsString(event)
        kafkaTemplate.send(TOPIC, event.id.toString(), payload)
        log.debug("Kafka 발행 → topic={}, productId={}", TOPIC, event.id)
    }

    companion object {
        const val TOPIC = "product.created"
    }
}
