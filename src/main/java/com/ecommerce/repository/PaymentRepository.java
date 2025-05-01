package com.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    // Add this method: Finds Payment by the orderId of the associated Order object
    Optional<Payment> findByOrderOrderId(Long orderId);
    // Or if you expect multiple payments per order (less likely for OneToOne):
    // List<Payment> findByOrderOrderId(Long orderId);

}
