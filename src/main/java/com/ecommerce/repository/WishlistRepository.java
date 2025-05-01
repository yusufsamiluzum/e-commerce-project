package com.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.Wishlist;
import com.ecommerce.entities.user.User;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    
    /**
     * Finds a wishlist by the associated user.
     *
     * @param user The user entity.
     * @return An Optional containing the Wishlist if found, otherwise empty.
     */
    Optional<Wishlist> findByUser(User user);

    /**
     * Finds a wishlist by the user's ID by traversing the relationship.
     * Spring Data JPA translates this to a query like:
     * SELECT w FROM Wishlist w WHERE w.user.userId = :userId
     *
     * @param userId The ID of the user associated with the wishlist.
     * @return An Optional containing the Wishlist if found, otherwise empty.
     */
    Optional<Wishlist> findByUserUserId(Long userId);
}
