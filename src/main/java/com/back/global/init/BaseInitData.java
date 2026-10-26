package com.back.global.init;

import com.back.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class BaseInitData {

    private final ProductService productService;

    @Bean
    @Profile("!test") // 테스트에서는 각 테스트가 필요한 데이터를 직접 만든다
    public ApplicationRunner initDataRunner() {
        return args -> {
            if (!productService.findAll().isEmpty()) {
                return;
            }

            // Electronics
            productService.create("MacBook Pro 16", List.of("laptop", "apple", "development", "programming"));
            productService.create("MacBook Air M3", List.of("laptop", "apple", "lightweight", "portable"));
            productService.create("Dell XPS 15", List.of("laptop", "windows", "development", "high-performance"));
            productService.create("iPhone 15 Pro", List.of("smartphone", "apple", "camera", "5G"));
            productService.create("Samsung Galaxy S24", List.of("smartphone", "android", "camera", "AI"));
            productService.create("iPad Pro 12.9", List.of("tablet", "apple", "drawing", "creative"));
            productService.create("Sony WH-1000XM5", List.of("headphone", "wireless", "noise-cancelling", "music"));
            productService.create("AirPods Pro 2", List.of("earbuds", "apple", "wireless", "noise-cancelling"));

            // Clothing
            productService.create("North Face Puffer Jacket", List.of("outerwear", "winter", "warm", "outdoor"));
            productService.create("Patagonia Fleece", List.of("outerwear", "fleece", "outdoor", "hiking"));
            productService.create("Levi's 501 Jeans", List.of("pants", "denim", "casual", "classic"));
            productService.create("Nike Air Max 90", List.of("shoes", "sneakers", "running", "sports"));
            productService.create("Adidas Ultraboost", List.of("shoes", "running", "sports", "comfortable"));

            // Food & Beverage
            productService.create("Starbucks Pike Place Roast", List.of("coffee", "roasted", "beverage", "caffeine"));
            productService.create("Twinings Earl Grey Tea", List.of("tea", "earl-grey", "beverage", "british"));
            productService.create("Coca-Cola Zero", List.of("soda", "beverage", "sugar-free", "refreshing"));

            // Home & Kitchen
            productService.create("Dyson V15 Vacuum", List.of("vacuum", "cleaning", "home", "cordless"));
            productService.create("Instant Pot Duo", List.of("cooker", "kitchen", "pressure", "multi-function"));
            productService.create("Nespresso Vertuo", List.of("coffee-machine", "kitchen", "espresso", "capsule"));

            // Books & Media
            productService.create("Clean Code Book", List.of("book", "programming", "software", "education"));

            System.out.println("=== Initial data created: 20 products ===");
        };
    }
}
