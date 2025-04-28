package com.ecommerce.services;

import com.ecommerce.entities.user.Admin;
import com.ecommerce.entities.user.Customer;
import com.ecommerce.entities.user.LogisticsProvider;
import com.ecommerce.entities.user.Seller;
import com.ecommerce.entities.user.User;

public interface IRegistrationService {

    public Customer customerRegistration(Customer customer);

    public Admin adminRegistration(Admin admin);

    public LogisticsProvider logisticsProviderRegistration(LogisticsProvider logisticsProvider);

    public Seller sellerRegistration(Seller seller);

    public String verify(User user); // This method is used for login verification
}
