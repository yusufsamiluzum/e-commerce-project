package com.ecommerce.dto;

import java.time.LocalDateTime;

import com.ecommerce.entities.Shipment.ShipmentStatus;

import lombok.Data;

@Data
public class DtoShipmentSummary {
    private Long shipmentId;
    private String trackingNumber;
    private String carrier; // e.g., FedEx, UPS
    private ShipmentStatus status;
    private LocalDateTime estimatedDeliveryDate; // Optional
    private LocalDateTime shippedDate; // Optional
}

  

