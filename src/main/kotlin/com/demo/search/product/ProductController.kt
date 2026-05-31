package com.demo.search.product

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/products")
class ProductController(private val productService: ProductService) {

    @PostMapping
    fun register(@RequestBody request: ProductRequest): ResponseEntity<Product> {
        return ResponseEntity.ok(productService.register(request))
    }
}
