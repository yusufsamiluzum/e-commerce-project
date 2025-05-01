package com.ecommerce.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.ecommerce.entities.order.Order.OrderStatus;

import lombok.Data;
@Data
public class DtoOrderResponse {
    
    private Long orderId;
    private String orderNumber;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private DtoAddress shippingAddress;
    private DtoAddress billingAddress;
    private List<DtoOrderItem> items;
    private DtoPaymentSummary payment; // Embed payment summary
    private List<DtoShipmentSummary> shipments; // Embed shipment summaries
    private DtoUserSummary customer; // Include summary of the customer
    // private DtoUserSummary seller; // Only needed if relevant (e.g., marketplace context)

}
