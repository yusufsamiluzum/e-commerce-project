package com.ecommerce.controller.impl; // Eğer farklı bir paket oluşturduysanız, onu yazın

import com.ecommerce.config.securityconfig.SecurityUtils;
import com.ecommerce.dto.DtoOrderResponse;
import com.ecommerce.services.OrderService; // OrderService'i import etmeniz gerekebilir

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Satıcıya özel işlemleri yöneten REST Controller.
 * Bu controller'a erişim genellikle 'SELLER' rolü gerektirir.
 */
@RestController
@RequestMapping("/api/v1/seller") // Bu controller altındaki tüm endpoint'lerin başına /api/v1/seller gelecek
@RequiredArgsConstructor // Lombok kullanıyorsanız final alanlar için constructor oluşturur
@PreAuthorize("hasRole('SELLER')") // Bu controller'daki tüm endpoint'lere sadece SELLER rolü erişebilir
public class SellerController {

    // Gerekli servisleri inject et (Lombok @RequiredArgsConstructor ile veya @Autowired ile)
    private final OrderService orderService;

    /**
     * Giriş yapmış olan satıcının kendi siparişlerini listeler.
     * Endpoint: GET /api/v1/seller/orders/my
     *
     * @param authentication Spring Security tarafından sağlanan kimlik doğrulama bilgisi
     * @return Satıcının siparişlerinin listesi (DtoOrderResponse formatında)
     */
    @GetMapping("/orders/my")
    public ResponseEntity<List<DtoOrderResponse>> getMyOrders(Authentication authentication) {
        // SecurityUtils helper'ı kullanarak giriş yapmış satıcının ID'sini güvenli bir şekilde al
        Long sellerId = SecurityUtils.getAuthenticatedSellerId(authentication);

        // OrderService'teki ilgili metodu çağırarak siparişleri al
        // Bu metodun OrderServiceImpl içinde implemente edildiğinden ve
        // OrderRepository'de findBySellerUserId metodunun olduğundan emin olun.
        List<DtoOrderResponse> orders = orderService.getOrdersForCurrentSeller(sellerId);

        // Başarılı yanıt olarak sipariş listesini ve HTTP 200 OK durumunu döndür
        return ResponseEntity.ok(orders);
    }

    // --- Buraya Satıcıya Özel Diğer Endpoint'ler Eklenebilir ---
    // Örneğin:
    // @GetMapping("/dashboard/stats")
    // public ResponseEntity<?> getSellerDashboardStats(Authentication authentication) { ... }
    //
    // @PutMapping("/profile")
    // public ResponseEntity<?> updateSellerProfile(Authentication authentication, @RequestBody DtoSellerProfile profile) { ... }
    // ---

}