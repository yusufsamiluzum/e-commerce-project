package com.ecommerce.services.impl;

import com.ecommerce.dto.DtoAddress;
import com.ecommerce.dto.DtoProfile;
import com.ecommerce.dto.DtoSetNewPassword;
import com.ecommerce.entities.user.Address;
import com.ecommerce.entities.user.User;
import com.ecommerce.repository.AddressRepository;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.services.IUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- Helper Methods for Mapping ---

    private DtoProfile mapUserToDtoProfile(User user) {
        if (user == null) return null;
        DtoProfile dto = new DtoProfile();
        BeanUtils.copyProperties(user, dto, "password"); // Exclude password
        // Ensure username is copied if it's part of DtoProfile
        dto.setUsername(user.getUsername());
        // Note: Addresses are handled by separate methods now, not included in DtoProfile
        return dto;
    }

     private DtoAddress mapAddressToDtoAddress(Address address) {
        if (address == null) return null;
        DtoAddress dto = new DtoAddress();
        BeanUtils.copyProperties(address, dto, "user"); // Exclude the back-reference to User
        return dto;
    }

    private void mapDtoAddressToAddress(DtoAddress dto, Address entity) {
         // Map fields, excluding id and user which are handled separately
         BeanUtils.copyProperties(dto, entity, "addressId", "user", "createdAt", "updatedAt");
    }

    // --- Service Method Implementations ---

    @Override
    @Transactional(readOnly = true) // Read operations
    public Optional<DtoProfile> getUserProfileById(Long userId) {
        return userRepository.findById(userId)
                .map(this::mapUserToDtoProfile); // Map User entity to DtoProfile
    }

    @Override
    @Transactional // Write operation
    public DtoProfile updateUserProfile(Long userId, DtoProfile profileUpdate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Update allowed fields from DTO
        user.setEmail(profileUpdate.getEmail());
        user.setFirstName(profileUpdate.getFirstName());
        user.setLastName(profileUpdate.getLastName());
        user.setPhoneNumber(profileUpdate.getPhoneNumber());
        user.setDateOfBirth(profileUpdate.getDateOfBirth());
        user.setSex(profileUpdate.getSex());
        // Note: username and password are not updated here

        User updatedUser = userRepository.save(user);
        return mapUserToDtoProfile(updatedUser); // Return DTO
    }

    @Override
    @Transactional
    public boolean changePassword(Long userId, DtoSetNewPassword passwordChange) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        if (!passwordEncoder.matches(passwordChange.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect old password.");
        }

        user.setPassword(passwordEncoder.encode(passwordChange.getNewPassword()));
        userRepository.save(user);
        return true;
    }

    // --- Address Management Implementation (using DTOs) ---

    @Override
    @Transactional
    public DtoAddress addAddress(Long userId, DtoAddress addressDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Address newAddress = new Address();
        mapDtoAddressToAddress(addressDto, newAddress); // Map DTO fields to new entity

        // Handle default logic
        if (newAddress.isDefault()) {
            user.getAddresses().forEach(addr -> addr.setDefault(false));
        } else if (user.getAddresses().isEmpty()) {
            newAddress.setDefault(true);
        }
        // Let isDefault from DTO override if explicitly set
        newAddress.setDefault(addressDto.isDefault());


        user.addAddress(newAddress); // Links address to user
        // userRepository.save(user); // Cascade should handle saving user if needed

        Address savedAddress = addressRepository.save(newAddress); // Save the address
        return mapAddressToDtoAddress(savedAddress); // Return the DTO representation
    }

    @Override
    @Transactional
    public DtoAddress updateAddress(Long userId, Long addressId, DtoAddress addressDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Address existingAddress = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found with ID: " + addressId));

        if (!existingAddress.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Address ID " + addressId + " does not belong to User ID " + userId);
        }

        mapDtoAddressToAddress(addressDto, existingAddress); // Update entity from DTO

        // Handle default logic when updating
        if (existingAddress.isDefault()) {
            user.getAddresses().stream()
                .filter(addr -> !addr.getAddressId().equals(addressId))
                .forEach(addr -> addr.setDefault(false));
             // Ensure other addresses are saved if their default status changed
             addressRepository.saveAll(user.getAddresses().stream()
                                     .filter(a -> !a.getAddressId().equals(addressId)).toList());

        } else {
            // If this address is being set TO default, unset others
             if (addressDto.isDefault()) {
                 user.getAddresses().stream()
                    .filter(addr -> !addr.getAddressId().equals(addressId))
                    .forEach(addr -> addr.setDefault(false));
                 // Ensure other addresses are saved if their default status changed
                addressRepository.saveAll(user.getAddresses().stream()
                                     .filter(a -> !a.getAddressId().equals(addressId)).toList());
             }
            // Optional: Check if this leaves no default address, handle if necessary
        }
         existingAddress.setDefault(addressDto.isDefault()); // Make sure DTO value is set

        Address updatedAddress = addressRepository.save(existingAddress);
        return mapAddressToDtoAddress(updatedAddress); // Return DTO
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
         User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Address addressToDelete = addressRepository.findById(addressId)
             .filter(addr -> addr.getUser().getUserId().equals(userId)) // Ensure address belongs to user
             .orElseThrow(() -> new RuntimeException("Address not found with ID: " + addressId + " for User ID: " + userId));


        if (user.getAddresses().size() <= 1) {
             throw new RuntimeException("Cannot delete the last remaining address.");
        }

        boolean wasDefault = addressToDelete.isDefault();

        // Use orphan removal by removing from the collection
        user.getAddresses().remove(addressToDelete);
        addressToDelete.setUser(null); // Optional explicit break

        // If the deleted address was the default, promote another one
        if (wasDefault) {
             user.getAddresses().stream().findFirst().ifPresent(newDefault -> {
                 newDefault.setDefault(true);
                 addressRepository.save(newDefault); // Save the new default
             });
        }

        userRepository.save(user); // Save user to trigger orphan removal of the address
        // addressRepository.delete(addressToDelete); // Alternatively, delete directly if cascade/orphanRemoval isn't used
    }

    @Override
    @Transactional(readOnly = true)
    public List<DtoAddress> getUserAddresses(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Map the list of Address entities to DtoAddress objects
        return Optional.ofNullable(user.getAddresses())
                       .orElse(Collections.emptyList()) // Handle null list if possible
                       .stream()
                       .map(this::mapAddressToDtoAddress)
                       .collect(Collectors.toList());
    }

     @Override
     @Transactional
     public List<DtoAddress> setDefaultAddress(Long userId, Long addressId) {
         User user = userRepository.findById(userId)
                 .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

         Address targetAddress = user.getAddresses().stream()
                 .filter(addr -> addr.getAddressId().equals(addressId))
                 .findFirst()
                 .orElseThrow(() -> new RuntimeException("Address ID " + addressId + " not found for User ID " + userId));

         user.getAddresses().forEach(addr -> addr.setDefault(addr.getAddressId().equals(addressId)));

         addressRepository.saveAll(user.getAddresses()); // Save all addresses in the collection

         // Return the updated list as DTOs
         return user.getAddresses().stream()
                 .map(this::mapAddressToDtoAddress)
                 .collect(Collectors.toList());
     }
}