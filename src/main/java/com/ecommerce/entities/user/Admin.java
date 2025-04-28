package com.ecommerce.entities.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "admins")
@PrimaryKeyJoinColumn(name = "user_id")
public class Admin extends User {
    
    @Column(name = "department", length = 50)
    private String department;
    
    @Column(name = "admin_level", length = 20)
    private String adminLevel;  // E.g., "SUPER_ADMIN", "PRODUCT_ADMIN", "SUPPORT_ADMIN"
    
    @Column(name = "can_manage_users")
    private boolean canManageUsers = false;
    
    @Column(name = "can_manage_products")
    private boolean canManageProducts = false;
    
    @Column(name = "can_manage_orders")
    private boolean canManageOrders = false;
    
    @Column(name = "can_manage_payments")
    private boolean canManagePayments = false;
    
    @Override
    public String getRoleType() {
        return "ADMIN";
    }
}
