package com.ecommerce.entities.user;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long addressId;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
    
    @Column(name = "street_address", nullable = false, length = 255)
    private String streetAddress;
    
    @Column(name = "city", nullable = false, length = 100)
    private String city;
    
    @Column(name = "state", length = 100)
    private String state;
    
    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;
    
    @Column(name = "country", nullable = false, length = 100)
    private String country;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    // In Address.java
    @Column(name = "is_default")
    @JsonProperty("isDefault")
    private boolean isDefault;

    @Column(name = "is_billing")
    @JsonProperty("isBilling")
    private boolean isBilling;

    @Column(name = "is_shipping")
    @JsonProperty("isShipping")
    private boolean isShipping;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
