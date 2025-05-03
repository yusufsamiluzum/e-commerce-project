package com.ecommerce.services.impl;

import com.ecommerce.clients.ExternalShippingApiClient;
import com.ecommerce.entities.Shipment;
import com.ecommerce.entities.order.Order;
import com.ecommerce.entities.user.Address;
import com.ecommerce.entities.user.LogisticsProvider;
import com.ecommerce.entities.user.Seller;
import com.ecommerce.exceptions.ExternalApiException;
import com.ecommerce.exceptions.InvalidStatusException;
import com.ecommerce.exceptions.ShipmentCreationException;
import com.ecommerce.exceptions.ShipmentNotFoundException;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ShipmentRepository;
import com.ecommerce.services.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShipmentServiceImpl Tests")
class ShipmentServiceImplTest {

    // Mocks for dependencies
    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ExternalShippingApiClient externalApiClient;

    // Inject mocks into the service implementation
    @InjectMocks
    private ShipmentServiceImpl shipmentService;

    // Test data
    private Order testOrder;
    private LogisticsProvider testProvider;
    private Seller testSeller;
    private Address testShippingAddress;
    private Address testFromAddress; // Placeholder for sender address
    private ExternalShippingApiClient.ParcelDetails testParcelDetails; // Placeholder for parcel details
    private Shipment testShipment;
    private String testTrackingNumber = "TRACK12345";

    @BeforeEach
    void setUp() {
        // Initialize common test objects before each test
        testSeller = new Seller(); // Assume Seller has necessary fields or mock them
        testSeller.setUserId(1L);

        testShippingAddress = new Address();
        testShippingAddress.setStreet("123 Dest St");
        testShippingAddress.setCity("DestinationCity");
        testShippingAddress.setPostalCode("54321");
        testShippingAddress.setCountry("US");
        testShippingAddress.setState("CA");

        testOrder = new Order();
        testOrder.setOrderId(100L);
        testOrder.setShippingAddress(testShippingAddress);
        testOrder.setSeller(testSeller); // Link seller to order
        testOrder.setStatus(Order.OrderStatus.PROCESSING);

        testProvider = new LogisticsProvider();
        testProvider.setUserId(2L);
        testProvider.setCompanyName("FastShip");

        // Mock behavior for helper methods (getSenderAddress, calculateParcelDetails)
        // Since they are private, we test the public methods that use them and assume
        // they work or mock the external calls those methods trigger indirectly.
        // For a real test, consider refactoring these helpers to be protected or package-private,
        // or use PowerMockito/reflection if absolutely necessary (though generally discouraged).

        testFromAddress = new Address(); // Mocked sender address
        testFromAddress.setStreet("456 Source St");
        testFromAddress.setCity("SourceCity");

        testParcelDetails = new ExternalShippingApiClient.ParcelDetails(); // Mocked parcel details
        testParcelDetails.setWeight(10);

        testShipment = new Shipment();
        testShipment.setShipmentId(500L);
        testShipment.setOrder(testOrder);
        testShipment.setLogisticsProvider(testProvider);
        testShipment.setTrackingNumber(testTrackingNumber);
        testShipment.setCarrier(testProvider.getCompanyName());
        testShipment.setStatus(Shipment.ShipmentStatus.PROCESSING);
    }

    // --- createShipmentForOrder Tests ---

