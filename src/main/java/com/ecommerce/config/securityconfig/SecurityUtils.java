package com.ecommerce.config.securityconfig;



import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;


import java.util.Objects;

/**
 * Utility class for Spring Security related operations.
 */
public final class SecurityUtils {

    // Private constructor to prevent instantiation
    private SecurityUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Extracts the Seller ID (or general User ID if applicable) from the
     * authenticated principal stored in the Authentication object.
     *
     * Adapt this method based on your specific Spring Security configuration
     * (e.g., custom UserDetails, JWT claims).
     *
     * @param authentication The Authentication object from the SecurityContext.
     * @return The authenticated user's ID (as Long).
     * @throws AccessDeniedException if the user is not authenticated.
     * @throws IllegalStateException if the user ID cannot be extracted from the principal.
     */
    public static Long getAuthenticatedSellerId(Authentication authentication) {
        // 1. Basic Check: Ensure authentication object exists and is authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            // This shouldn't typically happen if @PreAuthorize is working, but good practice to check.
            throw new AccessDeniedException("Authentication required.");
        }

        // 2. Get the Principal: This object holds the user's identity details.
        Object principal = authentication.getPrincipal();
        Objects.requireNonNull(principal, "Authentication principal cannot be null");

        // 3. Extract ID based on Principal Type:
        //    Choose the block that matches your Spring Security configuration.

        // --- Option A: Using a Custom UserDetails Implementation ---
        // This is the recommended approach if you have a UserDetailsService
        // that loads your specific user details (including the ID).
        if (principal instanceof UserPrincipal) { // *** Replace CustomUserDetails with your actual class name ***
            UserPrincipal userDetails = (UserPrincipal) principal;
            // Assuming your CustomUserDetails class has a method like getId() or getUserId() or getSellerId()
            Long userId = userDetails.getUser().getUserId(); // *** Adjust method name as needed ***
            if (userId == null) {
                 throw new IllegalStateException("User ID not found in CustomUserDetails principal.");
            }
            // If you specifically need a Seller ID and your CustomUserDetails holds different user types,
            // you might need additional checks here (e.g., check roles or a user type field).
            // For now, we assume getId() returns the relevant ID for the logged-in user (who is a Seller in this context).
            return userId;
        }

        // --- Option B: Using JWT Claims ---
        // Use this if your Authentication principal is directly a Jwt object
        // (e.g., when using Spring Security's built-in JWT resource server support).
        else if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            // Extract the ID from a specific claim in the JWT token.
            // Common claim names could be "sub" (subject), "userId", "sellerId", "sid", etc.
            // *** Adjust the claim name ("sid" used here as an example) ***
            String claimName = "sid"; // Or "userId", "sub", etc.

            Object idClaim = jwt.getClaim(claimName);
             if (idClaim == null) {
                 throw new IllegalStateException("Required claim ('" + claimName + "') not found in JWT token.");
             }

             // Handle potential types for the claim (Number or String)
             if (idClaim instanceof Number) {
                 return ((Number) idClaim).longValue();
             } else if (idClaim instanceof String) {
                 try {
                     return Long.parseLong((String) idClaim);
                 } catch (NumberFormatException e) {
                     throw new IllegalStateException("Invalid ID format in JWT claim '" + claimName + "': " + idClaim);
                 }
             } else {
                  throw new IllegalStateException("Unsupported type for JWT claim '" + claimName + "': " + idClaim.getClass().getName());
             }
        }

        // --- Option C: Fallback for Standard UserDetails (Less Ideal) ---
        // If the principal is the standard UserDetails, it usually only guarantees the username.
        else if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            String username = userDetails.getUsername();
            // PROBLEM: Standard UserDetails doesn't typically hold the database ID.
            // You would need to perform an extra database lookup here using the username
            // to find the corresponding Seller ID. This is generally inefficient and couples
            // this utility class with repositories.
            // It's much better to use a CustomUserDetails implementation (Option A).
            throw new IllegalStateException("Cannot reliably extract Seller ID from standard UserDetails. Implement a custom UserDetails class or use JWT claims containing the ID.");
            // Example (if you absolutely must, but avoid):
            // SellerRepository sellerRepository = ... // How to get repository here? Bad practice.
            // Seller seller = sellerRepository.findByUsername(username).orElseThrow(...);
            // return seller.getSellerId();
        }

        // --- Option D: Fallback for String Principal (Less Ideal) ---
        else if (principal instanceof String) {
            // Sometimes, the principal might be configured to be just the username String.
            String username = (String) principal;
            // Same problem as Option C: Need an extra lookup based on username. Avoid.
             throw new IllegalStateException("Cannot reliably extract Seller ID from String principal. Implement a custom UserDetails class or use JWT claims containing the ID.");
        }


        // --- If none of the above match ---
        throw new IllegalStateException("Cannot determine authenticated user ID from principal type: " + principal.getClass().getName());
    }

    // --- Placeholder for your CustomUserDetails implementation ---
    /*
    package com.ecommerce.security; // Or your security package

    import org.springframework.security.core.GrantedAuthority;
    import org.springframework.security.core.userdetails.UserDetails;
    import java.util.Collection;

    // Example structure - Adapt to your needs!
    public class CustomUserDetails implements UserDetails {

        private Long id; // The database ID (User ID, Seller ID, etc.)
        private String username;
        private String password;
        private Collection<? extends GrantedAuthority> authorities;
        private boolean accountNonExpired;
        private boolean accountNonLocked;
        private boolean credentialsNonExpired;
        private boolean enabled;
        // Add other user fields if needed (e.g., email, userType)

        // Constructor(s) to populate these fields (likely from your User/Seller entity)

        public Long getId() {
            return id;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return accountNonExpired;
        }

        @Override
        public boolean isAccountNonLocked() {
            return accountNonLocked;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return credentialsNonExpired;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }
    }
    */
}

