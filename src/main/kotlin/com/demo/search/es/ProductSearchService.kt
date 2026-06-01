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
        var criteria = Criteria("isActive").`is`(true)

        // 키워드 검색 (name 또는 description 포함)
        if (!keyword.isNullOrBlank()) {
            val keywordCriteria = Criteria("name").contains(keyword)
                .or(Criteria("description").contains(keyword))
            criteria = criteria.and(keywordCriteria)
        }

        // 카테고리 필터
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

        // 정렬
        val sortOption = when (sort) {
            "price_asc" -> Sort.by(Sort.Direction.ASC, "price")
            "price_desc" -> Sort.by(Sort.Direction.DESC, "price")
            else -> Sort.unsorted()  // 기본: 관련도순 (_score)
        }

        // Query 타입으로 명시 → Overload resolution ambiguity 해결
        val query: Query = CriteriaQuery(criteria)
            .apply { setPageable(PageRequest.of(page, size, sortOption)) }

        return esOperations.search(query, ProductDocument::class.java)
            .map(SearchHit<ProductDocument>::getContent)
            .toList()
    }
}
