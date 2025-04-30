package com.ecommerce.controller.impl; // Or your preferred controller package for admin functions

import com.ecommerce.dto.DtoProfile;
import com.ecommerce.dto.DtoUserSummary;
import com.ecommerce.services.IAdminUserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// Import PreAuthorize annotation
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for administrative operations related to users.
 * All endpoints in this controller require ADMIN privileges, enforced by @PreAuthorize.
 */
@RestController
@RequestMapping("/api/admin/users") // Base path for admin user management
// You could apply @PreAuthorize("hasRole('ADMIN')") at the class level
// if ALL methods require the same role. For clarity, applying per-method.
public class AdminUserController {

    @Autowired
    private IAdminUserService adminUserService;

    /**
     * Gets a list of all users (summary view).
     * Security: Requires ADMIN role.
     *
     * @return ResponseEntity containing a list of DtoUserSummary.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // Only users with ROLE_ADMIN can access
    public ResponseEntity<List<DtoUserSummary>> getAllUsers() {
        List<DtoUserSummary> users = adminUserService.getAllUsers();
        return ResponseEntity.ok(users); // 200 OK
    }

    /**
     * Bans a user by setting their status.
     * Security: Requires ADMIN role.
     *
     * @param userId The ID of the user to ban.
     * @return ResponseEntity containing the DtoProfile of the banned user or 404 Not Found.
     */
    @PostMapping("/{userId}/ban")
    @PreAuthorize("hasRole('ADMIN')") // Only users with ROLE_ADMIN can access
    public ResponseEntity<DtoProfile> banUser(@PathVariable Long userId) {
        try {
            DtoProfile bannedUserProfile = adminUserService.banUser(userId);
            return ResponseEntity.ok(bannedUserProfile); // 200 OK
        } catch (RuntimeException ex) {
            // Consider logging the exception here
            if (ex.getMessage().contains("User not found")) {
                return ResponseEntity.notFound().build(); // 404 Not Found
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
        }
    }

    /**
     * Unbans a user by setting their status back to ACTIVE.
     * Security: Requires ADMIN role.
     *
     * @param userId The ID of the user to unban.
     * @return ResponseEntity containing the DtoProfile of the unbanned user or 404 Not Found.
     */
    @PostMapping("/{userId}/unban")
    @PreAuthorize("hasRole('ADMIN')") // Only users with ROLE_ADMIN can access
    public ResponseEntity<DtoProfile> unbanUser(@PathVariable Long userId) {
         try {
            DtoProfile unbannedUserProfile = adminUserService.unbanUser(userId);
            return ResponseEntity.ok(unbannedUserProfile); // 200 OK
        } catch (RuntimeException ex) {
            // Consider logging the exception here
            if (ex.getMessage().contains("User not found")) {
                return ResponseEntity.notFound().build(); // 404 Not Found
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
        }
    }

    /**
     * Deletes a user account (Hard Delete).
     * Security: Requires ADMIN role. Use with extreme caution.
     *
     * @param userId The ID of the user to delete.
     * @return ResponseEntity indicating success (204 No Content) or failure (404 Not Found).
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')") // Only users with ROLE_ADMIN can access
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        try {
            boolean success = adminUserService.deleteUser(userId);
            if (success) {
                return ResponseEntity.noContent().build(); // 204 No Content
            } else {
                 // This might indicate an unexpected state if deleteUser returns false without exception
                 // Consider logging this scenario
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (RuntimeException ex) {
             // Consider logging the exception here
            if (ex.getMessage().contains("User not found")) {
                return ResponseEntity.notFound().build(); // 404 Not Found
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 Internal Server Error
        }
    }
}
