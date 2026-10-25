package com.back.product.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
public class ProductKeyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @Setter
    @Getter
    private Product product;

    @Column
    @Getter
    @Setter
    private String keyword;

    public ProductKeyword(String keyword) {
        this.keyword = keyword;
    }
}
