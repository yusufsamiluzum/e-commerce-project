package com.ecommerce.controller.impl;

import com.ecommerce.exceptions.InvalidStatusException;
import com.ecommerce.exceptions.ShipmentNotFoundException;
import com.ecommerce.services.ShipmentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Import a DTO representing the expected webhook payload structure
// e.g., import com.ecommerce.dto.WebhookPayloadDto;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private ShipmentService shipmentService;

    // Define a DTO that matches the structure of the webhook JSON payload
    // This structure is specific to the API provider (EasyPost, Shippo, etc.)
    public static class WebhookPayloadDto {
        // Example fields - ADJUST THESE BASED ON THE ACTUAL PAYLOAD
        public String eventType; // e.g., "tracker.updated"
        public TrackingData data;

        public static class TrackingData {
            public String trackingCode; // The tracking number
            public String status;       // The new status string (e.g., "in_transit")
            public String statusDetail; // Additional detail
            // Add other fields as needed (timestamp, location, carrier details)
        }
    }


    @PostMapping("/shipments") // Or the specific path expected by the provider
    public ResponseEntity<Void> handleShipmentUpdate(@RequestBody WebhookPayloadDto payload
                                                     /* Add @RequestHeader for signature validation if needed */) {
        log.info("Received webhook payload: {}", payload); // Be careful logging sensitive data

        // TODO: Implement webhook security validation if the provider supports it
        // (e.g., verify HMAC signature using a secret key)
        // if (!isValidSignature(request, payload)) {
        //     log.warn("Invalid webhook signature received.");
        //     return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        // }


        // Extract relevant data - adjust field names based on actual payload DTO
        if (payload == null || payload.data == null || payload.data.trackingCode == null || payload.data.status == null) {
             log.warn("Received incomplete webhook payload.");
             // Return OK to prevent retries for bad data, or Bad Request if appropriate
             return ResponseEntity.ok().build();
        }

        String trackingNumber = payload.data.trackingCode;
        String externalStatus = payload.data.status;
        String statusDetails = payload.data.statusDetail; // Optional

        try {
            shipmentService.handleWebhookUpdate(trackingNumber, externalStatus, statusDetails);
            return ResponseEntity.ok().build(); // Acknowledge successful processing
        } catch (ShipmentNotFoundException e) {
            log.warn("Webhook handler error: {}", e.getMessage());
            // Return OK even if not found to stop the provider from retrying usually.
            return ResponseEntity.ok().build();
        } catch (InvalidStatusException e) {
             log.error("Webhook handler error: {}", e.getMessage());
             // Bad request might be suitable if the status is truly invalid
             return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error handling webhook for tracking number {}: {}", trackingNumber, e.getMessage(), e);
            // Internal server error - the provider might retry
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
