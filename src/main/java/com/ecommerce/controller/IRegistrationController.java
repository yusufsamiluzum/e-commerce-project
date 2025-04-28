package com.ecommerce.controller;

import com.ecommerce.entities.user.Admin;
import com.ecommerce.entities.user.Customer;
import com.ecommerce.entities.user.LogisticsProvider;
import com.ecommerce.entities.user.Seller;

public interface IRegistrationController {

    public Customer customerRegistration(Customer customer);

    public Admin adminRegistration(Admin admin);
 
    public LogisticsProvider logisticsProviderRegistration(LogisticsProvider LogisticsProvider);  

    public Seller sellerRegistration(Seller seller);
  
}
