package com.ecommerce.dto; // Assuming DTOs are in this package


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

import com.ecommerce.entities.ReturnRequest.ReturnReason;
import com.ecommerce.entities.ReturnRequest.ReturnStatus;

/**
 * Data Transfer Object for ReturnRequest entity.
 * Used for creating and viewing return requests via API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoReturnRequest {

    private Long returnRequestId;
    private Long orderItemId; // ID of the item being returned
    private Long customerId; // ID of the customer making the request
    private ReturnReason reason;
    private String comments;
    private ReturnStatus status;
    private Long pickupAddressId; // Optional: ID of the pickup address
    private String resolutionNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Optional: Include summary DTOs for related entities if needed for display
    private DtoProductSummary productSummary; // Summary of the product being returned
    private DtoUserSummary customerSummary; // Summary of the customer

    // Constructor for creating a request (input DTO)
    public DtoReturnRequest(Long orderItemId, Long customerId, ReturnReason reason, String comments, Long pickupAddressId) {
        this.orderItemId = orderItemId;
        this.customerId = customerId;
        this.reason = reason;
        this.comments = comments;
        this.pickupAddressId = pickupAddressId;
        // Status defaults to PENDING in the entity or service logic
    }

    // You might need more constructors or static factory methods depending on use cases
}
