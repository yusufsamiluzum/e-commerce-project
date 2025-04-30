package com.ecommerce.services.impl;


import com.ecommerce.dto.DtoProfile;
import com.ecommerce.dto.DtoUserSummary;
import com.ecommerce.entities.user.User;
import com.ecommerce.entities.user.User.UserStatus;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.services.IAdminUserService;
import org.springframework.beans.BeanUtils; // Import BeanUtils
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminUserServiceImpl implements IAdminUserService {

    @Autowired
    private UserRepository userRepository;

    // Helper to map User to DtoProfile (could be shared or defined here)
    private DtoProfile mapUserToDtoProfile(User user) {
        if (user == null) return null;
        DtoProfile dto = new DtoProfile();
        BeanUtils.copyProperties(user, dto, "password", "addresses"); // Exclude sensitive/complex fields
         dto.setUsername(user.getUsername());
        return dto;
    }


    @Override
    @Transactional(readOnly = true)
    public List<DtoUserSummary> getAllUsers() {
        return userRepository.findAll().stream()
                 .map(user -> new DtoUserSummary(user.getUserId(), user.getUsername(), user.getFirstName(), user.getLastName()))
                 .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public DtoProfile banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        user.setStatus(UserStatus.BANNED);
        User updatedUser = userRepository.save(user);
        return mapUserToDtoProfile(updatedUser); // Return DtoProfile
    }

    @Override
    @Transactional
    public DtoProfile unbanUser(Long userId) {
         User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        user.setStatus(UserStatus.ACTIVE);
        User updatedUser = userRepository.save(user);
        return mapUserToDtoProfile(updatedUser); // Return DtoProfile
    }

    @Override
    @Transactional
    public boolean deleteUser(Long userId) {
         if (!userRepository.existsById(userId)) {
             throw new RuntimeException("User not found with ID: " + userId);
         }
         // Performing hard delete
         userRepository.deleteById(userId);
         return true;
    }
}