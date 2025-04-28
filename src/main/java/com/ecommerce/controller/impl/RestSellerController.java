package com.ecommerce.controller.impl;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.config.securityconfig.UserPrincipal;
import com.ecommerce.controller.ISellerController;

@RestController
@RequestMapping("/rest/api/seller")
@PreAuthorize("hasRole('SELLER')")
public class RestSellerController extends RestBaseController implements ISellerController{

    @GetMapping("/hello")
    @Override
    public String helloSeller() {
        // Get the authenticated user's details
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // Extract the username
        String username = userPrincipal.getUsername();
        System.out.println("Hello Seller " + username + "!");
        
        return "Hello Seller " + username + "!";
    }

}
