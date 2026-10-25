package com.back.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column
    @Getter
    @Setter
    private String name;

    // mappedBy : 외래키(product_id)는 ProductKeyword.product 쪽이 관리한다(연관관계의 주인)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Getter
    private List<ProductKeyword> keywords = new ArrayList<>();

    // 연관관계 편의 메서드 : 양쪽을 한 번에 연결한다
    public void addKeyword(ProductKeyword keyword) {
        keywords.add(keyword);
        keyword.setProduct(this);
    }

    public void removeKeyword(ProductKeyword keyword) {
        keywords.remove(keyword);
        keyword.setProduct(null);
    }

    public void addKeyword(String keywordText) {
        addKeyword(new ProductKeyword(keywordText));
    }
}
