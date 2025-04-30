package com.ecommerce.dto;

import java.time.LocalDateTime;

import com.ecommerce.entities.Shipment.ShipmentStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoShipment { // Simplified Shipment info for Order DTO
    private Long shipmentId;
    private String trackingNumber;
    private String carrier;
    private ShipmentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Maybe include LogisticsProvider name/summary?
    // private String logisticsProviderName;
}
