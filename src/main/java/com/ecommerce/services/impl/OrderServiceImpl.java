package com.ecommerce.services.impl;

import com.ecommerce.dto.*; // Import all DTOs
import com.ecommerce.entities.Payment;
import com.ecommerce.entities.order.*;
import com.ecommerce.entities.user.*;
import com.ecommerce.entities.product.Product;

import com.ecommerce.exceptions.*;
import com.ecommerce.mappers.*; // Import Mappers
import com.ecommerce.repository.AddressRepository;
import com.ecommerce.repository.CustomerRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.services.OrderService;
import com.ecommerce.services.PaymentService;
import com.ecommerce.exceptions.OrderCreationException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.util.ArrayList; // Keep this import
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor // Lombok annotation for constructor injection
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    // No longer need OrderItemRepository directly if CascadeType.ALL handles items
    // private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;

    // --- Inject PaymentService ---
    private final PaymentService paymentService;

    // --- Inject Mappers ---
    private final OrderMapper orderMapper;
    // Mappers for nested objects are used by OrderMapper via 'uses' attribute
    // No need to inject OrderItemMapper, ProductMapper etc. directly here if configured correctly
    
    
    @Override
    @Transactional(readOnly = true) // Okuma işlemi olduğu için
    public List<DtoOrderResponse> getOrdersForCurrentSeller(Long sellerId) {
        log.debug("Fetching orders for seller ID: {}", sellerId); // Loglama ekleyebilirsiniz

        // Opsiyonel: Seller'ın gerçekten var olup olmadığını kontrol etmek iyi bir pratik olabilir
        // Seller seller = sellerRepository.findById(sellerId)
        //        .orElseThrow(() -> new UserNotFoundException("Seller not found with ID: " + sellerId));

        // OrderRepository'ye eklediğimiz metodu çağır
        List<Order> orders = orderRepository.findBySellerUserId(sellerId);

        log.debug("Found {} orders for seller ID: {}", orders.size(), sellerId);

        // Bulunan Order listesini DTO listesine çevir ve döndür
        return orderMapper.toDtoOrderResponseList(orders);
    }
    
    

    @Override
    @Transactional
    public DtoOrderResponse createOrder(DtoOrderRequest requestDTO, Long customerId) {
        // 1. Fetch Customer
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new UserNotFoundException("Customer not found with ID: " + customerId));

        // 2. Fetch Addresses
        Address shippingAddress = addressRepository.findByAddressIdAndUserUserId(requestDTO.getShippingAddressId(), customerId)
                 .orElseThrow(() -> new AddressNotFoundException("Shipping address not found or doesn't belong to customer"));
        Address billingAddress = addressRepository.findByAddressIdAndUserUserId(requestDTO.getBillingAddressId(), customerId)
                 .orElseThrow(() -> new AddressNotFoundException("Billing address not found or doesn't belong to customer"));

        // 3. Create the Order entity
        Order order = new Order();
        order.setCustomer(customer);
        order.setShippingAddress(shippingAddress);
        order.setBillingAddress(billingAddress);
        order.setStatus(Order.OrderStatus.PENDING); // More specific initial status
        order.setOrderNumber(generateOrderNumber());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        // 4. Process Order Items
        for (DtoOrderItemRequest itemDTO : requestDTO.getItems()) {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Product with ID " + itemDTO.getProductId() + " not found"));
            
            if (order.getSeller() == null && product.getSeller() != null) { // Henüz atanmadıysa ve ürünün satıcısı varsa
                order.setSeller(product.getSeller());
                log.info("Setting seller for order {} to seller ID: {}", order.getOrderNumber(), product.getSeller().getUserId()); // Loglama
           } else if (order.getSeller() != null && product.getSeller() != null && !order.getSeller().getUserId().equals(product.getSeller().getUserId())) {
               // Farklı satıcıdan ürün eklenmeye çalışılırsa hata ver (varsayıma göre)
                log.error("Attempted to add product from different seller (Product: {}, Existing Seller: {}, New Seller: {}) to order {}",
                       product.getProductId(), order.getSeller().getUserId(), product.getSeller().getUserId(), order.getOrderNumber());
                throw new CartOperationException("An order can only contain products from a single seller.");
           }
            
            

            if (product.getStockQuantity() < itemDTO.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for product ID: " + product.getProductId());
            }
            product.setStockQuantity(product.getStockQuantity() - itemDTO.getQuantity());
            // No need to save product here if OrderItem cascade includes Product merge/update,
            // but explicit save is safer.
            productRepository.save(product);

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setPriceAtPurchase(product.getPrice());

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
        }

        // 5. Set final order details
        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);
        
        
     // Satıcı atanmadıysa hata ver (siparişte ürün yoksa veya ürünlerin satıcısı yoksa olabilir)
        if (order.getSeller() == null) {
             log.error("Cannot create order {} without a seller. Ensure products have sellers.", order.getOrderNumber());
             throw new OrderCreationException("Seller could not be determined for the order. Products might be missing seller information.");
        }

        // ---> 6. Create Initial Payment Record <---
        Payment initialPayment = new Payment();
        initialPayment.setOrder(order); // Link payment to this order
        initialPayment.setAmount(totalAmount);
        initialPayment.setStatus(Payment.PaymentStatus.PENDING);
        initialPayment.setPaymentMethod(null); // Payment method unknown initially
        initialPayment.setGatewayTransactionId(null); // No transaction ID yet

        // ---> Associate Payment with Order <---
        // Assuming bidirectional @OneToOne with mappedBy on Order side
        order.setPayment(initialPayment);

        // 7. Save the order (and Payment via Cascade, assumed)
        Order savedOrder = orderRepository.save(order);

        // If not cascading Payment from Order, save it explicitly BEFORE saving order:
        // paymentRepository.save(initialPayment);
        // order.setPayment(initialPayment); // Ensure the relationship is set before saving Order
        // Order savedOrder = orderRepository.save(order);


        // 8. Convert saved entity to Response DTO
        // Ensure OrderMapper handles mapping the embedded Payment to DtoPaymentSummary
        return orderMapper.toDtoOrderResponse(savedOrder);
    }


    @Override
    public DtoOrderResponse getOrderById(Long orderId, Long userId, String userRole) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + orderId + " not found"));

        // Authorization Check
        boolean isAdmin = userRole != null && userRole.equals("ROLE_ADMIN"); // userRole'un "ROLE_ADMIN" olup olmadığını kontrol et
        boolean isOwner = order.getCustomer() != null && order.getCustomer().getUserId().equals(userId);

        if (!isAdmin && !isOwner) { // Eğer admin DEĞİLSE ve sahip DEĞİLSE yetkisiz erişim fırlat
             throw new UnauthorizedAccessException("User does not have permission to view this order");
        }

        return orderMapper.toDtoOrderResponse(order); // Use mapper
    }

    @Override
    public List<DtoOrderResponse> getOrdersByCustomerId(Long customerId) {
        List<Order> orders = orderRepository.findByCustomerUserId(customerId);
        return orderMapper.toDtoOrderResponseList(orders); // Use mapper for list
    }

    @Override
    public List<DtoOrderResponse> getAllOrders(/* Add Pagination Params */) {
        List<Order> orders = orderRepository.findAll(/* Pass Pageable object */);
        return orderMapper.toDtoOrderResponseList(orders); // Use mapper for list
    }


    @Override
    @Transactional
    public DtoOrderResponse updateOrderStatus(Long orderId, Order.OrderStatus newStatus, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order with ID " + orderId + " not found"));

        // TODO: Authorization check (who can update status?)
        // TODO: Status transition validation

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        // TODO: Trigger side effects (notify customer, initiate shipment etc.)

        return orderMapper.toDtoOrderResponse(updatedOrder); // Use mapper
    }

    @Override
    @Transactional
    public DtoOrderResponse cancelOrder(Long orderId, Long userId, String userRole) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        // Authorization
        if (!userRole.equals("ADMIN") && !order.getCustomer().getUserId().equals(userId)) {
             throw new UnauthorizedAccessException("User cannot cancel this order");
        }

        // Business Logic: Check if cancellable
        if (order.getStatus() == Order.OrderStatus.SHIPPED || order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new OrderCancellationException("Cannot cancel order that is already shipped or delivered");
        }
         if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            log.info("Order {} is already cancelled. Returning current state.", orderId);
            return orderMapper.toDtoOrderResponse(order); // Already cancelled
        }

        // Check if a refund is required (i.e., payment was successful)
        boolean requiresRefund = order.getPayment() != null &&
                                 order.getPayment().getStatus() == Payment.PaymentStatus.SUCCESS;

        log.info("Cancelling order {}. Requires refund: {}", orderId, requiresRefund);

        // Restore stock
        log.debug("Restoring stock for order {}", orderId);
        for (OrderItem item : order.getItems()) {
             Product product = item.getProduct();
             int quantityToRestore = item.getQuantity();
             product.setStockQuantity(product.getStockQuantity() + quantityToRestore);
             productRepository.save(product);
             log.debug("Restored {} units for product {}", quantityToRestore, product.getProductId());
        }

        // Trigger Refund Process if required
        if (requiresRefund) {
            Payment paymentToRefund = order.getPayment();
            log.info("Initiating refund for payment {} associated with order {}", paymentToRefund.getPaymentId(), orderId);
            try {
                // Delegate refund initiation to PaymentService
                paymentService.initiateRefund(paymentToRefund.getPaymentId());
                log.info("Refund successfully initiated for payment {}", paymentToRefund.getPaymentId());
                // Optional: You might set the payment status to REFUND_PENDING here if needed
                // paymentToRefund.setStatus(Payment.PaymentStatus.REFUND_PENDING); // Example
                // paymentRepository.save(paymentToRefund); // Save if status changed
            } catch (Exception e) {
                // Log the error, but allow order cancellation to proceed
                log.error("Failed to initiate refund for payment {} (Order {}). Order will still be cancelled. Error: {}",
                          paymentToRefund.getPaymentId(), orderId, e.getMessage(), e);
                // Depending on requirements, you might:
                // 1. Throw a specific exception to indicate partial failure.
                // 2. Store a flag indicating refund needs manual attention.
                // For now, we log and continue cancellation.
            }
        }

        // Update order status
        order.setStatus(Order.OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);
        log.info("Order {} successfully cancelled.", orderId);

        return orderMapper.toDtoOrderResponse(cancelledOrder); // Use mapper
    }


    private String generateOrderNumber() {
        // Simple example: timestamp + random part
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    // convertToDto method is now handled by the injected OrderMapper
}
