package com.demo.search.rank

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

// Redis Sorted Set(ZSet)으로 인기검색어를 관리
// key: "search:rank", member: 검색어, score: 누적 검색 횟수
@Service
class SearchRankService(private val redis: StringRedisTemplate) {

    private val KEY = "search:rank"

    // 검색할 때 호출 → 해당 키워드 score +1
    fun record(keyword: String) {
        redis.opsForZSet().incrementScore(KEY, keyword, 1.0)
    }

    // 상위 N개 인기검색어 반환 (score 내림차순)
    fun getTopKeywords(limit: Long = 10): List<RankItem> {
        return redis.opsForZSet()
            .reverseRangeWithScores(KEY, 0, limit - 1)
            ?.mapIndexed { index, tuple ->
                RankItem(
                    rank = index + 1,
                    keyword = tuple.value ?: "",
                    count = tuple.score?.toLong() ?: 0,
                )
            } ?: emptyList()
    }

    // 특정 키워드의 현재 순위 조회
    fun getRank(keyword: String): Long? {
        // reverseRank는 0-based → +1
        return redis.opsForZSet().reverseRank(KEY, keyword)?.plus(1)
    }
}

data class RankItem(val rank: Int, val keyword: String, val count: Long)
