package com.ecommerce.services.impl.authandregisterservices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.ecommerce.entities.user.Admin;
import com.ecommerce.entities.user.Customer;
import com.ecommerce.entities.user.LogisticsProvider;
import com.ecommerce.entities.user.Seller;
import com.ecommerce.entities.user.User;
import com.ecommerce.repository.authandregisterrepo.RegistrationRepo;
import com.ecommerce.services.IRegistrationService;

import jakarta.transaction.Transactional;

@Service
public class RegistrationService implements IRegistrationService {

    @Autowired
    private RegistrationRepo registrationRepo;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    @Transactional // Ensures atomicity
    public Customer customerRegistration(Customer customer) {
        customer.setPassword(encoder.encode(customer.getPassword()));

        return registrationRepo.save(customer);
    }

    @Override
    public Admin adminRegistration(Admin admin) {
        admin.setPassword(encoder.encode(admin.getPassword()));
       return registrationRepo.save(admin);
    }

    @Override
    public LogisticsProvider logisticsProviderRegistration(LogisticsProvider logisticsProvider) {
        logisticsProvider.setPassword(encoder.encode(logisticsProvider.getPassword()));
        return registrationRepo.save(logisticsProvider);
    }

    @Override
    public Seller sellerRegistration(Seller seller) {
        seller.setPassword(encoder.encode(seller.getPassword()));
        return registrationRepo.save(seller);
    }

    public String verify(User user){
        System.out.println("Verifying role: " + user.getRole());
        Authentication authentication =
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        if(authentication.isAuthenticated()){
            return jwtService.generateToken(user.getUsername());
        } 
            
        return "Login failed!";   
    }
}
