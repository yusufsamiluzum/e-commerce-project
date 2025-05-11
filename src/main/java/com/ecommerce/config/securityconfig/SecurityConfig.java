package com.ecommerce.config.securityconfig;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
// Removed unused import: import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties.Jwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
// Removed unused import: import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // <<< ADDED for @PreAuthorize
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder; // <<< Import PasswordEncoder
// Removed unused import: import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;


// Removed unused import: import com.ecommerce.entities.user.User;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // <<< ADDED: Enable method-level security for @PreAuthorize in UserController
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService; // Remains the same (AuthenticationService)

    @Autowired
    private JwtFilter jwtFilter;

    // +++ ADDED: Define PasswordEncoder as a Bean +++
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 12 is a good default
        return new BCryptPasswordEncoder(12);
    }
    // ++++++++++++++++++++++++++++++++++++++++++++++

    // Define the paths for Swagger/OpenAPI documentation
    private static final String[] SWAGGER_WHITELIST = {
        "/v3/api-docs/**",          // OpenAPI specification
        "/swagger-ui/**",           // Swagger UI webjar resources
        "/swagger-ui.html"          // Swagger UI HTML page
        // Add any other custom paths if you changed the defaults
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(request -> request
                .requestMatchers(SWAGGER_WHITELIST).permitAll()
                .requestMatchers(
                    "/rest/api/registration/customer",
                    "/rest/api/registration/admin",
                    "/rest/api/registration/logisticsProvider",
                    "/rest/api/registration/seller",
                    "/rest/api/registration/login"
                ).permitAll()
                
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS preflight için

                // Ürünleri ve kategorileri GET ile herkes görebilsin
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**").permitAll()
                
                .requestMatchers(HttpMethod.GET, "/product-images/**").permitAll()

                // Yorum EKLEME (POST) sadece MÜŞTERİ rolüyle yapılabilsin
                .requestMatchers(HttpMethod.POST, "/api/v1/products/{productId:[0-9]+}/reviews").hasRole("CUSTOMER")
                // Yorum GÜNCELLEME (PUT) kimliği doğrulanmış kullanıcılar (sahibi mi kontrolü @PreAuthorize ile controller'da)
                .requestMatchers(HttpMethod.PUT, "/api/v1/reviews/{reviewId:[0-9]+}").authenticated()
                // Yorum SİLME (DELETE) kimliği doğrulanmış kullanıcılar (sahibi mi/admin mi kontrolü @PreAuthorize ile controller'da)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/{reviewId:[0-9]+}").authenticated()

                // Kullanıcı ile ilgili genel endpoint'ler kimlik doğrulaması gerektirsin
                .requestMatchers("/api/users/**").authenticated()

                // Diğer /api/v1/ altındaki (yukarıda belirtilmeyen) tüm endpoint'ler kimlik doğrulaması gerektirsin
                .requestMatchers("/api/v1/**").authenticated()

                // Diğer tüm istekler kimlik doğrulaması gerektirsin
                .anyRequest().authenticated()
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // --- MODIFIED: Pass the AuthenticationProvider Bean ---
            .authenticationProvider(authenticationProvider()) // Add the configured provider
            // --- End Modification ---
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    /* Commented out userDetailsService bean remains the same */

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")); // Added OPTIONS/PATCH
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // +++ MODIFIED: Inject the PasswordEncoder bean +++
        provider.setPasswordEncoder(passwordEncoder()); // Use the bean
        // ++++++++++++++++++++++++++++++++++++++++++++++
        provider.setUserDetailsService(userDetailsService); // UserDetailsService remains injected
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
