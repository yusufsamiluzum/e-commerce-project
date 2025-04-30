package com.ecommerce.config.securityconfig;

import java.io.IOException;

// import org.springframework.context.ApplicationContext; // <<< REMOVED
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService; // <<< ADDED
import org.springframework.security.core.userdetails.UsernameNotFoundException; // <<< ADDED for exception handling
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger; // <<< ADDED: Logging
import org.slf4j.LoggerFactory; // <<< ADDED: Logging

// Removed unused import: import com.ecommerce.services.impl.authandregisterservices.AuthenticationService;
import com.ecommerce.services.impl.authandregisterservices.JWTService;

import io.jsonwebtoken.ExpiredJwtException; // <<< ADDED: Specific exception
import io.jsonwebtoken.MalformedJwtException; // <<< ADDED: Specific exception

import io.jsonwebtoken.UnsupportedJwtException; // <<< ADDED: Specific exception

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter{

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class); // <<< ADDED: Logger

    @Autowired
    private JWTService jwtService;

    // +++ MODIFIED: Inject UserDetailsService directly +++
    @Autowired
    private UserDetailsService userDetailsService; // Use the interface
    // --- REMOVED: @Autowired private ApplicationContext context;
    // ++++++++++++++++++++++++++++++++++++++++++++++++

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                username = jwtService.extractUserName(token);
                log.debug("Extracted username '{}' from token", username);
            } catch (ExpiredJwtException e) {
                log.warn("JWT Token expired: {}", e.getMessage());
                // Optionally set response status or let Spring Security handle downstream
            // Inside doFilterInternal method's try-catch for extractUserName
            } catch (UnsupportedJwtException | MalformedJwtException | io.jsonwebtoken.security.SecurityException | IllegalArgumentException e) { // <-- Replaced SignatureException
                log.warn("Invalid JWT Token (parse error or security issue): {}", e.getMessage());
            } catch (Exception e) {
                log.error("Error extracting username from token", e);
            }
        } else {
            log.trace("No Authorization Bearer header found");
        }

        // Only process if username extracted and no existing authentication in context
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = null;
            try {
                // +++ MODIFIED: Use injected userDetailsService +++
                userDetails = this.userDetailsService.loadUserByUsername(username);
                // ++++++++++++++++++++++++++++++++++++++++++++++++

                if (jwtService.validateToken(token, userDetails)) {
                    log.debug("JWT token validated successfully for user '{}'", username);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // Set authentication in the context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authentication set in SecurityContext for user '{}'", username);
                } else {
                    log.warn("JWT token validation failed for user '{}'", username);
                }
            } catch (UsernameNotFoundException e) {
                log.warn("User '{}' not found based on token subject.", username);
            } catch (Exception e) {
                 log.error("Error during token validation or UserDetailsService lookup for user '{}'", username, e);
            }
        } else {
             if (username == null) {
                log.trace("Username could not be extracted from token or no token provided.");
             }
             if (SecurityContextHolder.getContext().getAuthentication() != null) {
                 log.trace("SecurityContext already contains Authentication for this request.");
             }
        }

        // Continue the filter chain regardless of authentication outcome
        filterChain.doFilter(request, response);
    }
}
