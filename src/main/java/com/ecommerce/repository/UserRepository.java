package com.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.dto.DtoProfile;
import com.ecommerce.entities.user.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<DtoProfile> findByUsername(String username);

    
    // Custom query methods can be defined here if needed

}
