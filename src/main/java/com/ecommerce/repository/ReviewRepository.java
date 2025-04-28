package com.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.Review;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Custom query methods can be defined here if needed

}
