package com.back.product.tool;

import com.back.product.entity.Product;
import com.back.product.entity.ProductKeyword;
import com.back.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

// AI가 호출할 수 있는 도구 모음. 메서드의 반환값은 JSON으로 바뀌어 AI에게 전달된다.
// 지연 로딩(keywords)을 도구 안에서 읽으므로 트랜잭션 안에서 실행한다.
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSearchTool {

    private final ProductService productService;

    @Tool(description = "주어진 키워드와 유사한 상품을 KNN 벡터 검색으로 찾습니다. 검색 키워드와 의미적으로 유사한 상품의 이름과 키워드 목록을 반환합니다.")
    public List<ProductInfo> searchProducts(
            @ToolParam(description = "유사한 상품을 검색할 키워드들 (쉼표로 구분). 예: 'laptop, gaming, high-performance'") String keywords,
            @ToolParam(description = "반환할 최대 상품 수. 기본값은 5입니다.", required = false) Integer k) {

        List<String> keywordList = Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        return toInfos(productService.knnSearch(keywordList, limitOrDefault(k)));
    }

    @Tool(description = "특정 상품 ID를 기준으로 유사한 상품을 찾습니다. 키워드 임베딩을 기반으로 비슷한 특성을 가진 상품들을 반환합니다.")
    public List<ProductInfo> findSimilarProducts(
            @ToolParam(description = "유사한 상품을 찾을 기준 상품의 ID") Long productId,
            @ToolParam(description = "반환할 최대 유사 상품 수. 기본값은 5입니다.", required = false) Integer k) {

        return toInfos(productService.findSimilarProducts(productId, limitOrDefault(k)));
    }

    @Tool(description = "상품 ID로 특정 상품의 상세 정보를 조회합니다.")
    public ProductInfo getProductById(@ToolParam(description = "조회할 상품의 ID") Long productId) {
        return productService.findById(productId)
                .map(this::toInfo)
                .orElse(null);
    }

    @Tool(description = "시스템에 등록된 모든 상품 목록을 조회합니다.")
    public List<ProductInfo> getAllProducts() {
        return toInfos(productService.findAll());
    }

    // AI가 k를 빼고 호출하면 null이 들어온다. int로 받으면 null을 받을 수 없어 실패한다.
    private int limitOrDefault(Integer k) {
        return (k == null || k <= 0) ? 5 : k;
    }

    private List<ProductInfo> toInfos(List<Product> products) {
        return products.stream().map(this::toInfo).toList();
    }

    private ProductInfo toInfo(Product p) {
        return new ProductInfo(p.getId(), p.getName(),
                p.getKeywords().stream().map(ProductKeyword::getKeyword).toList());
    }

    // 엔티티 대신 AI에게 필요한 정보만 담은 DTO
    public record ProductInfo(Long id, String name, List<String> keywords) {}
}
