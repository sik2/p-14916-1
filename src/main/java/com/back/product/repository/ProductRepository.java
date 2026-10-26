package com.back.product.repository;

import com.back.product.entity.Product;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.SearchResults;
import org.springframework.data.domain.Vector;
import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends CrudRepository<Product, Long> {

    // searchBy + Embedding(필드명) + Near : embedding 벡터가 가까운 순서로 찾는다
    SearchResults<Product> searchByEmbeddingNear(Vector vector, Score score, Limit limit);
}
