package com.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.user.Address;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    // Custom query methods can be defined here if needed
    // For example, find addresses by user ID, etc.

}
