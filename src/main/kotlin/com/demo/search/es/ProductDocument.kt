package com.demo.search.es

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.annotations.Setting

// nori 분석기 설정 파일 참조
@Document(indexName = "products")
@Setting(settingPath = "elasticsearch/product-index-settings.json")
data class ProductDocument(
    @Id
    val id: String,

    // nori: 한국어 형태소 분석기 (나이키→나이키, 러닝화→러닝/화)
    @Field(type = FieldType.Text, analyzer = "nori_analyzer", searchAnalyzer = "nori_analyzer")
    val name: String,

    @Field(type = FieldType.Text, analyzer = "nori_analyzer", searchAnalyzer = "nori_analyzer")
    val description: String,

    // Keyword: 정확히 일치, 집계/정렬용 (nori 분석 안 함)
    @Field(type = FieldType.Keyword)
    val category: String,

    @Field(type = FieldType.Integer)
    val price: Int,

    @Field(type = FieldType.Boolean)
    val isActive: Boolean = true,
)
