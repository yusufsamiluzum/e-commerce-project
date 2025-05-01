package com.ecommerce.services.impl;

import com.ecommerce.clients.ExternalShippingApiClient;
import com.ecommerce.entities.Shipment;
import com.ecommerce.entities.order.Order;
import com.ecommerce.entities.user.Address;
import com.ecommerce.entities.user.LogisticsProvider;
import com.ecommerce.exceptions.ExternalApiException;
import com.ecommerce.exceptions.InvalidStatusException;
import com.ecommerce.exceptions.ShipmentCreationException;
import com.ecommerce.exceptions.ShipmentNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ShipmentRepository;
import com.ecommerce.services.ShipmentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.Locale;
import java.util.Optional;

@Service
public class ShipmentServiceImpl implements ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentServiceImpl.class);

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private OrderRepository orderRepository; // Inject OrderRepository to update Order status

    @Autowired
    private ExternalShippingApiClient externalApiClient; // Inject the client

    @Override
    @Transactional // Make this transactional
    public Shipment createShipmentForOrder(Order order, LogisticsProvider logisticsProvider /*, ShipmentDetails details */) throws ShipmentCreationException {
        log.info("Creating shipment for Order ID: {}", order.getOrderId());

        // 1. Validate inputs (e.g., ensure order has shipping address)
        if (order.getShippingAddress() == null) {
            throw new IllegalArgumentException("Order must have a shipping address.");
        }
        // Assume you have a way to get the 'from' address (e.g., seller's address or warehouse)
        Address fromAddress = getSenderAddress(order.getSeller()); // Implement this logic

        // 2. Prepare data for the external API call
        Address toAddress = order.getShippingAddress();
        // TODO: Create ParcelDetails based on order items (weight, dimensions) - Implement this logic
        ExternalShippingApiClient.ParcelDetails parcelDetails = calculateParcelDetails(order);

        try {
            // 3. Call the external API client
            ExternalShippingApiClient.ShipmentApiResponse apiResponse = externalApiClient.createExternalShipment(
                    fromAddress,
                    toAddress,
                    parcelDetails,
                    logisticsProvider.getCompanyName()
            );

            // 4. Create and save the local Shipment entity
            Shipment shipment = new Shipment();
            shipment.setOrder(order);
            shipment.setLogisticsProvider(logisticsProvider);
            // Use getters:
            shipment.setTrackingNumber(apiResponse.getTrackingNumber());
            shipment.setCarrier(logisticsProvider.getCompanyName());
            // Use getters:
            shipment.setStatus(mapExternalStatus(apiResponse.getInitialStatus()));

            Shipment savedShipment = shipmentRepository.save(shipment);
            log.info("Shipment created locally with ID: {} and Tracking: {}", savedShipment.getShipmentId(), savedShipment.getTrackingNumber());

            // 5. Update the Order status (optional, but common)
            order.setStatus(Order.OrderStatus.SHIPPED);
            orderRepository.save(order); // Save the updated order

            // TODO: Store label URL somewhere if needed

            return savedShipment;

        } catch (Exception e) {
            log.error("Failed to create shipment via external API for Order ID: {}", order.getOrderId(), e);
            // Wrap specific API exceptions if the client throws them
            throw new ShipmentCreationException("External API call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Shipment.ShipmentStatus getShipmentStatus(String trackingNumber) throws ShipmentNotFoundException, ExternalApiException {
         log.debug("Getting status for tracking number: {}", trackingNumber);
         Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found with tracking: " + trackingNumber));

        // Optional: Call external API to get latest status (if not relying solely on webhooks)
        /*
        try {
            ExternalShippingApiClient.TrackingStatusApiResponse apiResponse = externalApiClient.getExternalTrackingStatus(trackingNumber);
            Shipment.ShipmentStatus newStatus = mapExternalStatus(apiResponse.status);
            if (shipment.getStatus() != newStatus) {
                 log.info("Updating status for {} from {} to {}", trackingNumber, shipment.getStatus(), newStatus);
                 shipment.setStatus(newStatus);
                 shipmentRepository.save(shipment);
            }
             return newStatus;
        } catch (Exception e) {
            log.error("Failed to get external status for tracking number: {}", trackingNumber, e);
            throw new ExternalApiException("Failed to fetch status from external API", e);
        }
        */

        // Primarily return the locally stored status (updated by webhooks)
        return shipment.getStatus();
    }

    @Override
    @Transactional
    public void handleWebhookUpdate(String trackingNumber, String externalStatus, String statusDetails) throws ShipmentNotFoundException, InvalidStatusException {
        log.info("Webhook received for Tracking: {}, Status: {}", trackingNumber, externalStatus);
        Shipment shipment = shipmentRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> {
                    log.warn("Webhook received for unknown tracking number: {}", trackingNumber);
                    return new ShipmentNotFoundException("Shipment not found for tracking number: " + trackingNumber);
                });

        Shipment.ShipmentStatus newStatus = mapExternalStatus(externalStatus);

        if (shipment.getStatus() != newStatus) {
            log.info("Updating shipment {} status from {} to {}", shipment.getShipmentId(), shipment.getStatus(), newStatus);
            shipment.setStatus(newStatus);
            // Optionally store statusDetails somewhere if needed
            shipmentRepository.save(shipment);

            // Optionally, update Order status if the shipment reaches a final state (e.g., DELIVERED)
             if (newStatus == Shipment.ShipmentStatus.DELIVERED) {
                 Order order = shipment.getOrder();
                 if (order.getStatus() != Order.OrderStatus.DELIVERED && order.getStatus() != Order.OrderStatus.CANCELLED) {
                     log.info("Updating Order {} status to DELIVERED", order.getOrderId());
                     order.setStatus(Order.OrderStatus.DELIVERED);
                     orderRepository.save(order);
                 }
             }
            // Handle other statuses like FAILED_DELIVERY, RETURNED etc.

        } else {
            log.info("Webhook status {} for {} is the same as current status. No update needed.", newStatus, trackingNumber);
        }
    }

     @Override
     public Optional<Shipment> findByTrackingNumber(String trackingNumber) {
         return shipmentRepository.findByTrackingNumber(trackingNumber);
     }

    // --- Helper Methods ---

    private Shipment.ShipmentStatus mapExternalStatus(String externalStatus) throws InvalidStatusException {
         if (externalStatus == null || externalStatus.isBlank()) {
             return Shipment.ShipmentStatus.PROCESSING; // Default or initial
         }

        // This mapping depends heavily on the strings used by the specific API provider
        String statusLower = externalStatus.toLowerCase(Locale.ROOT).replace("_", ""); // Normalize
        switch (statusLower) {
            case "processing":
            case "pretransit": // Common alternative
            case "labelcreated":
                return Shipment.ShipmentStatus.PROCESSING;
            case "intransit":
                 return Shipment.ShipmentStatus.IN_TRANSIT;
            case "pickup": // Check specific API term
            case "pickedup":
                return Shipment.ShipmentStatus.PICKED_UP;
            case "outfordelivery":
                return Shipment.ShipmentStatus.OUT_FOR_DELIVERY;
            case "delivered":
                return Shipment.ShipmentStatus.DELIVERED;
            case "failure": // Common term
            case "deliveryfailed":
            case "faileddelivery":
                 return Shipment.ShipmentStatus.FAILED_DELIVERY;
            case "returned": // Check specific API term
            case "returntosender":
                 return Shipment.ShipmentStatus.RETURNED;
            // Add mappings for other statuses provided by the API (unknown, exception, etc.)
            default:
                log.warn("Unmapped external status received: {}", externalStatus);
                // Decide whether to throw an error or map to a default/unknown status
                // For robustness, maybe map to the current status or a generic 'IN_TRANSIT' if unsure.
                // Or throw: throw new InvalidStatusException("Unknown external status: " + externalStatus);
                return Shipment.ShipmentStatus.IN_TRANSIT; // Example fallback
        }
    }


    // TODO: Implement these helper methods based on your application's logic
    private Address getSenderAddress(com.ecommerce.entities.user.Seller seller) {
        // Logic to retrieve the appropriate 'from' address (e.g., seller's primary business address)
        // This needs access to Seller/User data and their addresses.
         log.warn("getSenderAddress needs implementation!");
         // Placeholder:
         Address address = new Address();
         address.setStreet("123 Warehouse St");
         address.setCity("Shippington");
         address.setPostalCode("90210");
         address.setCountry("US");
         address.setState("CA");
         address.setPhoneNumber("555-1234");
         return address;
    }

    private ExternalShippingApiClient.ParcelDetails calculateParcelDetails(Order order) {
        // Logic to calculate total weight and potentially estimate dimensions based on order items.
        // This requires accessing Product data linked to OrderItems.
        log.warn("calculateParcelDetails needs implementation!");
        // Placeholder:
        ExternalShippingApiClient.ParcelDetails details = new ExternalShippingApiClient.ParcelDetails();
        details.setWeight(16); // e.g., 16 oz
        details.setLength(10); details.setWidth(8); details.setHeight(4); // e.g., inches
        return details;
    }
}
