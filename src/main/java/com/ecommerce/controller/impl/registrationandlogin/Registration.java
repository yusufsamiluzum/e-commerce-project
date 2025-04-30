package com.ecommerce.controller.impl.registrationandlogin;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.controller.IRegistrationController;
import com.ecommerce.controller.impl.RestBaseController;
import com.ecommerce.dto.DtoLoginRequest;
import com.ecommerce.entities.user.Admin;
import com.ecommerce.entities.user.Customer;
import com.ecommerce.entities.user.LogisticsProvider;
import com.ecommerce.entities.user.Seller;
import com.ecommerce.entities.user.User;
import com.ecommerce.services.IRegistrationService;



import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/rest/api/registration")
public class Registration extends RestBaseController implements IRegistrationController {

    @Autowired
    private IRegistrationService registrationService;
   
    @PostMapping("/customer")
    public Customer customerRegistration(@RequestBody Customer customer) {      
        
        return registrationService.customerRegistration(customer);
    }


    @PostMapping("/admin")
    public Admin adminRegistration(@RequestBody Admin admin) {      
        
        return registrationService.adminRegistration(admin);
    }

    @PostMapping("/logisticsProvider")
    public LogisticsProvider logisticsProviderRegistration(@RequestBody LogisticsProvider LogisticsProvider) {      
        
        return registrationService.logisticsProviderRegistration(LogisticsProvider);
    }

    @PostMapping("/seller")
    public Seller sellerRegistration(@RequestBody Seller seller) {       
        
        return registrationService.sellerRegistration(seller);
    }

    @PostMapping("/login")
    public String login(@RequestBody DtoLoginRequest loginRequest) {
        System.out.println("Login request received: " + loginRequest.getUsername());
        User user = new User();
        BeanUtils.copyProperties(loginRequest, user);
        return registrationService.verify(user);
    }
}
