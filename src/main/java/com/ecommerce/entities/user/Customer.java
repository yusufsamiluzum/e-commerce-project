package com.ecommerce.entities.user;

import java.util.ArrayList;
import java.util.List;

import com.ecommerce.entities.Review;
import com.ecommerce.entities.cart.Cart;
import com.ecommerce.entities.order.Order;
import com.ecommerce.entities.product.ProductComparison;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "customers")
@PrimaryKeyJoinColumn(name = "user_id")
public class Customer extends User {
    
    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    private Cart cart; 
    
    @OneToMany(mappedBy = "customer")
    private List<Order> orders = new ArrayList<>();
    
    @OneToMany(mappedBy = "customer")
    private List<Review> reviews = new ArrayList<>();
    
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<ProductComparison> comparisons = new ArrayList<>();
    
    @Override
    public String getRoleType() {
        return "CUSTOMER";
    }

    @Override
    public String toString() {
        return "Customer{" +
                // Exclude lazy-loaded fields like 'orders'
                "id=" + userId +
                ", username='" + username + '\'' +
                // ... other non-lazy fields
                '}';
    }
}