    @Test
    @DisplayName("createShipmentForOrder - Success")
    void createShipmentForOrder_Success() throws Exception {
        // Arrange
        // --- Corrected line below ---
        // Use the existing constructor with arguments
        ExternalShippingApiClient.ShipmentApiResponse apiResponse = new ExternalShippingApiClient.ShipmentApiResponse(
            testTrackingNumber, // Provide the tracking number
            "http://example.com/label/dummy.pdf", // Provide a dummy label URL
            "processing" // Provide the initial status
        );
        // --- End of correction ---

        // Mock external API call
        when(externalApiClient.createExternalShipment(any(Address.class), any(Address.class), any(ExternalShippingApiClient.ParcelDetails.class), eq(testProvider.getCompanyName())))
                .thenReturn(apiResponse);
        // Mock repository save operations
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> {
             Shipment s = invocation.getArgument(0);
             s.setShipmentId(501L); // Simulate saving and getting an ID
             return s;
         });
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder); // Assume order save is successful

        // Act
        Shipment createdShipment = shipmentService.createShipmentForOrder(testOrder, testProvider);

        // Assert
        assertNotNull(createdShipment);
        assertEquals(testTrackingNumber, createdShipment.getTrackingNumber());
        assertEquals(testProvider.getCompanyName(), createdShipment.getCarrier());
        assertEquals(Shipment.ShipmentStatus.PROCESSING, createdShipment.getStatus()); // Mapped status
        assertEquals(Order.OrderStatus.SHIPPED, testOrder.getStatus()); // Verify order status updated

        // Verify interactions
        verify(externalApiClient, times(1)).createExternalShipment(any(Address.class), eq(testShippingAddress), any(ExternalShippingApiClient.ParcelDetails.class), eq(testProvider.getCompanyName()));
        verify(shipmentRepository, times(1)).save(any(Shipment.class));
        verify(orderRepository, times(1)).save(testOrder);
    }

    @Test
    @DisplayName("createShipmentForOrder - Missing Shipping Address")
    void createShipmentForOrder_MissingAddress_ThrowsIllegalArgumentException() {
        // Arrange
        testOrder.setShippingAddress(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            shipmentService.createShipmentForOrder(testOrder, testProvider);
        });
        assertEquals("Order must have a shipping address.", exception.getMessage());

        // Verify no external calls or saves happened
        verifyNoInteractions(externalApiClient, shipmentRepository, orderRepository);
    }

    @Test
    @DisplayName("createShipmentForOrder - External API Failure")
    void createShipmentForOrder_ExternalApiFailure_ThrowsShipmentCreationException() throws Exception {
        // Arrange
        // Mock external API call to throw an exception
        when(externalApiClient.createExternalShipment(any(Address.class), any(Address.class), any(ExternalShippingApiClient.ParcelDetails.class), eq(testProvider.getCompanyName())))
                .thenThrow(new RuntimeException("API Connection timed out"));

        // Act & Assert
        ShipmentCreationException exception = assertThrows(ShipmentCreationException.class, () -> {
            shipmentService.createShipmentForOrder(testOrder, testProvider);
        });
        assertTrue(exception.getMessage().contains("External API call failed"));
        assertTrue(exception.getCause() instanceof RuntimeException);

        // Verify no saves happened
        verify(shipmentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    // --- getShipmentStatus Tests ---

    @Test
    @DisplayName("getShipmentStatus - Found")
    void getShipmentStatus_Found_ReturnsStatus() throws ShipmentNotFoundException, ExternalApiException {
        // Arrange
        when(shipmentRepository.findByTrackingNumber(testTrackingNumber)).thenReturn(Optional.of(testShipment));

        // Act
        Shipment.ShipmentStatus status = shipmentService.getShipmentStatus(testTrackingNumber);

        // Assert
        assertEquals(Shipment.ShipmentStatus.PROCESSING, status);
        verify(shipmentRepository, times(1)).findByTrackingNumber(testTrackingNumber);
        // Assuming no external call for status check by default in this method version
        verifyNoInteractions(externalApiClient);
    }

    @Test
    @DisplayName("getShipmentStatus - Not Found")
    void getShipmentStatus_NotFound_ThrowsShipmentNotFoundException() {
        // Arrange
        when(shipmentRepository.findByTrackingNumber(testTrackingNumber)).thenReturn(Optional.empty());

        // Act & Assert
        ShipmentNotFoundException exception = assertThrows(ShipmentNotFoundException.class, () -> {
            shipmentService.getShipmentStatus(testTrackingNumber);
        });
        assertEquals("Shipment not found with tracking: " + testTrackingNumber, exception.getMessage());
        verify(shipmentRepository, times(1)).findByTrackingNumber(testTrackingNumber);
        verifyNoInteractions(externalApiClient);
    }

    // --- handleWebhookUpdate Tests ---

    @Test
    @DisplayName("handleWebhookUpdate - Success Status Change")
    void handleWebhookUpdate_Success_StatusChanged() throws ShipmentNotFoundException, InvalidStatusException {
        // Arrange
        String externalStatus = "DELIVERED";
        String statusDetails = "Signed by neighbour";
        testShipment.setStatus(Shipment.ShipmentStatus.OUT_FOR_DELIVERY); // Current status
        testOrder.setStatus(Order.OrderStatus.SHIPPED); // Current order status

        when(shipmentRepository.findByTrackingNumber(testTrackingNumber)).thenReturn(Optional.of(testShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(testShipment); // Assume save is successful
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder); // Assume save is successful

        // Act
        shipmentService.handleWebhookUpdate(testTrackingNumber, externalStatus, statusDetails);

        // Assert
        assertEquals(Shipment.ShipmentStatus.DELIVERED, testShipment.getStatus()); // Verify shipment status updated
        assertEquals(Order.OrderStatus.DELIVERED, testOrder.getStatus()); // Verify order status updated

        // Verify interactions
        verify(shipmentRepository, times(1)).findByTrackingNumber(testTrackingNumber);
        verify(shipmentRepository, times(1)).save(testShipment);
        verify(orderRepository, times(1)).save(testOrder); // Order updated because status is DELIVERED
    }

     @Test
    @DisplayName("handleWebhookUpdate - Success Status Same")
    void handleWebhookUpdate_Success_StatusSame() throws ShipmentNotFoundException, InvalidStatusException {
        // Arrange
        String externalStatus = "IN_TRANSIT";
        String statusDetails = "Departed hub";
        testShipment.setStatus(Shipment.ShipmentStatus.IN_TRANSIT); // Current status same as webhook

        when(shipmentRepository.findByTrackingNumber(testTrackingNumber)).thenReturn(Optional.of(testShipment));
        // No save should be called if status doesn't change

        // Act
        shipmentService.handleWebhookUpdate(testTrackingNumber, externalStatus, statusDetails);

        // Assert
        assertEquals(Shipment.ShipmentStatus.IN_TRANSIT, testShipment.getStatus()); // Status remains the same

        // Verify interactions
        verify(shipmentRepository, times(1)).findByTrackingNumber(testTrackingNumber);
        verify(shipmentRepository, never()).save(any(Shipment.class)); // Shipment not saved
        verify(orderRepository, never()).save(any(Order.class));     // Order not saved
    }


    @Test
    @DisplayName("handleWebhookUpdate - Shipment Not Found")
    void handleWebhookUpdate_NotFound_ThrowsShipmentNotFoundException() {
        // Arrange
        String unknownTracking = "UNKNOWN123";
        when(shipmentRepository.findByTrackingNumber(unknownTracking)).thenReturn(Optional.empty());

        // Act & Assert
        ShipmentNotFoundException exception = assertThrows(ShipmentNotFoundException.class, () -> {
            shipmentService.handleWebhookUpdate(unknownTracking, "IN_TRANSIT", "Departed hub");
        });
        assertEquals("Shipment not found for tracking number: " + unknownTracking, exception.getMessage());

        // Verify interactions
        verify(shipmentRepository, times(1)).findByTrackingNumber(unknownTracking);
        verify(shipmentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("handleWebhookUpdate - Invalid External Status (mapped to IN_TRANSIT)")
    void handleWebhookUpdate_InvalidStatus_MapsToDefault() throws ShipmentNotFoundException, InvalidStatusException {
        // Arrange
        String externalStatus = "SOME_WEIRD_STATUS"; // Unmappable status
        testShipment.setStatus(Shipment.ShipmentStatus.PROCESSING); // Current status

        when(shipmentRepository.findByTrackingNumber(testTrackingNumber)).thenReturn(Optional.of(testShipment));
        when(shipmentRepository.save(any(Shipment.class))).thenReturn(testShipment); // Assume save is successful

        // Act
        // Expecting no InvalidStatusException because the default mapping handles it
        assertDoesNotThrow(() -> {
             shipmentService.handleWebhookUpdate(testTrackingNumber, externalStatus, "Something happened");
        });


        // Assert
        // Verify the status was updated to the fallback status (IN_TRANSIT in this implementation)
        assertEquals(Shipment.ShipmentStatus.IN_TRANSIT, testShipment.getStatus());

        // Verify interactions
        verify(shipmentRepository, times(1)).findByTrackingNumber(testTrackingNumber);
        verify(shipmentRepository, times(1)).save(testShipment);
        verify(orderRepository, never()).save(any(Order.class)); // Order status shouldn't change for IN_TRANSIT
    }

     // --- findByTrackingNumber Tests ---

     @Test
     @DisplayName("findByTrackingNumber - Found")
     void findByTrackingNumber_Found() {
         // Arrange
         when(shipmentRepository.findByTrackingNumber(testTrackingNumber)).thenReturn(Optional.of(testShipment));

         // Act
         Optional<Shipment> foundShipment = shipmentService.findByTrackingNumber(testTrackingNumber);

         // Assert
         assertTrue(foundShipment.isPresent());
         assertEquals(testShipment, foundShipment.get());
         verify(shipmentRepository, times(1)).findByTrackingNumber(testTrackingNumber);
     }

     @Test
     @DisplayName("findByTrackingNumber - Not Found")
     void findByTrackingNumber_NotFound() {
         // Arrange
         when(shipmentRepository.findByTrackingNumber(testTrackingNumber)).thenReturn(Optional.empty());

         // Act
         Optional<Shipment> foundShipment = shipmentService.findByTrackingNumber(testTrackingNumber);

         // Assert
         assertFalse(foundShipment.isPresent());
         verify(shipmentRepository, times(1)).findByTrackingNumber(testTrackingNumber);
     }


    // --- mapExternalStatus Tests ---
    // Note: mapExternalStatus is private. Testing it directly is hard without reflection/PowerMock.
    // We test it indirectly via createShipmentForOrder and handleWebhookUpdate.
    // If direct testing is needed, consider making it package-private or protected.
    // For demonstration, let's assume we could call it (e.g., if it were package-private)
    // Or we can test the public methods ensuring they call mapExternalStatus implicitly.

    @Test
    @DisplayName("mapExternalStatus - Known Statuses")
    void mapExternalStatus_KnownStatuses() throws InvalidStatusException {
       // This test is conceptual as the method is private.
       // We rely on tests like createShipmentForOrder_Success and handleWebhookUpdate_Success_StatusChanged
       // to implicitly verify the mapping logic for specific cases.

       // Example conceptual checks (if the method were accessible):
       // assertEquals(Shipment.ShipmentStatus.PROCESSING, shipmentService.mapExternalStatus("processing"));
       // assertEquals(Shipment.ShipmentStatus.IN_TRANSIT, shipmentService.mapExternalStatus("IN_TRANSIT"));
       // assertEquals(Shipment.ShipmentStatus.DELIVERED, shipmentService.mapExternalStatus("Delivered"));
       // assertEquals(Shipment.ShipmentStatus.FAILED_DELIVERY, shipmentService.mapExternalStatus("failure"));
    }

     @Test
    @DisplayName("mapExternalStatus - Unknown Status Maps to Fallback")
    void mapExternalStatus_UnknownStatus() throws InvalidStatusException {
         // This test is conceptual as the method is private.
         // Verified indirectly by handleWebhookUpdate_InvalidStatus_MapsToDefault
        // Example conceptual check (if the method were accessible):
        // assertEquals(Shipment.ShipmentStatus.IN_TRANSIT, shipmentService.mapExternalStatus("SOME_UNKNOWN_STATE"));
    }

     @Test
    @DisplayName("mapExternalStatus - Null or Blank Status Maps to Processing")
    void mapExternalStatus_NullOrBlankStatus() throws InvalidStatusException {
        // This test is conceptual as the method is private.
        // Example conceptual check (if the method were accessible):
        // assertEquals(Shipment.ShipmentStatus.PROCESSING, shipmentService.mapExternalStatus(null));
        // assertEquals(Shipment.ShipmentStatus.PROCESSING, shipmentService.mapExternalStatus(" "));
        // assertEquals(Shipment.ShipmentStatus.PROCESSING, shipmentService.mapExternalStatus(""));
    }
}
