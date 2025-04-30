package com.ecommerce.services.impl.authandregisterservices;

import org.slf4j.Logger; // <<< ADDED: For logging
import org.slf4j.LoggerFactory; // <<< ADDED: For logging
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException; // <<< ADDED: For specific exception
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // <<< REMOVED: No longer creating instance here
import org.springframework.security.crypto.password.PasswordEncoder; // <<< ADDED: Injecting the bean
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException; // <<< ADDED: For broader exceptions
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // <<< Import Transactional

import com.ecommerce.entities.user.Admin;
import com.ecommerce.entities.user.Customer;
import com.ecommerce.entities.user.LogisticsProvider;
import com.ecommerce.entities.user.Seller;
import com.ecommerce.entities.user.User;
import com.ecommerce.repository.authandregisterrepo.RegistrationRepo;
import com.ecommerce.services.IRegistrationService;

// Removed unused import: jakarta.transaction.Transactional;

@Service
public class RegistrationService implements IRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class); // <<< ADDED: Logger

    @Autowired
    private RegistrationRepo registrationRepo;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    // +++ MODIFIED: Inject PasswordEncoder Bean +++
    @Autowired
    private PasswordEncoder passwordEncoder;
    // --- REMOVED: private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    // ++++++++++++++++++++++++++++++++++++++++++++

    @Override
    @Transactional // Ensures atomicity
    public Customer customerRegistration(Customer customer) {
        customer.setPassword(passwordEncoder.encode(customer.getPassword())); // Use injected encoder
        log.info("Registering new customer: {}", customer.getUsername()); // <<< ADDED: Logging
        return registrationRepo.save(customer);
    }

    @Override
    @Transactional // <<< ADDED Transactional
    public Admin adminRegistration(Admin admin) {
        admin.setPassword(passwordEncoder.encode(admin.getPassword())); // Use injected encoder
        log.info("Registering new admin: {}", admin.getUsername()); // <<< ADDED: Logging
        return registrationRepo.save(admin);
    }

    @Override
    @Transactional // <<< ADDED Transactional
    public LogisticsProvider logisticsProviderRegistration(LogisticsProvider logisticsProvider) {
        logisticsProvider.setPassword(passwordEncoder.encode(logisticsProvider.getPassword())); // Use injected encoder
        log.info("Registering new logistics provider: {}", logisticsProvider.getUsername()); // <<< ADDED: Logging
        return registrationRepo.save(logisticsProvider);
    }

    @Override
    @Transactional // <<< ADDED Transactional
    public Seller sellerRegistration(Seller seller) {
        seller.setPassword(passwordEncoder.encode(seller.getPassword())); // Use injected encoder
        log.info("Registering new seller: {}", seller.getUsername()); // <<< ADDED: Logging
        return registrationRepo.save(seller);
    }

    // --- MODIFIED: verify method with basic exception handling ---
    public String verify(User user) {
        log.debug("Attempting authentication for user: {}", user.getUsername()); // Use debug for potentially sensitive info
        try {
            Authentication authentication =
                    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));

            // Check authentication status explicitly (though authenticate() throws exception on failure)
            if (authentication.isAuthenticated()) {
                log.info("Authentication successful for user: {}", user.getUsername());
                return jwtService.generateToken(user.getUsername());
            } else {
                // This case might be rare if authenticate throws exceptions correctly
                log.warn("Authentication failed for user {} without exception.", user.getUsername());
                throw new BadCredentialsException("Login failed: Authentication object not authenticated.");
            }
        } catch (BadCredentialsException e) {
            log.warn("Authentication failed for user {}: Invalid credentials", user.getUsername());
            throw e; // Re-throw for potential global handling (e.g., @ControllerAdvice)
        } catch (AuthenticationException e) {
            log.error("Authentication failed for user {}: {}", user.getUsername(), e.getMessage());
            // Handle other authentication exceptions (locked account, disabled account etc.)
            // You might want to throw a more generic or specific custom exception here
            throw new RuntimeException("Login failed: " + e.getMessage(), e); // Wrap or re-throw
        } catch (Exception e) {
            // Catch unexpected errors during authentication
            log.error("Unexpected error during authentication for user {}: {}", user.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Login failed due to an unexpected error.", e);
        }
        // --- Removed: return "Login failed!"; (Now throws exceptions)
    }
    // --- End Modification ---
}
