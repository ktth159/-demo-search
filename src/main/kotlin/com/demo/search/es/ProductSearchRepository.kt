package com.demo.search.es

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

interface ProductSearchRepository : ElasticsearchRepository<ProductDocument, String> {

    // name 또는 description에 키워드 포함 검색
    fun findByNameContainingOrDescriptionContaining(name: String, description: String): List<ProductDocument>

    // 카테고리 필터
    fun findByCategory(category: String): List<ProductDocument>
}
