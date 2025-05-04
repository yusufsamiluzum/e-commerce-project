package com.ecommerce.entities;

import com.ecommerce.entities.order.OrderItem;
import com.ecommerce.entities.user.Address;
import com.ecommerce.entities.user.Customer;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a request to return a specific item from an order.
 */
@Entity
@Table(name = "return_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_request_id")
    private Long returnRequestId;

    // Link to the specific order item being returned
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    // Link to the customer initiating the return
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private ReturnReason reason;

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments; // Optional customer comments

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReturnStatus status = ReturnStatus.PENDING; // Default status

    // Optional: Link to an address if pickup is required/different from original shipping
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pickup_address_id")
    private Address pickupAddress;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes; // Notes from admin/support handling the return

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Enum defining possible reasons for a product return.
     */
    public enum ReturnReason {
        DEFECTIVE_ITEM,
        WRONG_ITEM_RECEIVED,
        ITEM_NOT_AS_DESCRIBED,
        DAMAGED_IN_TRANSIT,
        NO_LONGER_NEEDED,
        ORDERED_BY_MISTAKE,
        BETTER_PRICE_AVAILABLE,
        OTHER
    }

    /**
     * Enum defining the status lifecycle of a return request.
     */
    public enum ReturnStatus {
        PENDING,            // Request submitted, awaiting review
        APPROVED,           // Return request approved
        REJECTED,           // Return request denied
        PROCESSING,         // Item received, processing refund/replacement
        COMPLETED,          // Refund/replacement issued
        CANCELLED           // Request cancelled by customer or system
    }
}
