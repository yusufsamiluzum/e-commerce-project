package com.ecommerce.controller.impl;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.config.securityconfig.UserPrincipal;
import com.ecommerce.controller.IAdminController;

@RestController
@RequestMapping("/rest/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class RestAdminController implements IAdminController{

    @GetMapping("/hello")  
    @Override
    public String helloAdmin() {
        // Get the authenticated user's details
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Extract the username
        String username = userPrincipal.getUsername();
        System.out.println("Hello Admin " + username + "!");
        
        return "Hello Admin " + username + "!";
    }
}
