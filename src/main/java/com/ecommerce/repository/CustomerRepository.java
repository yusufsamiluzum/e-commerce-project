package com.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.entities.user.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // Custom query methods can be defined here if needed

}
