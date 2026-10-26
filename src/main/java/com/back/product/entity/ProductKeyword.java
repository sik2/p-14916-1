package com.back.product.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore // Product -> keywords -> product -> keywords ... 무한 반복(순환 참조)을 끊는다
    private Product product;

    @Column
    @Getter
    @Setter
    private String keyword;

    public ProductKeyword(String keyword) {
        this.keyword = keyword;
    }
}
