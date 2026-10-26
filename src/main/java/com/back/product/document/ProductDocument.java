package com.back.product.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

// Elasticsearch의 products 인덱스에 저장되는 문서. 상품 정보는 PostgreSQL에 있고, 여기에는 id와 벡터만 둔다.
@Document(indexName = "products")
@Data
public class ProductDocument {
    @Id
    private Long id; // Product.id와 같은 값

    // dims : 임베딩 차원(AiConfig.EMBEDDING_DIMENSIONS)과 같아야 한다
    // similarity : KNN 검색 때 쓸 유사도 함수
    @Field(type = FieldType.Dense_Vector, dims = 384, similarity = "cosine")
    private float[] embedding;
}
