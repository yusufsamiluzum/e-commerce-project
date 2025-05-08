package com.ecommerce.services.security; // Paket adını kendi yapınıza göre ayarlayın

import com.ecommerce.config.securityconfig.UserPrincipal;
import com.ecommerce.entities.order.Order;
import com.ecommerce.repository.OrderRepository;
import lombok.RequiredArgsConstructor; // Eğer Lombok kullanıyorsanız
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Okuma işlemi için

@Service("orderSecurityService") // Spring'in bu sınıfı bulması ve @PreAuthorize'da kullanılması için bean adı
@RequiredArgsConstructor // OrderRepository'yi constructor ile inject etmek için (veya @Autowired kullanın)
public class OrderSecurityService {

    private final OrderRepository orderRepository; // Gerekli repository

    /**
     * Belirtilen siparişin, o anki kimliği doğrulanmış kullanıcıya ait olup olmadığını kontrol eder.
     * @param currentUser Kimliği doğrulanmış kullanıcı bilgileri (UserPrincipal)
     * @param orderId Kontrol edilecek siparişin ID'si
     * @return Sipariş kullanıcıya aitse true, değilse false
     */
    @Transactional(readOnly = true) // Veritabanından sadece okuma yapılacağı için performansı artırabilir
    public boolean isOrderOwner(UserPrincipal currentUser, Long orderId) {
        // Gerekli parametrelerin null olup olmadığını kontrol et
        if (currentUser == null || currentUser.getUser() == null || orderId == null) {
            System.err.println("OrderSecurityService: Invalid input parameters (currentUser or orderId is null)."); // Hata ayıklama için log
            return false;
        }

        // Siparişi veritabanından bul
        Order order = orderRepository.findById(orderId).orElse(null);

        // Sipariş bulundu mu ve siparişin bir müşterisi var mı diye kontrol et
        if (order == null || order.getCustomer() == null) {
            System.err.println("OrderSecurityService: Order not found or has no associated customer for orderId: " + orderId); // Hata ayıklama
            return false;
        }

        // Siparişin müşteri ID'sini al
        Long orderCustomerId = order.getCustomer().getUserId();
        // Kimliği doğrulanmış kullanıcının ID'sini al (UserPrincipal içindeki User'dan)
        Long authenticatedUserId = currentUser.getUser().getUserId();

        // ID'leri karşılaştır ve sonucu logla (Hata ayıklama için)
        boolean isOwner = orderCustomerId != null && orderCustomerId.equals(authenticatedUserId);
        System.out.println("OrderSecurityService: Checking ownership for orderId=" + orderId +
                           ", orderCustomerId=" + orderCustomerId +
                           ", authenticatedUserId=" + authenticatedUserId +
                           ", isOwner=" + isOwner);

        return isOwner;
    }
}