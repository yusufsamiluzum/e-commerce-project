package com.ecommerce.entities.user;

import java.util.ArrayList;
import java.util.List;

import com.ecommerce.entities.order.Order;
import com.ecommerce.entities.product.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sellers")
@PrimaryKeyJoinColumn(name = "user_id")
public class Seller extends User {
    
    @Column(name = "company_name", length = 100)
    private String companyName;
    
    @Column(name = "business_registration_number", length = 50)
    private String businessRegistrationNumber;
    
    @Column(name = "tax_identification_number", length = 50)
    private String taxIdentificationNumber;
    
    @Column(name = "is_verified")
    private boolean isVerified = false;
    
    @Column(name = "payment_account_id", length = 100)
    private String paymentAccountId;  // For receiving payments
    
    @OneToMany(mappedBy = "seller")
    @ToString.Exclude
    private List<Product> products = new ArrayList<>();
    
    @OneToMany(mappedBy = "seller")
    @ToString.Exclude
    private List<Order> receivedOrders = new ArrayList<>();
    
    @Override
    public String getRoleType() {
        return "SELLER";
    }
}
