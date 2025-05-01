package com.ecommerce.services;

import com.ecommerce.entities.Shipment;
import com.ecommerce.entities.order.Order;
import com.ecommerce.entities.user.LogisticsProvider;
// Import DTOs if you create them (e.g., ShipmentRequestDto, WebhookPayloadDto)
import com.ecommerce.exceptions.ExternalApiException;
import com.ecommerce.exceptions.InvalidStatusException;
import com.ecommerce.exceptions.ShipmentCreationException;
import com.ecommerce.exceptions.ShipmentNotFoundException;

import java.util.Optional;

public interface ShipmentService {

    /**
     * Creates a shipment record locally and initiates shipment creation
     * with the external logistics provider API.
     *
     * @param order The order to be shipped.
     * @param logisticsProvider The chosen logistics provider.
     * // Add other necessary parameters like parcel details if not derivable from Order
     * @return The created Shipment entity with tracking info.
     * @throws ShipmentCreationException if the external API call fails.
     */
    Shipment createShipmentForOrder(Order order, LogisticsProvider logisticsProvider /*, ShipmentDetails details */) throws ShipmentCreationException;

    /**
     * Retrieves the current status of a shipment using its tracking number.
     * Preferably, rely on webhooks, but this can be a fallback or manual check.
     *
     * @param trackingNumber The tracking number.
     * @return The current ShipmentStatus.
     * @throws ShipmentNotFoundException if the shipment isn't found locally.
     * @throws ExternalApiException if fetching status from external API fails.
     */
    Shipment.ShipmentStatus getShipmentStatus(String trackingNumber) throws ShipmentNotFoundException, ExternalApiException;

    /**
     * Handles incoming status updates from the external provider's webhook.
     *
     * @param trackingNumber The tracking number from the webhook payload.
     * @param externalStatus The status provided by the external service.
     * @param statusDetails Additional details from the webhook (optional).
     * @throws ShipmentNotFoundException if the shipment isn't found locally.
     * @throws InvalidStatusException if the external status cannot be mapped.
     */
    void handleWebhookUpdate(String trackingNumber, String externalStatus, String statusDetails) throws ShipmentNotFoundException, InvalidStatusException;

    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    // Add other methods as needed (e.g., generate label, cancel shipment if API supports it)
}


