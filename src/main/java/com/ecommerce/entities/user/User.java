package com.ecommerce.entities.user;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    protected Long userId;
    
    @Column(name = "username", unique = true, nullable = false, length = 50)
    protected String username;
    
    @Column(name = "email", unique = true, nullable = false, length = 100)
    protected String email;
    
    @Column(name = "password_hash", nullable = false, length = 60)  // For BCrypt hash
    protected String password;
    
    @Column(name = "first_name", length = 50)
    protected String firstName;
    
    @Column(name = "last_name", length = 50)
    protected String lastName;
    
    @Column(name = "phone_number", length = 20)
    protected String phoneNumber;

    @Column(name = "communication_preference")
    protected CommunicationChoice CommunicationPreference = CommunicationChoice.EMAIL;

    @Column(name = "sex")
    protected Sex sex = Sex.UNDEFINED;

    @Column(name = "date_of_birth")
    protected Date dateOfBirth;

    @Column(name = "role", nullable = false, length = 20)
    protected UserRole role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    protected List<Address> addresses = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    protected UserStatus status = UserStatus.ACTIVE;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    protected LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    protected LocalDateTime updatedAt;
    
    public enum UserStatus { 
        ACTIVE, 
        BANNED, 
        INACTIVE,
        PENDING_APPROVAL 
    }

    public enum UserRole {
        ADMIN,
        CUSTOMER,
        SELLER,
        LOGISTICS_PROVIDER
    }

    public enum CommunicationChoice {
        EMAIL,
        SMS,
        PHONE,
        BOTH
    }

    public enum Sex{
        MAN,
        WOMAN,
        UNDEFINED
    }
    
    //method to get role type (to be implemented by subclasses)
    public String getRoleType(){
        return role.name();
    }

    // Helper method to manage bidirectional relationship
    public void addAddress(Address address) {
        addresses.add(address);
        address.setUser(this); // Link Address to this User
    }

    // Custom setter to handle JSON deserialization
    public void setAddresses(List<Address> addresses) {
        this.addresses.clear();
        if (addresses != null) {
            addresses.forEach(this::addAddress);
        }
    }
}
