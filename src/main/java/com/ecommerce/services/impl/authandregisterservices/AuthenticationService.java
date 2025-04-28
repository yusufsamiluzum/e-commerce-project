package com.ecommerce.services.impl.authandregisterservices;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ecommerce.config.securityconfig.UserPrincipal;
import com.ecommerce.entities.user.User;
import com.ecommerce.repository.authandregisterrepo.AuthenticationRepo;

@Service
public class AuthenticationService implements UserDetailsService {
    
    @Autowired
    public AuthenticationRepo authenticationRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        
        User user = authenticationRepo.findByUsername(username);
        System.out.println("Loaded user: " + user);
        if(user == null){
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        System.out.println("Loaded user role: " + user.getRoleType());
        return new UserPrincipal(user);
    }


    
}
