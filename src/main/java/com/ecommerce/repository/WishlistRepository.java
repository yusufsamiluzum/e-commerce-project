package com.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.Wishlist;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    // Custom query methods can be defined here if needed

}
