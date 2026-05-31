package com.demo.search.es

import com.demo.search.rank.RankItem
import com.demo.search.rank.SearchRankService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/search")
class SearchController(
    private val searchRepository: ProductSearchRepository,
    private val rankService: SearchRankService,
) {

    // 키워드 검색 + 인기검색어 카운트 증가
    @GetMapping
    fun search(@RequestParam keyword: String): List<ProductDocument> {
        rankService.record(keyword)
        return searchRepository.findByNameContainingOrDescriptionContaining(keyword, keyword)
    }

    // 카테고리 필터 검색
    @GetMapping("/category/{category}")
    fun searchByCategory(@PathVariable category: String): List<ProductDocument> {
        return searchRepository.findByCategory(category)
    }

    // 인기검색어 순위 Top 10
    @GetMapping("/rank")
    fun rank(): List<RankItem> {
        return rankService.getTopKeywords(10)
    }

    // 특정 키워드의 현재 순위
    @GetMapping("/rank/{keyword}")
    fun keywordRank(@PathVariable keyword: String): Map<String, Any?> {
        return mapOf(
            "keyword" to keyword,
            "rank" to rankService.getRank(keyword),
        )
    }
}
