package com.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.user.Address;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
     /**
     * Finds an address by its ID and ensures it belongs to the specified user ID.
     * This relies on the Address entity having a 'user' field mapped to the User entity,
     * and the User entity having a 'userId' field.
     * Spring Data JPA automatically creates the query based on the method name.
     *
     * @param addressId The ID of the address to find.
     * @param userId The ID of the user the address must belong to.
     * @return An Optional containing the Address if found and owned by the user, otherwise empty.
     */
    Optional<Address> findByAddressIdAndUserUserId(Long addressId, Long userId);

    // You might also have:
    List<Address> findByUserUserId(Long userId); // Find all addresses for a user
}