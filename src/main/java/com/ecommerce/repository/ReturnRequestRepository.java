package com.ecommerce.repository; // Assuming repositories are here


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.entities.ReturnRequest;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ReturnRequest entities.
 */
@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    /**
     * Finds all return requests initiated by a specific customer.
     *
     * @param customerId The ID of the customer.
     * @return A list of return requests for the given customer.
     */
    List<ReturnRequest> findByCustomerId(Long customerId);

    /**
     * Finds all return requests associated with a specific order item.
     * (Typically there should only be one, but this allows finding it).
     *
     * @param orderItemId The ID of the order item.
     * @return A list of return requests for the given order item.
     */
    List<ReturnRequest> findByOrderItemId(Long orderItemId);

     /**
     * Finds a return request by its ID and customer ID for authorization checks.
     *
     * @param returnRequestId The ID of the return request.
     * @param customerId The ID of the customer.
     * @return An Optional containing the return request if found and belongs to the customer.
     */
    Optional<ReturnRequest> findByReturnRequestIdAndCustomerId(Long returnRequestId, Long customerId);

    // Add more custom query methods as needed (e.g., find by status, date range)
}