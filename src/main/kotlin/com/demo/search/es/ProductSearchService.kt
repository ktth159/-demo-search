package com.demo.search.es

import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.data.elasticsearch.core.query.Query
import org.springframework.stereotype.Service

@Service
class ProductSearchService(private val esOperations: ElasticsearchOperations) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 복합 검색: 키워드 + 카테고리 필터 + 가격 범위 + 정렬
     *
     * GET /search?keyword=나이키
     * GET /search?keyword=러닝화&category=shoes
     * GET /search?minPrice=50000&maxPrice=200000
     * GET /search?keyword=나이키&sort=price_asc
     */
    fun search(
        keyword: String?,
        category: String?,
        minPrice: Int?,
        maxPrice: Int?,
        sort: String?,
        page: Int = 0,
        size: Int = 10,
    ): List<ProductDocument> {
        log.debug("ES 검색 keyword={}, category={}, minPrice={}, maxPrice={}, sort={}", keyword, category, minPrice, maxPrice, sort)

        // Criteria 기반 쿼리 빌드
        // 활성 상품만 (기본 필터)
        var criteria = Criteria("isActive").`is`(true)

        // 카테고리 필터 (Keyword 타입 - 정확히 일치)
        if (!category.isNullOrBlank()) {
            criteria = criteria.and(Criteria("category").`is`(category))
        }

        // 가격 범위 필터
        if (minPrice != null) {
            criteria = criteria.and(Criteria("price").greaterThanEqual(minPrice))
        }
        if (maxPrice != null) {
            criteria = criteria.and(Criteria("price").lessThanEqual(maxPrice))
        }

        // 키워드 검색: name 또는 description에 match (nori 분석 적용)
        // subCriteria로 OR 조건을 괄호로 묶어 위 AND 필터들과 올바르게 결합
        if (!keyword.isNullOrBlank()) {
            criteria = criteria.subCriteria(
                Criteria("name").matches(keyword)
                    .or(Criteria("description").matches(keyword))
            )
        }

        // 정렬
        val sortOption = when (sort) {
            "price_asc" -> Sort.by(Sort.Direction.ASC, "price")
            "price_desc" -> Sort.by(Sort.Direction.DESC, "price")
            else -> Sort.unsorted()  // 기본: 관련도순 (_score)
        }

        // CriteriaQuery 생성자에 Pageable 직접 전달
        val query: Query = CriteriaQuery(criteria, PageRequest.of(page, size, sortOption))

        return esOperations.search(query, ProductDocument::class.java)
            .map { it.content }
            .toList()
    }
}
