package com.ecommerce.controller.impl; // Or your preferred controller package

import com.ecommerce.dto.DtoAddress;
import com.ecommerce.dto.DtoProfile;
import com.ecommerce.dto.DtoSetNewPassword;
import com.ecommerce.services.IUserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// Import PreAuthorize annotation
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for handling user profile and address related operations.
 * Security is enforced using @PreAuthorize annotations.
 * Assumes the authentication principal has an 'id' property matching the Long userId.
 */
@RestController
@RequestMapping("/api/users") // Base path for user-related endpoints
public class UserController {

    @Autowired
    private IUserService userService;

    // --- Profile Endpoints ---

    /**
     * Gets the profile information for the specified user.
     * Security: Allows access if the authenticated user's ID matches the path variable userId,
     * OR if the user has the 'ADMIN' role.
     *
     * @param userId The ID of the user whose profile is requested.
     * @return ResponseEntity containing DtoProfile or 404 Not Found.
     */
    @GetMapping("/{userId}/profile")
    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<DtoProfile> getUserProfile(@PathVariable Long userId) {
        Optional<DtoProfile> profileOpt = userService.getUserProfileById(userId);
        return profileOpt
                .map(ResponseEntity::ok) // 200 OK with profile DTO
                .orElseGet(() -> ResponseEntity.notFound().build()); // 404 Not Found
    }

    /**
     * Updates the profile information for the specified user.
     * Security: Allows access only if the authenticated user's ID matches the path variable userId.
     *
     * @param userId The ID of the user whose profile is being updated.
     * @param profileUpdate The DTO containing the updated profile data.
     * @return ResponseEntity containing the updated DtoProfile or 404/500 error.
     */
    @PutMapping("/{userId}/profile")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<DtoProfile> updateUserProfile(@PathVariable Long userId, @RequestBody DtoProfile profileUpdate) {
        try {
            DtoProfile updatedProfile = userService.updateUserProfile(userId, profileUpdate);
            return ResponseEntity.ok(updatedProfile); // 200 OK with updated profile
        } catch (RuntimeException ex) {
             // Consider logging the exception here
             if (ex.getMessage().contains("User not found")) {
                 return ResponseEntity.notFound().build(); // 404 Not Found
             }
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
        }
    }

    /**
     * Changes the password for the specified user.
     * Security: Allows access only if the authenticated user's ID matches the path variable userId.
     *
     * @param userId The ID of the user changing their password.
     * @param passwordChange The DTO containing the old and new passwords.
     * @return ResponseEntity indicating success (204 No Content) or failure (400/404/500 error).
     */
    @PostMapping("/{userId}/password")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<Void> changePassword(@PathVariable Long userId, @RequestBody DtoSetNewPassword passwordChange) {
        try {
            boolean success = userService.changePassword(userId, passwordChange);
            if (success) {
                return ResponseEntity.noContent().build(); // 204 No Content on success
            } else {
                // This path might not be reached if exceptions are thrown for failures
                return ResponseEntity.badRequest().build(); // 400 Bad Request (generic failure)
            }
        } catch (RuntimeException ex) {
             // Consider logging the exception here
             if (ex.getMessage().contains("User not found")) {
                 return ResponseEntity.notFound().build(); // 404 Not Found
             } else if (ex.getMessage().contains("Incorrect old password")) {
                 return ResponseEntity.badRequest().build(); // 400 Bad Request (invalid old password)
             }
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
        }
    }

    // --- Address Endpoints ---

    /**
     * Gets all addresses for the specified user.
     * Security: Allows access if the authenticated user's ID matches the path variable userId,
     * OR if the user has the 'ADMIN' role.
     *
     * @param userId The ID of the user whose addresses are requested.
     * @return ResponseEntity containing a list of DtoAddress or 404/500 error.
     */
    @GetMapping("/{userId}/addresses")
    @PreAuthorize("#userId == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<List<DtoAddress>> getUserAddresses(@PathVariable Long userId) {
         try {
            List<DtoAddress> addresses = userService.getUserAddresses(userId);
            return ResponseEntity.ok(addresses); // 200 OK with list of addresses
        } catch (RuntimeException ex) {
             // Consider logging the exception here
             if (ex.getMessage().contains("User not found")) {
                 return ResponseEntity.notFound().build(); // 404 Not Found
             }
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
        }
    }

