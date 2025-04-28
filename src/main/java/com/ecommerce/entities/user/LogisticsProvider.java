package com.ecommerce.entities.user;

import java.util.ArrayList;
import java.util.List;

import com.ecommerce.entities.Shipment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
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
@Table(name = "logistics_providers")
@PrimaryKeyJoinColumn(name = "user_id")
public class LogisticsProvider extends User {
    
    @Column(name = "company_name", length = 100)
    private String companyName;
    
    @Column(name = "service_area", columnDefinition = "TEXT")
    private String serviceArea;  // JSON array of covered postal codes or regions
    
    @Column(name = "tracking_url_pattern", length = 255)
    private String trackingUrlPattern;  // URL pattern with '{trackingNumber}' placeholder
    
    @Column(name = "is_verified")
    private boolean isVerified = false;
    
    @OneToMany(mappedBy = "logisticsProvider")
    private List<Shipment> shipments = new ArrayList<>();
    
    @Override
    public String getRoleType() {
        return "LOGISTICS_PROVIDER";
    }
}
