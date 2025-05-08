package com.ecommerce.services.impl.authandregisterservices;

import java.security.NoSuchAlgorithmException; // Keep for potential future use? Maybe remove.
import java.util.Base64; // Keep for potential future use? Maybe remove.
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// Removed unused import: import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value; // <<< ADDED: For injecting properties
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.ecommerce.config.securityconfig.UserPrincipal;
import com.ecommerce.entities.user.User;

import org.slf4j.Logger; // <<< ADDED: Logging
import org.slf4j.LoggerFactory; // <<< ADDED: Logging

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException; // <<< ADDED: Specific exception
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException; // <<< ADDED: Specific exception
import io.jsonwebtoken.UnsupportedJwtException; // <<< ADDED: Specific exception
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException; // <<< ADDED: Specific exception
import jakarta.annotation.PostConstruct; // <<< ADDED: For key initialization

@Service
public class JWTService {

    private static final Logger log = LoggerFactory.getLogger(JWTService.class); // <<< ADDED: Logger

    // +++ MODIFIED: Inject secret key from properties +++
    @Value("${jwt.secret.key}") // Example property name: jwt.secret.key
    private String secretKeyString; // Store the Base64 encoded string from properties

    private SecretKey secretKey; // The actual SecretKey object used for signing/verification

    // Initialize the SecretKey after properties are injected
    @PostConstruct
    public void init() {
        if (secretKeyString == null || secretKeyString.isEmpty()) {
            log.error("JWT secret key is not configured properly in application properties (e.g., jwt.secret.key)");
            throw new IllegalStateException("JWT secret key is not configured.");
        }
        try {
             byte[] keyBytes = Decoders.BASE64.decode(secretKeyString);
             this.secretKey = Keys.hmacShaKeyFor(keyBytes);
             log.info("JWTService initialized with configured secret key.");
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 encoding for JWT secret key provided in properties.", e);
            throw new IllegalStateException("Invalid JWT secret key configuration.", e);
        }
    }

    // --- REMOVED: Dynamic key generation in constructor ---
    /*
    public JWTService(){
        try{
            KeyGenerator keyGen = KeyGenerator.getInstance("HmacSHA256");
            SecretKey sk = keyGen.generateKey();
            secretKeyString = Base64.getEncoder().encodeToString(sk.getEncoded());
        }catch(NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }
    }
    */
    // ++++++++++++++++++++++++++++++++++++++++++++++++++++

 // JWTService.java içinde (generateToken metodu)
    public String generateToken(UserDetails userDetails) { // Parametre UserDetails oldu
        Map<String, Object> claims = new HashMap<>();

        // UserDetails'in sizin UserPrincipal implementasyonunuz olduğunu varsayalım
        if (userDetails instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) userDetails;
            User user = userPrincipal.getUser(); // User nesnesini al

            // Gerekli claim'leri ekle
            claims.put("userId", user.getUserId());
            claims.put("email", user.getEmail());
            claims.put("firstName", user.getFirstName());
            claims.put("lastName", user.getLastName());

            // Rolleri authorities olarak ekle (Angular tarafı bunu bekliyor gibi)
            List<String> authorities = userDetails.getAuthorities().stream()
                                           .map(GrantedAuthority::getAuthority)
                                           .collect(Collectors.toList());
            claims.put("authorities", authorities); // Veya "role" key'i ile tek rol

            // ÖNEMLİ: Frontend'deki decodeToken fonksiyonunun beklediği
            // claim isimleriyle (userId, authorities/role vb.) eşleştiğinden emin olun.

        } else {
             // Hata yönetimi veya loglama
             log.error("UserDetails is not an instance of UserPrincipal. Cannot add custom claims.");
             // Veya sadece temel claim'leri ekleyerek devam et
        }


        // Kalan token oluşturma kodları (expiration vb.)
        long expirationTimeMillis = 30 * 60 * 1000; // 10 dakika
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationTimeMillis);

        return Jwts.builder()
                .claims(claims) // Güncellenmiş claim map'ini kullan
                .subject(userDetails.getUsername()) // Subject hala username olabilir
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSecretKey())
                .compact();
    }

    // --- Renamed: getKey() to getSecretKey() for clarity ---
    private SecretKey getSecretKey() {
         if (this.secretKey == null) {
             // This should ideally not happen if @PostConstruct worked, but as a safeguard:
             log.error("JWT SecretKey was not initialized!");
             throw new IllegalStateException("JWT Secret Key not available.");
         }
        return this.secretKey;
    }
    // --- End Rename ---

    public String extractUserName(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            log.warn("Error extracting username from JWT: {}", e.getMessage());
            return null; // Or throw a specific exception
        }
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimResolver) {
        final Claims claims = extractAllClaims(token);
        return claimResolver.apply(claims);
    }

    // --- MODIFIED: Added more specific exception handling ---
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey()) // Use the initialized SecretKey object
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("JWT token has expired: {}", e.getMessage());
            throw e; // Re-throw specific exception
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty or invalid: {}", e.getMessage());
            throw e;
        }
    }
    // --- End Modification ---

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String userName = extractUserName(token);
            // Check username equality and token expiration
            boolean isValid = userName != null && userName.equals(userDetails.getUsername()) && !isTokenExpired(token);
            if (!isValid) {
                log.debug("Token validation failed for user '{}'. Username match: {}, Expired: {}",
                         userDetails.getUsername(),
                         userName != null && userName.equals(userDetails.getUsername()),
                         isTokenExpired(token));
            }
            return isValid;
        } catch (ExpiredJwtException | UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
             // Exceptions during extraction mean the token is invalid
             log.warn("Token validation failed due to exception: {}", e.getMessage());
             return false;
         }
    }

    private boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
             return true; // If parsing throws ExpiredJwtException, it is indeed expired
         } catch (Exception e) {
            // Other parsing errors mean we can't determine expiration, treat as invalid/expired
            log.warn("Could not determine token expiration due to error: {}", e.getMessage());
            return true;
        }
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

}
