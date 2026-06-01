package com.demo.search.es

import com.demo.search.rank.RankItem
import com.demo.search.rank.SearchRankService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/search")
class SearchController(
    private val productSearchService: ProductSearchService,
    private val searchRepository: ProductSearchRepository,
    private val rankService: SearchRankService,
) {
    /**
     * 상품 검색 API
     *
     * GET /search?keyword=나이키
     * GET /search?keyword=러닝화&category=shoes
     * GET /search?keyword=운동화&minPrice=50000&maxPrice=200000
     * GET /search?keyword=나이키&sort=price_asc
     * GET /search?category=shoes&sort=price_desc&page=0&size=5
     */
    @GetMapping
    fun search(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) minPrice: Int?,
        @RequestParam(required = false) maxPrice: Int?,
        @RequestParam(required = false, defaultValue = "") sort: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int,
    ): List<ProductDocument> {
        // 검색어가 있을 때만 인기검색어 카운트
        if (!keyword.isNullOrBlank()) {
            rankService.record(keyword)
        }
        return productSearchService.search(keyword, category, minPrice, maxPrice, sort, page, size)
    }

    // 인기검색어 Top 10
    @GetMapping("/rank")
    fun rank(): List<RankItem> = rankService.getTopKeywords(10)

    // 특정 키워드 순위
    @GetMapping("/rank/{keyword}")
    fun keywordRank(@PathVariable keyword: String): Map<String, Any?> =
        mapOf("keyword" to keyword, "rank" to rankService.getRank(keyword))
}
