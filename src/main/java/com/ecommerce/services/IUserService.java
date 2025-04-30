package com.ecommerce.services;

import com.ecommerce.dto.DtoAddress;
import com.ecommerce.dto.DtoProfile;
import com.ecommerce.dto.DtoSetNewPassword;
// DTOs are used for return types now
// import com.ecommerce.entities.user.Address;
// import com.ecommerce.entities.user.User;
import java.util.List;
import java.util.Optional;

public interface IUserService {

    /**
     * Retrieves user profile information by user ID.
     *
     * @param userId The ID of the user.
     * @return An Optional containing the DtoProfile if found, otherwise empty.
     */
    Optional<DtoProfile> getUserProfileById(Long userId); // Returns DtoProfile

    /**
     * Retrieves user profile information by username.
     *
     * @param username The username of the user.
     * @return An Optional containing the DtoProfile if found, otherwise empty.
     */
   // Optional<DtoProfile> getUserProfileByUsername(String username); // Returns DtoProfile

    /**
     * Updates the profile information for a given user.
     *
     * @param userId The ID of the user to update.
     * @param profileUpdate The DTO containing updated profile data.
     * @return The updated DtoProfile.
     * @throws RuntimeException if the user is not found.
     */
    DtoProfile updateUserProfile(Long userId, DtoProfile profileUpdate); // Accepts and returns DtoProfile

    /**
     * Changes the password for a given user.
     *
     * @param userId The ID of the user changing password.
     * @param passwordChange The DTO containing old and new passwords[cite: 4].
     * @return boolean indicating if the password change was successful.
     * @throws RuntimeException if the user is not found or old password doesn't match.
     */
    boolean changePassword(Long userId, DtoSetNewPassword passwordChange); // Accepts DtoSetNewPassword

    // --- Address Management ---

    /**
     * Adds a new address for the specified user.
     *
     * @param userId The ID of the user.
     * @param addressDto The DTO containing the new address details[cite: 1].
     * @return The newly created DtoAddress[cite: 1].
     * @throws RuntimeException if the user is not found.
     */
    DtoAddress addAddress(Long userId, DtoAddress addressDto); // Accepts and returns DtoAddress

    /**
     * Updates an existing address for the specified user.
     *
     * @param userId The ID of the user (for verification).
     * @param addressId The ID of the address to update.
     * @param addressDto The DTO containing the updated address details[cite: 1].
     * @return The updated DtoAddress[cite: 1].
     * @throws RuntimeException if the user or address is not found, or address doesn't belong to user.
     */
    DtoAddress updateAddress(Long userId, Long addressId, DtoAddress addressDto); // Accepts and returns DtoAddress

    /**
     * Deletes an address associated with the user.
     *
     * @param userId The ID of the user (for verification).
     * @param addressId The ID of the address to delete.
     * @throws RuntimeException if the user or address is not found, or address doesn't belong to user.
     */
    void deleteAddress(Long userId, Long addressId); // No DTO needed for delete confirmation

    /**
     * Retrieves all addresses for a specific user as DTOs.
     *
     * @param userId The ID of the user.
     * @return A list of the user's addresses as DtoAddress objects[cite: 1].
     * @throws RuntimeException if the user is not found.
     */
    List<DtoAddress> getUserAddresses(Long userId); // Returns List<DtoAddress>

     /**
     * Sets a specific address as the default address for the user.
     *
     * @param userId The ID of the user.
     * @param addressId The ID of the address to set as default.
     * @return The updated list of user addresses as DtoAddress objects[cite: 1].
     * @throws RuntimeException if the user or address is not found, or address doesn't belong to user.
     */
    List<DtoAddress> setDefaultAddress(Long userId, Long addressId); // Returns List<DtoAddress>
}