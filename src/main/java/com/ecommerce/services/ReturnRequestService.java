package com.ecommerce.services;

import java.util.List;

import com.ecommerce.dto.DtoReturnRequest;
import com.ecommerce.entities.ReturnRequest.ReturnStatus;

/**
 * Service interface for managing product return requests.
 */
public interface ReturnRequestService {
    DtoReturnRequest createReturnRequest(DtoReturnRequest returnRequestDto);
    DtoReturnRequest getReturnRequestById(Long returnRequestId);
    List<DtoReturnRequest> getReturnRequestsByCustomerId(Long customerId);
    DtoReturnRequest updateReturnStatus(Long returnRequestId, ReturnStatus status, String resolutionNotes);
    // Add other methods as needed (e.g., get by order item)
}