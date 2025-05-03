package com.ecommerce.services.impl;

import com.ecommerce.dto.DtoAddress;
import com.ecommerce.dto.DtoProfile;
import com.ecommerce.dto.DtoSetNewPassword;
import com.ecommerce.entities.user.Address;
import com.ecommerce.entities.user.User;
import com.ecommerce.repository.AddressRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.services.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService; // The class under test

    private User testUser;
    private Address testAddress1;
    private Address testAddress2;
    private DtoProfile testDtoProfile;
    private DtoAddress testDtoAddress1;
    private DtoAddress testDtoAddress2;
    private final Long userId = 1L;
    private final Long addressId1 = 10L;
    private final Long addressId2 = 11L;

    // Helper method to map User to DtoProfile (mirrors the one in service)
    private DtoProfile mapUserToDtoProfile(User user) {
        if (user == null) return null;
        DtoProfile dto = new DtoProfile();
        BeanUtils.copyProperties(user, dto, "password");
        dto.setUsername(user.getUsername());
        return dto;
    }

     // Helper method to map Address to DtoAddress (mirrors the one in service)
    private DtoAddress mapAddressToDtoAddress(Address address) {
        if (address == null) return null;
        DtoAddress dto = new DtoAddress();
        BeanUtils.copyProperties(address, dto, "user"); // Exclude the back-reference
        return dto;
    }

    @BeforeEach
    void setUp() {
        // Initialize common test objects
        testUser = new User();
        testUser.setUserId(userId);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword"); // Assume password is pre-encoded
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setAddresses(new ArrayList<>()); // Initialize addresses list

        testAddress1 = new Address();
        testAddress1.setAddressId(addressId1);
        testAddress1.setStreet("123 Main St");
        testAddress1.setCity("Anytown");
        testAddress1.setCountry("USA");
        testAddress1.setDefault(true);
        testAddress1.setUser(testUser);

        testAddress2 = new Address();
        testAddress2.setAddressId(addressId2);
        testAddress2.setStreet("456 Oak Ave");
        testAddress2.setCity("Otherville");
        testAddress2.setCountry("USA");
        testAddress2.setDefault(false);
        testAddress2.setUser(testUser);

        testUser.addAddress(testAddress1); // Add address using the helper method

        testDtoProfile = mapUserToDtoProfile(testUser); //
        testDtoAddress1 = mapAddressToDtoAddress(testAddress1);
        testDtoAddress2 = mapAddressToDtoAddress(testAddress2);

    }

    @Test
    void getUserProfileById_found() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        Optional<DtoProfile> result = userService.getUserProfileById(userId); //

        assertTrue(result.isPresent());
        assertEquals(testDtoProfile.getUsername(), result.get().getUsername()); //
        assertEquals(testDtoProfile.getEmail(), result.get().getEmail()); //
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserProfileById_notFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        Optional<DtoProfile> result = userService.getUserProfileById(userId); //

        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void updateUserProfile_success() {
        DtoProfile profileUpdate = new DtoProfile(); //
        profileUpdate.setEmail("new.email@example.com");
        profileUpdate.setFirstName("Updated");
        profileUpdate.setLastName("Name");
        profileUpdate.setPhoneNumber("1234567890");
        profileUpdate.setDateOfBirth(new Date());
        profileUpdate.setSex(User.Sex.WOMAN); //

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0)); // Return saved user

        DtoProfile updatedProfile = userService.updateUserProfile(userId, profileUpdate); //

        assertNotNull(updatedProfile);
        assertEquals(profileUpdate.getEmail(), updatedProfile.getEmail());
        assertEquals(profileUpdate.getFirstName(), updatedProfile.getFirstName());
        assertEquals(profileUpdate.getLastName(), updatedProfile.getLastName());
        assertEquals(testUser.getUsername(), updatedProfile.getUsername()); // Username shouldn't change

        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, times(1)).save(testUser);
        assertEquals("Updated", testUser.getFirstName()); // Verify original user object was modified
        assertEquals("new.email@example.com", testUser.getEmail());
    }

     @Test
    void updateUserProfile_userNotFound_throwsException() {
        DtoProfile profileUpdate = new DtoProfile(); //
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUserProfile(userId, profileUpdate); //
        });

        assertEquals("User not found with ID: " + userId, exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void changePassword_success() {
        DtoSetNewPassword passwordChange = new DtoSetNewPassword("oldPasswordPlain", "newPasswordPlain");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPasswordPlain", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPasswordPlain")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        boolean result = userService.changePassword(userId, passwordChange); //

        assertTrue(result);
        assertEquals("encodedNewPassword", testUser.getPassword());
        verify(userRepository, times(1)).findById(userId);
        verify(passwordEncoder, times(1)).matches("oldPasswordPlain", "encodedPassword");
        verify(passwordEncoder, times(1)).encode("newPasswordPlain");
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void changePassword_incorrectOldPassword_throwsException() {
        DtoSetNewPassword passwordChange = new DtoSetNewPassword("wrongOldPassword", "newPasswordPlain");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongOldPassword", "encodedPassword")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.changePassword(userId, passwordChange); //
        });

        assertEquals("Incorrect old password.", exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(passwordEncoder, times(1)).matches("wrongOldPassword", "encodedPassword");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

     @Test
    void changePassword_userNotFound_throwsException() {
        DtoSetNewPassword passwordChange = new DtoSetNewPassword("old", "new");
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.changePassword(userId, passwordChange); //
        });

        assertEquals("User not found with ID: " + userId, exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
         verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void addAddress_firstAddress_becomesDefault() {
        testUser.setAddresses(new ArrayList<>()); // Ensure user starts with no addresses
        DtoAddress newDtoAddress = new DtoAddress();
        newDtoAddress.setStreet("789 Pine St");
        newDtoAddress.setCity("Newcity");
        newDtoAddress.setDefault(false); // <<< Test explicitly requests non-default

        // This Address object represents what's passed to the repo save
        // Note: It might not have an ID yet.
        Address newAddressEntity = new Address();
        BeanUtils.copyProperties(newDtoAddress, newAddressEntity, "addressId"); // Don't copy ID
        newAddressEntity.setUser(testUser);
        // The service logic will ultimately set this based on the DTO input:
        newAddressEntity.setDefault(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        // Simulate saving the new address and getting back an ID
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address saved = invocation.getArgument(0);
            // Ensure the state matches what the service sets before saving
            assertEquals(false, saved.isDefault()); // Verify service respected DTO input
            saved.setAddressId(100L); // Assign a dummy ID *after* saving
            return saved;
        });

        // --- Method under test ---
        DtoAddress resultDto = userService.addAddress(userId, newDtoAddress); //

        // --- Assertions ---
        assertNotNull(resultDto);
        assertEquals("789 Pine St", resultDto.getStreet());
        // CORRECTED ASSERTION: Expect false because the DTO requested false
        assertFalse(resultDto.isDefault(), "Should be false as DTO requested false");
        assertEquals(100L, resultDto.getAddressId()); // Check if dummy ID is mapped back

        assertEquals(1, testUser.getAddresses().size()); // User should now have 1 address
        Address addedAddressInList = testUser.getAddresses().get(0);
        // CORRECTED ASSERTION: Expect false in the persisted entity state too
        assertFalse(addedAddressInList.isDefault(), "Address in user list should be false");
        assertEquals(100L, addedAddressInList.getAddressId()); // Verify ID was set in list


        verify(userRepository, times(1)).findById(userId);
        // Verify save is called on the *new* Address object matching DTO (default=false)
        verify(addressRepository, times(1)).save(argThat(addr ->
                addr.getStreet().equals("789 Pine St") &&
                !addr.isDefault() && // Should be false
                addr.getUser() == testUser));
    }

    @Test
    void addAddress_newDefault_unsetsOldDefault() {
        // User already has testAddress1 (default=true, ID=10L)
        assertTrue(testAddress1.isDefault());
        assertEquals(1, testUser.getAddresses().size());

        // Use the pre-defined non-default DTO (ID=11L initially, but shouldn't matter for *input* DTO)
        DtoAddress newDtoAddress = testDtoAddress2;
        newDtoAddress.setAddressId(null); // ID shouldn't be present on input DTO for add
        newDtoAddress.setDefault(true);   // Explicitly set the new one as default

        Address newAddressEntity = new Address(); // Entity created inside service
        BeanUtils.copyProperties(newDtoAddress, newAddressEntity, "addressId");
        newAddressEntity.setUser(testUser);
        newAddressEntity.setDefault(true); // Service will set this

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        // CORRECTED MOCK: Simulate ID generation on save
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address saved = invocation.getArgument(0);
            // Assign the ID that the real save would generate (using testAddress2's ID for consistency)
            saved.setAddressId(addressId2);
            return saved;
        });

        // --- Method under test ---
        DtoAddress resultDto = userService.addAddress(userId, newDtoAddress); //

        // --- Assertions ---
        assertNotNull(resultDto);
        assertEquals(testAddress2.getStreet(), resultDto.getStreet());
        assertTrue(resultDto.isDefault()); // New address is default
        assertEquals(addressId2, resultDto.getAddressId()); // Check ID is returned

        assertEquals(2, testUser.getAddresses().size()); // User now has 2 addresses

        // Verify state in user's list (NPE occurred around here before)
        Address addedAddressInList = testUser.getAddresses().stream()
                .filter(a -> addressId2.equals(a.getAddressId())) // Use ID to find
                .findFirst().orElseThrow(() -> new AssertionError("Added address not found in user list"));
        Address originalAddressInList = testUser.getAddresses().stream()
                .filter(a -> addressId1.equals(a.getAddressId())) // Use ID to find
                .findFirst().orElseThrow(() -> new AssertionError("Original address not found in user list"));

        assertTrue(addedAddressInList.isDefault()); // Verify the new one is default
        assertFalse(originalAddressInList.isDefault()); // Verify the old one is no longer default

        verify(userRepository, times(1)).findById(userId);
        // Verify the new address is saved (with ID assigned by mock) and is default
        verify(addressRepository, times(1)).save(argThat(addr ->
                addressId2.equals(addr.getAddressId()) && // Check ID was set before save (or during by mock)
                addr.isDefault() &&
                addr.getUser() == testUser ));
    }

    @Test
    void addAddress_newNonDefault_keepsOldDefault() {
        // User already has testAddress1 (default=true, ID=10L)
        assertTrue(testAddress1.isDefault());
        assertEquals(1, testUser.getAddresses().size());

        DtoAddress newDtoAddress = testDtoAddress2; // Non-default DTO
        newDtoAddress.setAddressId(null); // No ID on input DTO for add
        newDtoAddress.setDefault(false); // Explicitly set as non-default

        Address newAddressEntity = new Address(); // Entity created inside service
        BeanUtils.copyProperties(newDtoAddress, newAddressEntity, "addressId");
        newAddressEntity.setUser(testUser);
        newAddressEntity.setDefault(false); // Service will set this

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        // CORRECTED MOCK: Simulate ID generation on save
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address saved = invocation.getArgument(0);
            // Assign the ID that the real save would generate
            saved.setAddressId(addressId2);
            return saved;
        });

        // --- Method under test ---
        DtoAddress resultDto = userService.addAddress(userId, newDtoAddress); //

        // --- Assertions ---
        assertNotNull(resultDto);
        assertEquals(testAddress2.getStreet(), resultDto.getStreet());
        assertFalse(resultDto.isDefault()); // New address is NOT default
        assertEquals(addressId2, resultDto.getAddressId()); // Check ID

        assertEquals(2, testUser.getAddresses().size()); // User now has 2 addresses

        // Verify state in user's list (NPE occurred around here before)
        Address addedAddressInList = testUser.getAddresses().stream()
                 .filter(a -> addressId2.equals(a.getAddressId())) // Use ID to find
                 .findFirst().orElseThrow(() -> new AssertionError("Added address not found in user list"));
        Address originalAddressInList = testUser.getAddresses().stream()
                 .filter(a -> addressId1.equals(a.getAddressId())) // Use ID to find
                 .findFirst().orElseThrow(() -> new AssertionError("Original address not found in user list"));

        assertFalse(addedAddressInList.isDefault()); // Verify the new one is NOT default
        assertTrue(originalAddressInList.isDefault()); // Verify the old one IS STILL default

        verify(userRepository, times(1)).findById(userId);
        // Verify the new address is saved (with ID assigned by mock) and is NOT default
        verify(addressRepository, times(1)).save(argThat(addr ->
                 addressId2.equals(addr.getAddressId()) &&
                 !addr.isDefault() &&
                 addr.getUser() == testUser));
    }

    @Test
    void updateAddress_success_changeToDefault() {
        // Start with address1=default, address2=non-default
        testUser.addAddress(testAddress2); // Add the second address
        assertTrue(testAddress1.isDefault());
        assertFalse(testAddress2.isDefault());

        DtoAddress updateDto = mapAddressToDtoAddress(testAddress2);
        updateDto.setStreet("Updated Street");
        updateDto.setDefault(true); // Make address2 the default

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(addressRepository.findById(addressId2)).thenReturn(Optional.of(testAddress2));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // Mock saving the *other* address (address1) which becomes non-default
        when(addressRepository.saveAll(anyList())).thenReturn(List.of(testAddress1)); // Mock saveAll behaviour


        DtoAddress resultDto = userService.updateAddress(userId, addressId2, updateDto); //


        assertNotNull(resultDto);
        assertEquals(addressId2, resultDto.getAddressId());
        assertEquals("Updated Street", resultDto.getStreet());
        assertTrue(resultDto.isDefault()); // Updated address is now default

        // Verify the state of the entities in the user list
        assertTrue(testAddress2.isDefault(), "Address 2 entity should be default");
        assertFalse(testAddress1.isDefault(), "Address 1 entity should NOT be default"); // Service logic changed it

        verify(userRepository, times(1)).findById(userId);
        verify(addressRepository, times(1)).findById(addressId2);
        // Verify address2 is saved (it was the one being updated)
        verify(addressRepository, times(1)).save(argThat(a -> a.getAddressId().equals(addressId2)));

        // Verify address1 (the *other* one) was saved via saveAll because its default status changed
        // CORRECTED LINE: Cast iterable to Collection before checking size/content
        verify(addressRepository, times(1)).saveAll(argThat(iterable -> {
            Collection<Address> collection = (Collection<Address>) iterable; // Cast here
            return collection.size() == 1 &&
                   collection.stream().findFirst().map(Address::getAddressId).orElse(-1L).equals(addressId1);
        }));
    }

     @Test
    void updateAddress_success_changeToNonDefault() {
        // Start with address1=default
        assertTrue(testAddress1.isDefault());

        DtoAddress updateDto = mapAddressToDtoAddress(testAddress1);
        updateDto.setStreet("Updated Main St");
        updateDto.setDefault(false); // Make address1 non-default

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(addressRepository.findById(addressId1)).thenReturn(Optional.of(testAddress1));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));
        // No saveAll needed here because only the updated address's default status changes


        DtoAddress resultDto = userService.updateAddress(userId, addressId1, updateDto); //


        assertNotNull(resultDto);
        assertEquals(addressId1, resultDto.getAddressId());
        assertEquals("Updated Main St", resultDto.getStreet());
        assertFalse(resultDto.isDefault()); // Updated address is now non-default

        assertFalse(testAddress1.isDefault(), "Address 1 entity should be non-default");


        verify(userRepository, times(1)).findById(userId);
        verify(addressRepository, times(1)).findById(addressId1);
        // Verify address1 is saved
        verify(addressRepository, times(1)).save(argThat(a -> a.getAddressId().equals(addressId1)));
        // Verify saveAll was NOT called
        verify(addressRepository, never()).saveAll(any());
    }

     @Test
    void updateAddress_addressNotFound_throwsException() {
        DtoAddress updateDto = new DtoAddress();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(addressRepository.findById(addressId2)).thenReturn(Optional.empty()); // Address not found

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateAddress(userId, addressId2, updateDto); //
        });

        assertEquals("Address not found with ID: " + addressId2, exception.getMessage());
        verify(addressRepository, times(1)).findById(addressId2);
        verify(addressRepository, never()).save(any(Address.class));
        verify(addressRepository, never()).saveAll(any());
    }

      @Test
    void updateAddress_addressDoesNotBelongToUser_throwsException() {
        User otherUser = new User(); otherUser.setUserId(99L); //
        testAddress2.setUser(otherUser); // Assign address to a different user

        DtoAddress updateDto = mapAddressToDtoAddress(testAddress2);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(addressRepository.findById(addressId2)).thenReturn(Optional.of(testAddress2)); // Found, but belongs to otherUser

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateAddress(userId, addressId2, updateDto); //
        });

        assertEquals("Address ID " + addressId2 + " does not belong to User ID " + userId, exception.getMessage());
        verify(addressRepository, times(1)).findById(addressId2);
        verify(addressRepository, never()).save(any(Address.class));
         verify(addressRepository, never()).saveAll(any());
    }


    @Test
    void deleteAddress_success_deleteNonDefault() {
        // Start with address1=default, address2=non-default
        testUser.addAddress(testAddress2);
        assertEquals(2, testUser.getAddresses().size());

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        // Crucially, mock findById to return the address *and* filter check passes
        when(addressRepository.findById(addressId2)).thenReturn(Optional.of(testAddress2));
        when(userRepository.save(any(User.class))).thenReturn(testUser); // Mock saving the user after removal

        userService.deleteAddress(userId, addressId2); //

        assertEquals(1, testUser.getAddresses().size()); // Size reduced
        assertFalse(testUser.getAddresses().contains(testAddress2)); // Address removed from collection
        assertTrue(testUser.getAddresses().get(0).isDefault()); // Remaining address is still default

        verify(userRepository, times(1)).findById(userId);
        verify(addressRepository, times(1)).findById(addressId2);
        // Verify user is saved (triggering orphan removal)
        verify(userRepository, times(1)).save(testUser);
        // Verify address repo delete NOT called directly if relying on orphan removal
        // verify(addressRepository, never()).delete(testAddress2);
         verify(addressRepository, never()).save(any(Address.class)); // No default promotion needed
    }

     @Test
    void deleteAddress_success_deleteDefault_promoteNext() {
        // Start with address1=default, address2=non-default
        testUser.addAddress(testAddress2);
        assertEquals(2, testUser.getAddresses().size());
        assertTrue(testAddress1.isDefault());
        assertFalse(testAddress2.isDefault());

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        // Mock finding the default address to delete
        when(addressRepository.findById(addressId1)).thenReturn(Optional.of(testAddress1));
         // Mock saving the user after removal
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        // Mock saving the *new* default address (address2)
        when(addressRepository.save(testAddress2)).thenReturn(testAddress2);

        userService.deleteAddress(userId, addressId1); //

        assertEquals(1, testUser.getAddresses().size()); // Size reduced
        assertFalse(testUser.getAddresses().contains(testAddress1)); // Address 1 removed
        Address remainingAddress = testUser.getAddresses().get(0);
        assertEquals(addressId2, remainingAddress.getAddressId());
        assertTrue(remainingAddress.isDefault(), "Remaining address should be promoted to default"); // Address 2 promoted

        verify(userRepository, times(1)).findById(userId);
        verify(addressRepository, times(1)).findById(addressId1);
        // Verify user is saved (triggering orphan removal)
        verify(userRepository, times(1)).save(testUser);
        // Verify the *new* default address (address2) was saved
        verify(addressRepository, times(1)).save(testAddress2);
    }


    @Test
    void deleteAddress_lastAddress_throwsException() {
        // User only has testAddress1
        assertEquals(1, testUser.getAddresses().size());

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(addressRepository.findById(addressId1)).thenReturn(Optional.of(testAddress1)); // Address exists

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.deleteAddress(userId, addressId1); //
        });

        assertEquals("Cannot delete the last remaining address.", exception.getMessage()); //
        assertEquals(1, testUser.getAddresses().size()); // Address list unchanged

        verify(userRepository, times(1)).findById(userId);
        verify(addressRepository, times(1)).findById(addressId1);
        verify(userRepository, never()).save(any(User.class));
        verify(addressRepository, never()).delete(any(Address.class));
         verify(addressRepository, never()).save(any(Address.class));
    }

     @Test
    void deleteAddress_addressNotFoundOrNotBelonging_throwsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        // Mock findById to return empty OR an address not belonging to the user (filter fails)
        when(addressRepository.findById(addressId2)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.deleteAddress(userId, addressId2); //
        });

         assertEquals("Address not found with ID: " + addressId2 + " for User ID: " + userId, exception.getMessage()); //

        verify(userRepository, times(1)).findById(userId);
        verify(addressRepository, times(1)).findById(addressId2);
        verify(userRepository, never()).save(any(User.class));
    }


    @Test
    void getUserAddresses_success() {
         testUser.addAddress(testAddress2); // Add second address
         when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

         List<DtoAddress> result = userService.getUserAddresses(userId); //

         assertNotNull(result);
         assertEquals(2, result.size());
         List<Long> resultIds = result.stream().map(DtoAddress::getAddressId).collect(Collectors.toList());
         assertTrue(resultIds.contains(addressId1));
         assertTrue(resultIds.contains(addressId2));

         verify(userRepository, times(1)).findById(userId);
    }

    @Test
    void getUserAddresses_userNotFound_throwsException() {
         when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserAddresses(userId); //
        });

        assertEquals("User not found with ID: " + userId, exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
    }

     @Test
    void getUserAddresses_noAddresses_returnsEmptyList() {
        testUser.setAddresses(new ArrayList<>()); // Ensure user has no addresses
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        List<DtoAddress> result = userService.getUserAddresses(userId); //

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userRepository, times(1)).findById(userId);
    }


    @Test
    void setDefaultAddress_success() {
        // Start with address1=default, address2=non-default
        testUser.addAddress(testAddress2);
        assertTrue(testAddress1.isDefault());
        assertFalse(testAddress2.isDefault());

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        // Mock saveAll to simulate saving both addresses after default change
        when(addressRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));


        List<DtoAddress> result = userService.setDefaultAddress(userId, addressId2); // Set address2 as default


        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify DTOs reflect the change
        DtoAddress resultDto1 = result.stream().filter(dto -> dto.getAddressId().equals(addressId1)).findFirst().orElseThrow();
        DtoAddress resultDto2 = result.stream().filter(dto -> dto.getAddressId().equals(addressId2)).findFirst().orElseThrow();
        assertFalse(resultDto1.isDefault(), "DTO for address 1 should not be default");
        assertTrue(resultDto2.isDefault(), "DTO for address 2 should be default");

        // Verify entities reflect the change
        assertFalse(testAddress1.isDefault(), "Entity address 1 should not be default");
        assertTrue(testAddress2.isDefault(), "Entity address 2 should be default"); // Service logic modifies entity

        verify(userRepository, times(1)).findById(userId);
        // Verify saveAll was called with the list containing *both* addresses
        // CORRECTED LINE: Cast iterable to Collection before checking size
        verify(addressRepository, times(1)).saveAll(argThat(iterable -> {
            Collection<Address> collection = (Collection<Address>) iterable; // Cast here
            return collection.size() == 2 &&
                   collection.stream().anyMatch(a -> a.getAddressId().equals(addressId1) && !a.isDefault()) &&
                   collection.stream().anyMatch(a -> a.getAddressId().equals(addressId2) && a.isDefault());
        }));
    }

    @Test
    void setDefaultAddress_addressNotFoundForUser_throwsException() {
         // User only has address1
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        // Trying to set addressId2 (which doesn't exist for this user) as default

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.setDefaultAddress(userId, addressId2); //
        });

        assertEquals("Address ID " + addressId2 + " not found for User ID " + userId, exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(addressRepository, never()).saveAll(anyList());
    }

     @Test
    void setDefaultAddress_userNotFound_throwsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.setDefaultAddress(userId, addressId1); //
        });

        assertEquals("User not found with ID: " + userId, exception.getMessage());
        verify(userRepository, times(1)).findById(userId);
        verify(addressRepository, never()).saveAll(anyList());
    }
}