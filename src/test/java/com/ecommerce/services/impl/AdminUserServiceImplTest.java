package com.ecommerce.services.impl;

import com.ecommerce.dto.DtoProfile;
import com.ecommerce.dto.DtoUserSummary;
import com.ecommerce.entities.user.User;
import com.ecommerce.entities.user.User.UserStatus;
import com.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils; // Ensure BeanUtils is available or mock its behavior if needed

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User user1;
    private User user2;

    // Helper to map User to DtoProfile (copied from implementation for verification)
    private DtoProfile mapUserToDtoProfile(User user) {
        if (user == null) return null;
        DtoProfile dto = new DtoProfile();
        // Assuming DtoProfile has setters corresponding to User fields
        // Use BeanUtils or manual mapping matching the implementation
        BeanUtils.copyProperties(user, dto, "password", "addresses");
        dto.setUsername(user.getUsername()); // Explicitly set username as in impl
        // Set other fields if needed for comparison
        return dto;
    }


    @BeforeEach
    void setUp() {
        // Initialize common test data
        user1 = new User();
        user1.setUserId(1L);
        user1.setUsername("john.doe");
        user1.setFirstName("John");
        user1.setLastName("Doe");
        user1.setStatus(UserStatus.ACTIVE);
        // Set other necessary fields for user1

        user2 = new User();
        user2.setUserId(2L);
        user2.setUsername("jane.doe");
        user2.setFirstName("Jane");
        user2.setLastName("Doe");
        user2.setStatus(UserStatus.ACTIVE);
        // Set other necessary fields for user2
    }

    @Test
    void getAllUsers_shouldReturnUserSummaries() {
        // Arrange
        List<User> users = Arrays.asList(user1, user2);
        when(userRepository.findAll()).thenReturn(users);

        List<DtoUserSummary> expectedSummaries = users.stream()
                .map(user -> new DtoUserSummary(user.getUserId(), user.getUsername(), user.getFirstName(), user.getLastName()))
                .collect(Collectors.toList());

        // Act
        List<DtoUserSummary> actualSummaries = adminUserService.getAllUsers();

        // Assert
        assertNotNull(actualSummaries);
        assertEquals(expectedSummaries.size(), actualSummaries.size());
        // Optionally, compare content more deeply if constructor order/logic is complex
        assertEquals(expectedSummaries.get(0).getUserId(), actualSummaries.get(0).getUserId());
        assertEquals(expectedSummaries.get(1).getUsername(), actualSummaries.get(1).getUsername());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void banUser_shouldBanUserAndReturnProfile_whenUserExists() {
        // Arrange
        Long userIdToBan = 1L;
        when(userRepository.findById(userIdToBan)).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            // Simulate saving behavior if needed, e.g., returning the saved entity
            assertEquals(UserStatus.BANNED, savedUser.getStatus());
            return savedUser;
        });

        DtoProfile expectedProfile = mapUserToDtoProfile(user1); // Profile before status change in mock save
         expectedProfile.setStatus(UserStatus.BANNED); // Expected final status

        // Act
        DtoProfile actualProfile = adminUserService.banUser(userIdToBan);

        // Assert
        assertNotNull(actualProfile);
        assertEquals(UserStatus.BANNED, user1.getStatus()); // Verify status change on the original mock object
        assertEquals(expectedProfile.getUsername(), actualProfile.getUsername());
        assertEquals(expectedProfile.getStatus(), actualProfile.getStatus());
        // Add more assertions based on DtoProfile fields

        verify(userRepository, times(1)).findById(userIdToBan);
        verify(userRepository, times(1)).save(user1);
    }

    @Test
    void banUser_shouldThrowException_whenUserNotFound() {
        // Arrange
        Long userIdToBan = 99L;
        when(userRepository.findById(userIdToBan)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminUserService.banUser(userIdToBan);
        });
        assertEquals("User not found with ID: " + userIdToBan, exception.getMessage());
        verify(userRepository, times(1)).findById(userIdToBan);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void unbanUser_shouldUnbanUserAndReturnProfile_whenUserExists() {
        // Arrange
        Long userIdToUnban = 1L;
        user1.setStatus(UserStatus.BANNED); // Start with a banned user
        when(userRepository.findById(userIdToUnban)).thenReturn(Optional.of(user1));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
            return savedUser;
        });

         DtoProfile expectedProfile = mapUserToDtoProfile(user1); // Profile before status change in mock save
         expectedProfile.setStatus(UserStatus.ACTIVE); // Expected final status

        // Act
        DtoProfile actualProfile = adminUserService.unbanUser(userIdToUnban);

        // Assert
        assertNotNull(actualProfile);
        assertEquals(UserStatus.ACTIVE, user1.getStatus()); // Verify status change
        assertEquals(expectedProfile.getUsername(), actualProfile.getUsername());
        assertEquals(expectedProfile.getStatus(), actualProfile.getStatus());
        // Add more assertions based on DtoProfile fields

        verify(userRepository, times(1)).findById(userIdToUnban);
        verify(userRepository, times(1)).save(user1);
    }

    @Test
    void unbanUser_shouldThrowException_whenUserNotFound() {
        // Arrange
        Long userIdToUnban = 99L;
        when(userRepository.findById(userIdToUnban)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminUserService.unbanUser(userIdToUnban);
        });
        assertEquals("User not found with ID: " + userIdToUnban, exception.getMessage());
        verify(userRepository, times(1)).findById(userIdToUnban);
        verify(userRepository, never()).save(any(User.class));
    }

     @Test
    void deleteUser_shouldReturnTrue_whenUserExists() {
        // Arrange
        Long userIdToDelete = 1L;
        when(userRepository.existsById(userIdToDelete)).thenReturn(true);
        doNothing().when(userRepository).deleteById(userIdToDelete); // Mock void method

        // Act
        boolean result = adminUserService.deleteUser(userIdToDelete);

        // Assert
        assertTrue(result);
        verify(userRepository, times(1)).existsById(userIdToDelete);
        verify(userRepository, times(1)).deleteById(userIdToDelete);
    }

    @Test
    void deleteUser_shouldThrowException_whenUserNotFound() {
        // Arrange
        Long userIdToDelete = 99L;
        when(userRepository.existsById(userIdToDelete)).thenReturn(false);

        // Act & Assert
         RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adminUserService.deleteUser(userIdToDelete);
        });
        assertEquals("User not found with ID: " + userIdToDelete, exception.getMessage());

        verify(userRepository, times(1)).existsById(userIdToDelete);
        verify(userRepository, never()).deleteById(anyLong());
    }
}
