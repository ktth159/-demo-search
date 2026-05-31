package com.demo.search.es

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType

// Elasticsearch에 저장되는 문서. JPA Entity와 별개로 관리.
@Document(indexName = "products")
data class ProductDocument(
    @Id
    val id: String,

    @Field(type = FieldType.Text, analyzer = "standard")
    val name: String,

    @Field(type = FieldType.Text, analyzer = "standard")
    val description: String,

    @Field(type = FieldType.Integer)
    val price: Int,

    @Field(type = FieldType.Keyword)
    val category: String,
)
