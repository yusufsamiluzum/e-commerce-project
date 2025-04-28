package com.ecommerce.entities;


import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.ecommerce.entities.order.Order;
import com.ecommerce.entities.user.LogisticsProvider;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long shipmentId;
    
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    @ManyToOne
    @JoinColumn(name = "logistics_provider_id")
    private LogisticsProvider logisticsProvider;
    
    private String trackingNumber;
    private String carrier;
    
    @Enumerated(EnumType.STRING)
    private ShipmentStatus status = ShipmentStatus.PROCESSING;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    public enum ShipmentStatus { 
        PROCESSING, 
        PICKED_UP, 
        IN_TRANSIT,
        OUT_FOR_DELIVERY, 
        DELIVERED, 
        RETURNED,
        FAILED_DELIVERY 
    }
}
