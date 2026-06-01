package com.demo.search.event

import com.demo.search.es.ProductDocument
import com.demo.search.es.ProductSearchRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class ProductEventConsumer(
    private val searchRepository: ProductSearchRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Kafka 메시지를 받아서 Elasticsearch에 인덱싱
    @KafkaListener(topics = [ProductEventProducer.TOPIC])
    fun consume(payload: String) {
        val event = objectMapper.readValue(payload, ProductEvent::class.java)
        val document = ProductDocument(
            id = event.id.toString(),
            name = event.name,
            description = event.description,
            price = event.price,
            category = event.category,
            isActive = true,
        )
        searchRepository.save(document)
        log.debug("ES 인덱싱 완료 → productId={}, name={}", event.id, event.name)
    }
}
