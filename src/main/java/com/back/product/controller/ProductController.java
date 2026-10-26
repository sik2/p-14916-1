package com.back.product.controller;

import com.back.product.entity.Product;
import com.back.product.service.ProductChatService;
import com.back.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductChatService productChatService;

    // ==================== CREATE ====================
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody CreateRequest request) {
        return ResponseEntity.ok(productService.create(request.name(), request.keywords()));
    }

    // ==================== READ ====================
    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Product>> findAll() {
        return ResponseEntity.ok(productService.findAll());
    }

    // ==================== UPDATE ====================
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody UpdateRequest request) {
        if (productService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(productService.update(id, request.name(), request.keywords()));
    }

    // ==================== DELETE ====================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== KNN SEARCH ====================
    @GetMapping("/search")
    public ResponseEntity<List<Product>> search(@RequestParam String query, @RequestParam(defaultValue = "5") int k) {
        return ResponseEntity.ok(productService.knnSearch(query, k));
    }

    @PostMapping("/search")
    public ResponseEntity<List<Product>> searchByKeywords(@RequestBody SearchRequest request) {
        return ResponseEntity.ok(productService.knnSearch(request.keywords(), request.k()));
    }

    @GetMapping("/{id}/similar")
    public ResponseEntity<List<Product>> findSimilarProducts(@PathVariable Long id, @RequestParam(defaultValue = "5") int k) {
        if (productService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(productService.findSimilarProducts(id, k));
    }

    // ==================== CHAT ====================
    // userId는 필수다. 기본값을 두면 userId를 안 보낸 모든 사용자가 한 대화방을 공유하게 된다.
    // (실무에서는 요청 파라미터 대신 로그인 정보에서 꺼낸다)
    @PostMapping("/chat")
    public ResponseEntity<ProductChatService.ChatResponse> chat(@RequestBody ChatRequest request, @RequestParam String userId) {
        return ResponseEntity.ok(productChatService.chat(request.message(), userId));
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam String message, @RequestParam String userId) {
        SseEmitter emitter = new SseEmitter(0L); // 0 : 서버 쪽 타임아웃 없음

        Disposable subscription = productChatService.chatStream(message, userId).subscribe(
                token -> {
                    try {
                        // 토큰을 JSON({"content":" How"})으로 보내야 앞 공백이 사라지지 않는다
                        emitter.send(SseEmitter.event().name("message").data(new ChatChunk(token), MediaType.APPLICATION_JSON));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::completeWithError, // 스트림 에러
                emitter::complete           // 스트림 정상 종료
        );

        // 클라이언트가 연결을 끊으면 LLM 스트림 구독도 끊는다 (안 끊으면 답변 끝까지 토큰을 계속 소비)
        emitter.onCompletion(subscription::dispose);
        emitter.onTimeout(subscription::dispose);
        emitter.onError(e -> subscription.dispose());

        return emitter;
    }

    // ==================== Request DTOs ====================
    public record CreateRequest(String name, List<String> keywords) {}
    public record UpdateRequest(String name, List<String> keywords) {}
    public record ChatRequest(String message) {}
    public record ChatChunk(String content) {}
    public record SearchRequest(List<String> keywords, int k) {
        public SearchRequest {
            if (k <= 0) k = 10; // k를 보내지 않으면 0 → 기본값 10
        }
    }
}
