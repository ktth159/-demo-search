package com.demo.search.product

import com.demo.search.event.ProductEvent
import com.demo.search.event.ProductEventProducer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val eventProducer: ProductEventProducer,
) {
    @Transactional
    fun register(request: ProductRequest): Product {
        val product = productRepository.save(
            Product(
                name = request.name,
                description = request.description,
                price = request.price,
                category = request.category,
            )
        )
        // DB 저장 후 Kafka 이벤트 발행 → Consumer가 ES에 인덱싱
        eventProducer.send(
            ProductEvent(
                id = product.id,
                name = product.name,
                description = product.description,
                price = product.price,
                category = product.category,
            )
        )
        return product
    }
}

data class ProductRequest(
    val name: String,
    val description: String,
    val price: Int,
    val category: String,
)
