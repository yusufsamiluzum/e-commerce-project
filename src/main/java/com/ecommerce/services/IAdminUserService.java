package com.ecommerce.services;

import com.ecommerce.dto.DtoUserSummary;
import com.ecommerce.dto.DtoProfile; // Using DtoProfile for richer info on ban/unban response
// import com.ecommerce.entities.user.User;
import java.util.List;

public interface IAdminUserService {

    /**
     * Retrieves a list of all users summaries[cite: 3].
     *
     * @return A list of DtoUserSummary objects[cite: 3].
     */
    List<DtoUserSummary> getAllUsers();

    /**
     * Bans a user account by setting their status.
     *
     * @param userId The ID of the user to ban.
     * @return DtoProfile of the banned user[cite: 2].
     * @throws RuntimeException if the user is not found.
     */
    DtoProfile banUser(Long userId); // Returns DtoProfile

    /**
     * Unbans a user account by setting their status to ACTIVE.
     *
     * @param userId The ID of the user to unban.
     * @return DtoProfile of the unbanned user[cite: 2].
     * @throws RuntimeException if the user is not found.
     */
    DtoProfile unbanUser(Long userId); // Returns DtoProfile

    /**
     * Deletes a user account (Hard Delete). Use with caution.
     *
     * @param userId The ID of the user to delete.
     * @return boolean indicating success.
     */
    boolean deleteUser(Long userId);

}
