package com.ecommerce.repository.authandregisterrepo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.user.User;

@Repository
public interface AuthenticationRepo extends JpaRepository<User, Long> {
    
    User findByUsername(String username); // Custom query method to find user by username

}
