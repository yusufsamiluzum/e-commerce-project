package com.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.user.Seller;

@Repository
public interface SellerRepository extends JpaRepository<Seller, Long> {
    // Custom query methods can be defined here if needed
    // For example, find by email or username, etc.

}
