package com.ecommerce.dto;

import java.util.HashSet;
import java.util.Set;

import com.ecommerce.entities.product.Product;
import com.ecommerce.entities.user.Customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoComparison {

    private String name;
    private Customer customer;
    private Set<Product> products = new HashSet<>();
}
