package com.ecommerce.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.dto.DtoProductSummary;
import com.ecommerce.dto.DtoReturnRequest;
import com.ecommerce.dto.DtoUserSummary;
import com.ecommerce.entities.ReturnRequest;
import com.ecommerce.entities.ReturnRequest.ReturnStatus;
import com.ecommerce.entities.product.Product;
import com.ecommerce.entities.user.Address;
import com.ecommerce.entities.user.Customer;
import com.ecommerce.exceptions.ResourceNotFoundException;
import com.ecommerce.repository.AddressRepository;
import com.ecommerce.repository.CustomerRepository;
import com.ecommerce.repository.OrderItemRepository;
import com.ecommerce.repository.ReturnRequestRepository;
import com.ecommerce.services.ReturnRequestService;

import jakarta.transaction.Transactional;

@Service
class ReturnRequestServiceImpl implements ReturnRequestService {

    @Autowired
    private ReturnRequestRepository returnRequestRepository;

    @Autowired
    private CustomerRepository customerRepository; // Inject Customer repository

    @Autowired
    private OrderItemRepository orderItemRepository; // Inject OrderItem repository

    @Autowired
    private AddressRepository addressRepository; // Inject Address repository

    @Transactional
    @Override
    public DtoReturnRequest createReturnRequest(DtoReturnRequest returnRequestDto) {
        // --- Validation ---
        // 1. Check if Customer exists
        var customer = customerRepository.findById(returnRequestDto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + returnRequestDto.getCustomerId()));

        // 2. Check if OrderItem exists and belongs to the customer (add this check if OrderItem has customer relation)
        var orderItem = orderItemRepository.findById(returnRequestDto.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found with id: " + returnRequestDto.getOrderItemId()));
        // TODO: Add validation: Ensure orderItem belongs to the customer

        // 3. Check if Pickup Address exists and belongs to the customer (if provided)
        Address pickupAddress = null;
        if (returnRequestDto.getPickupAddressId() != null) {
            pickupAddress = addressRepository.findByAddressIdAndUserUserId(returnRequestDto.getPickupAddressId(), customer.getUserId()) // Assuming Address repo has this method
                    .orElseThrow(() -> new ResourceNotFoundException("Pickup Address not found or does not belong to customer"));
        }

        // 4. Check if a return request already exists for this order item (optional, depends on policy)
        boolean exists = returnRequestRepository.findByOrderItemOrderItemId(orderItem.getOrderItemId()).stream()
                .anyMatch(req -> req.getStatus() != ReturnStatus.CANCELLED && req.getStatus() != ReturnStatus.REJECTED);
        if (exists) {
            throw new IllegalStateException("An active return request already exists for this order item.");
        }
        // TODO: Add more validation (e.g., is the item returnable? Is it within the return window?)

        // --- Create Entity ---
        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setCustomer(customer);
        returnRequest.setOrderItem(orderItem);
        returnRequest.setReason(returnRequestDto.getReason());
        returnRequest.setComments(returnRequestDto.getComments());
        returnRequest.setPickupAddress(pickupAddress);
        returnRequest.setStatus(ReturnStatus.PENDING); // Initial status

        // --- Save and Convert ---
        ReturnRequest savedRequest = returnRequestRepository.save(returnRequest);
        return convertToDto(savedRequest);
    }

    @Override
    public DtoReturnRequest getReturnRequestById(Long returnRequestId) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnRequest not found with id: " + returnRequestId));
        return convertToDto(returnRequest);
    }

     // Example: Get request ensuring it belongs to the customer
     public DtoReturnRequest getReturnRequestByIdForCustomer(Long returnRequestId, Long customerId) {
        ReturnRequest returnRequest = returnRequestRepository.findByReturnRequestIdAndCustomerUserId(returnRequestId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnRequest not found with id: " + returnRequestId + " for customer " + customerId));
        return convertToDto(returnRequest);
    }


    @Override
    public List<DtoReturnRequest> getReturnRequestsByCustomerId(Long customerId) {
        // Ensure customer exists
         customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        List<ReturnRequest> requests = returnRequestRepository.findByCustomerUserId(customerId);
        return requests.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public DtoReturnRequest updateReturnStatus(Long returnRequestId, ReturnStatus status, String resolutionNotes) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnRequest not found with id: " + returnRequestId));

        // TODO: Add validation for status transitions (e.g., cannot go from COMPLETED back to PENDING)

        returnRequest.setStatus(status);
        if (resolutionNotes != null && !resolutionNotes.isBlank()) {
             returnRequest.setResolutionNotes(resolutionNotes);
        }

        ReturnRequest updatedRequest = returnRequestRepository.save(returnRequest);
        return convertToDto(updatedRequest);
    }

    // --- Helper Method: Convert Entity to DTO ---
    // (Consider using a mapping library like MapStruct for more complex conversions)
    private DtoReturnRequest convertToDto(ReturnRequest entity) {
        DtoReturnRequest dto = new DtoReturnRequest();
        dto.setReturnRequestId(entity.getReturnRequestId());
        dto.setOrderItemId(entity.getOrderItem().getOrderItemId()); // Assuming OrderItem has getId()
        dto.setCustomerId(entity.getCustomer().getUserId()); // Assuming Customer has getUserId()
        dto.setReason(entity.getReason());
        dto.setComments(entity.getComments());
        dto.setStatus(entity.getStatus());
        dto.setPickupAddressId(entity.getPickupAddress() != null ? entity.getPickupAddress().getAddressId() : null); // Assuming Address has getId()
        dto.setResolutionNotes(entity.getResolutionNotes());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // Populate summary DTOs (requires OrderItem to have Product relation)
        if (entity.getOrderItem() != null && entity.getOrderItem().getProduct() != null) {
             Product product = entity.getOrderItem().getProduct();
             // NOTE: Assumes Product has methods like getProductId, getName, getPrice etc.
             // You might need to fetch ProductImage separately for primaryImageUrl
             dto.setProductSummary(new DtoProductSummary(
                 product.getProductId(),
                 product.getName(),
                 product.getPrice(),
                 "placeholder_image_url", // Replace with actual image URL logic
                 product.getAverageRating(),
                 product.getBrand(),
                 product.getModel()
             ));
        }
         if (entity.getCustomer() != null) {
             Customer customer = entity.getCustomer();
             // NOTE: Assumes Customer has getUserId, getUsername etc.
             dto.setCustomerSummary(new DtoUserSummary(
                 customer.getUserId(),
                 customer.getUsername(),
                 customer.getFirstName(),
                 customer.getLastName()
             ));
         }

        return dto;
    }
}