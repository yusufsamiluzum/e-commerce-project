package com.ecommerce.controller.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.config.securityconfig.SecurityUtils;
import com.ecommerce.config.securityconfig.UserPrincipal;
import com.ecommerce.dto.DtoReturnRequest;
import com.ecommerce.entities.ReturnRequest.ReturnStatus;
import com.ecommerce.services.ReturnRequestService;

import jakarta.validation.Valid;

/**
 * REST Controller for managing product return requests.
 * Includes security checks based on user roles and ownership.
 */
@RestController
@RequestMapping("/api/v1/returns") // Base path for return requests
public class ReturnRequestController {

    @Autowired
    private ReturnRequestService returnRequestService;

    /**
     * Creates a new product return request.
     * Only accessible by authenticated users with the CUSTOMER role.
     * The customerId in the DTO must match the logged-in user.
     *
     * @param returnRequestDto DTO containing details for the new return request.
     * @param authentication   The authentication object for the current user.
     * @return ResponseEntity containing the created DtoReturnRequest and HTTP status 201 (Created).
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')") // Only customers can create return requests
    public ResponseEntity<DtoReturnRequest> createReturnRequest(
            @Valid @RequestBody DtoReturnRequest returnRequestDto,
            Authentication authentication) {

        // Get the authenticated user's ID using the correct method name
        Long loggedInCustomerId = SecurityUtils.getAuthenticatedSellerId(authentication); // <<< CORRECTED method name

        // Security Check: Ensure the customerId in the DTO matches the logged-in user
        if (!Objects.equals(returnRequestDto.getCustomerId(), loggedInCustomerId)) {
             throw new AccessDeniedException("You can only create return requests for yourself.");
             // Or return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        DtoReturnRequest createdRequest = returnRequestService.createReturnRequest(returnRequestDto);
        return new ResponseEntity<>(createdRequest, HttpStatus.CREATED);
    }

    /**
     * Gets a specific return request by its ID.
     * Accessible only by the customer who created it or an admin.
     *
     * @param id             The ID of the return request.
     * @param authentication The authentication object for the current user.
     * @return ResponseEntity containing the DtoReturnRequest or 404 if not found / 403 if forbidden.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')") // Customer or Admin can attempt to get
    public ResponseEntity<DtoReturnRequest> getReturnRequestById(
            @PathVariable Long id,
            Authentication authentication) {

        // Get the authenticated user's ID using the correct method name
        Long loggedInUserId = SecurityUtils.getAuthenticatedSellerId(authentication); // <<< CORRECTED method name
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal(); // Get principal details

        DtoReturnRequest request = returnRequestService.getReturnRequestById(id); // Fetch first

        // Security Check: Allow if user is ADMIN or if user is the CUSTOMER who owns the request
        boolean isAdmin = principal.getAuthorities().stream()
                                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin || Objects.equals(request.getCustomerId(), loggedInUserId)) {
            return ResponseEntity.ok(request);
        } else {
            // If the user is not an admin and not the owner, deny access
             throw new AccessDeniedException("You do not have permission to view this return request.");
             // Or return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Gets all return requests for the currently logged-in customer.
     * Only accessible by authenticated users with the CUSTOMER role.
     *
     * @param authentication The authentication object for the current user.
     * @return ResponseEntity containing a list of DtoReturnRequest.
     */
    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('CUSTOMER')") // Only customers can view their own requests
    public ResponseEntity<List<DtoReturnRequest>> getCurrentUserReturnRequests(Authentication authentication) {
         // Get the authenticated customer's ID using SecurityUtils with the correct method name
         Long customerId = SecurityUtils.getAuthenticatedSellerId(authentication); // <<< CORRECTED method name

         // No need for null check here as @PreAuthorize ensures authentication
         // and SecurityUtils throws exception if ID cannot be retrieved.

        List<DtoReturnRequest> requests = returnRequestService.getReturnRequestsByCustomerId(customerId);
        return ResponseEntity.ok(requests);
    }

     /**
     * Gets all return requests for a specific customer (Admin only).
     *
     * @param customerId The ID of the customer whose requests to fetch.
     * @return ResponseEntity containing a list of DtoReturnRequest.
     */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN')") // Only Admins can access this endpoint
    public ResponseEntity<List<DtoReturnRequest>> getReturnRequestsByCustomerId(@PathVariable Long customerId) {
        // Security is handled by @PreAuthorize
        List<DtoReturnRequest> requests = returnRequestService.getReturnRequestsByCustomerId(customerId);
        return ResponseEntity.ok(requests);
    }


    /**
     * Updates the status of a return request (Admin operation).
     *
     * @param id The ID of the return request to update.
     * @param statusUpdatePayload A Map containing the new 'status' and optional 'resolutionNotes'.
     * @return ResponseEntity containing the updated DtoReturnRequest.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')") // Only Admins can update status
    public ResponseEntity<DtoReturnRequest> updateReturnStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusUpdatePayload) {

        String statusString = statusUpdatePayload.get("status");
        String resolutionNotes = statusUpdatePayload.get("resolutionNotes"); // Optional

        if (statusString == null) {
            // Consider returning a more informative error response DTO
            return ResponseEntity.badRequest().body(null);
        }

        ReturnStatus newStatus;
        try {
            newStatus = ReturnStatus.valueOf(statusString.toUpperCase());
        } catch (IllegalArgumentException e) {
             // Consider returning a more informative error response DTO
            return ResponseEntity.badRequest().body(null); // Indicating invalid status value
        }

        // Security handled by @PreAuthorize

        DtoReturnRequest updatedRequest = returnRequestService.updateReturnStatus(id, newStatus, resolutionNotes);
        return ResponseEntity.ok(updatedRequest);
    }

    // --- Removed the placeholder helper method ---
    // Authentication object provides the necessary context via SecurityUtils
}

