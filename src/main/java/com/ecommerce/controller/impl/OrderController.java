package com.ecommerce.controller.impl;

import com.ecommerce.config.securityconfig.UserPrincipal; // Import UserPrincipal
import com.ecommerce.dto.DtoOrderRequest;
import com.ecommerce.dto.DtoOrderResponse;
import com.ecommerce.entities.order.Order.OrderStatus;
import com.ecommerce.services.OrderService;

// --- Import Spring Security classes ---
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.GrantedAuthority; // Needed for authorities


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors; // For collecting authorities

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')") // Ensure roles match UserPrincipal (e.g., ROLE_CUSTOMER)
    public ResponseEntity<DtoOrderResponse> createOrder(
            @Valid @RequestBody DtoOrderRequest orderRequestDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) { // Use UserPrincipal

        // Get customer ID from the wrapped User object
        // Assuming User class has getId() method. Adjust if needed.
        Long customerId = currentUser.getUser().getUserId(); // Updated access

        DtoOrderResponse createdOrder = orderService.createOrder(orderRequestDTO, customerId);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    @GetMapping("/{orderId}")
    // Example: Allow ADMIN or the specific CUSTOMER who owns the order
    // Update @orderSecurityService.isOrderOwner to accept UserPrincipal if needed
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @orderSecurityService.isOrderOwner(principal, #orderId)")
    public ResponseEntity<DtoOrderResponse> getOrderById(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal currentUser) { // Use UserPrincipal

        // Extract details needed for service layer authorization check
        Long userId = currentUser.getUser().getUserId(); // Updated access
        // Get roles/authorities correctly from UserPrincipal
        // Assuming only one role per user as per UserPrincipal implementation
        String userRole = currentUser.getAuthorities().iterator().next().getAuthority(); // Access authorities


        DtoOrderResponse order = orderService.getOrderById(orderId, userId, userRole);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasAuthority('ROLE_CUSTOMER')") // Ensure roles match UserPrincipal
    public ResponseEntity<List<DtoOrderResponse>> getCurrentUserOrders(
            @AuthenticationPrincipal UserPrincipal currentUser) { // Use UserPrincipal

        Long customerId = currentUser.getUser().getUserId(); // Updated access
        List<DtoOrderResponse> orders = orderService.getOrdersByCustomerId(customerId);
        return ResponseEntity.ok(orders);
    }

    // Endpoint for Admins to get orders by a specific customer ID
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')") // Ensure roles match UserPrincipal
    public ResponseEntity<List<DtoOrderResponse>> getOrdersByCustomerIdForAdmin(@PathVariable Long customerId) {
        List<DtoOrderResponse> orders = orderService.getOrdersByCustomerId(customerId);
        return ResponseEntity.ok(orders);
    }

     // Endpoint for Admins to get all orders (add pagination later)
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')") // Ensure roles match UserPrincipal
    public ResponseEntity<List<DtoOrderResponse>> getAllOrders(/* Add @RequestParam for pagination */) {
        List<DtoOrderResponse> orders = orderService.getAllOrders(/* Pass pagination params */);
        return ResponseEntity.ok(orders);
    }


    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')") // Ensure roles match UserPrincipal
    public ResponseEntity<DtoOrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status,
            @AuthenticationPrincipal UserPrincipal currentUser) { // Use UserPrincipal

        Long adminUserId = currentUser.getUser().getUserId(); // Updated access (For audit log / potential checks)
        DtoOrderResponse updatedOrder = orderService.updateOrderStatus(orderId, status, adminUserId);
        return ResponseEntity.ok(updatedOrder);
    }

    @PutMapping("/{orderId}/cancel")
    // Update @orderSecurityService.isOrderOwner to accept UserPrincipal if needed
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or @orderSecurityService.isOrderOwner(principal, #orderId)")
    public ResponseEntity<DtoOrderResponse> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserPrincipal currentUser) { // Use UserPrincipal

        Long userId = currentUser.getUser().getUserId(); // Updated access
        String userRole = currentUser.getAuthorities().iterator().next().getAuthority(); // Access authorities
        DtoOrderResponse cancelledOrder = orderService.cancelOrder(orderId, userId, userRole);
        return ResponseEntity.ok(cancelledOrder);
    }
}

// --- Helper Service for Security Checks (Example) ---
// Make sure OrderSecurityService uses UserPrincipal if needed:
// @Service("orderSecurityService")
// public class OrderSecurityService {
//     @Autowired private OrderRepository orderRepository;
//
//     public boolean isOrderOwner(UserPrincipal currentUser, Long orderId) { // Use UserPrincipal
//         if (currentUser == null || currentUser.getUser() == null || orderId == null) return false;
//         Order order = orderRepository.findById(orderId).orElse(null);
//         // Compare IDs correctly
//         return order != null && order.getCustomer().getUserId().equals(currentUser.getUser().getId()); // Updated access
//     }
// }