    /**
     * Adds a new address for the specified user.
     * Security: Allows access only if the authenticated user's ID matches the path variable userId.
     *
     * @param userId The ID of the user adding the address.
     * @param addressDto The DTO containing the new address details.
     * @return ResponseEntity containing the created DtoAddress (201 Created) or 404/500 error.
     */
    @PostMapping("/{userId}/addresses")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<DtoAddress> addAddress(@PathVariable Long userId, @RequestBody DtoAddress addressDto) {
        try {
            DtoAddress createdAddress = userService.addAddress(userId, addressDto);
            // Optionally, return location header: .created(URI.create("/api/users/" + userId + "/addresses/" + createdAddress.getAddressId()))
            return ResponseEntity.status(HttpStatus.CREATED).body(createdAddress); // 201 Created
        } catch (RuntimeException ex) {
             // Consider logging the exception here
             if (ex.getMessage().contains("User not found")) {
                 return ResponseEntity.notFound().build(); // 404 User Not Found
             }
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
        }
    }

    /**
     * Updates an existing address for the specified user.
     * Security: Allows access only if the authenticated user's ID matches the path variable userId.
     *
     * @param userId The ID of the user owning the address.
     * @param addressId The ID of the address to update.
     * @param addressDto The DTO containing updated address details.
     * @return ResponseEntity containing the updated DtoAddress (200 OK) or 403/404/500 error.
     */
    @PutMapping("/{userId}/addresses/{addressId}")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<DtoAddress> updateAddress(@PathVariable Long userId, @PathVariable Long addressId, @RequestBody DtoAddress addressDto) {
        try {
            // Service layer already checks if address belongs to user, but PreAuthorize ensures only the owner can call
            DtoAddress updatedAddress = userService.updateAddress(userId, addressId, addressDto);
            return ResponseEntity.ok(updatedAddress); // 200 OK
        } catch (RuntimeException ex) {
             // Consider logging the exception here
             if (ex.getMessage().contains("not found")) { // Covers user or address not found
                 return ResponseEntity.notFound().build(); // 404 Not Found
             } else if (ex.getMessage().contains("does not belong")) {
                 // This might be redundant if service check throws first, but good practice
                 return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden
             }
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
        }
    }

    /**
     * Deletes an address for the specified user.
     * Security: Allows access only if the authenticated user's ID matches the path variable userId.
     *
     * @param userId The ID of the user owning the address.
     * @param addressId The ID of the address to delete.
     * @return ResponseEntity indicating success (204 No Content) or 400/403/404/500 error.
     */
    @DeleteMapping("/{userId}/addresses/{addressId}")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        try {
             // Service layer already checks if address belongs to user, but PreAuthorize ensures only the owner can call
            userService.deleteAddress(userId, addressId);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (RuntimeException ex) {
             // Consider logging the exception here
             if (ex.getMessage().contains("not found")) {
                 return ResponseEntity.notFound().build(); // 404 Not Found
             } else if (ex.getMessage().contains("does not belong")) {
                  return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 403 Forbidden
             } else if (ex.getMessage().contains("Cannot delete the last")) {
                 return ResponseEntity.badRequest().build(); // 400 Bad Request (business rule violation)
             }
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
        }
    }

     /**
     * Sets a specific address as the default for the user.
     * Security: Allows access only if the authenticated user's ID matches the path variable userId.
     *
     * @param userId The ID of the user.
     * @param addressId The ID of the address to set as default.
     * @return ResponseEntity containing the updated list of addresses (200 OK) or 404/500 error.
     */
    @PostMapping("/{userId}/addresses/{addressId}/default")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<List<DtoAddress>> setDefaultAddress(@PathVariable Long userId, @PathVariable Long addressId) {
         try {
             // Service layer already checks if address belongs to user, but PreAuthorize ensures only the owner can call
            List<DtoAddress> updatedAddresses = userService.setDefaultAddress(userId, addressId);
            return ResponseEntity.ok(updatedAddresses); // 200 OK with updated list
        } catch (RuntimeException ex) {
             // Consider logging the exception here
             if (ex.getMessage().contains("not found")) { // Covers user or address not found
                 return ResponseEntity.notFound().build(); // 404 Not Found
             }
             // Specific check from service impl might be caught here too
             // else if (ex.getMessage().contains("not found for User")) { ... }
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
        }
    }
}
