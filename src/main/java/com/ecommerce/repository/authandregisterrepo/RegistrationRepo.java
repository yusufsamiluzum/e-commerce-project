package com.ecommerce.repository.authandregisterrepo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.user.User;

@Repository
public interface RegistrationRepo extends JpaRepository<User, Long> {
    // Define any custom query methods if needed
    // For example: List<RegistrationRepo> findByStatus(String status);

}
