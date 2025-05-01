// src/main/java/com/ecommerce/mappers/CartMapper.java
package com.ecommerce.mappers;

import com.ecommerce.dto.DtoCart;
import com.ecommerce.dto.DtoCartItem;
import com.ecommerce.dto.DtoProductSummary;
import com.ecommerce.entities.cart.Cart;
import com.ecommerce.entities.cart.CartItem;
import com.ecommerce.entities.product.Product; //

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CartMapper {

    public static DtoCart toDtoCart(Cart cart) { //
        if (cart == null) {
            return null;
        }
        DtoCart dto = new DtoCart(); //
        dto.setCartId(cart.getCartId()); //
        List<DtoCartItem> dtoItems = cart.getItems() == null ? Collections.emptyList() :
                cart.getItems().stream() //
                        .map(CartMapper::toDtoCartItem)
                        .collect(Collectors.toList());
        dto.setItems(dtoItems); //

        // Calculate total in the service or here
        BigDecimal total = dtoItems.stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getPrice() != null)
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))) //
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setCalculatedTotal(total); //

        return dto;
    }

    public static DtoCartItem toDtoCartItem(CartItem cartItem) { //
        if (cartItem == null) {
            return null;
        }
        DtoCartItem dto = new DtoCartItem(); //
        dto.setCartItemId(cartItem.getCartItemId()); //
        dto.setQuantity(cartItem.getQuantity()); //
        dto.setProduct(toDtoProductSummary(cartItem.getProduct())); //
        return dto;
    }

    public static DtoProductSummary toDtoProductSummary(Product product) { //
        if (product == null) {
            return null;
        }
        DtoProductSummary dto = new DtoProductSummary(); //
        dto.setProductId(product.getProductId()); //
        dto.setName(product.getName()); //
        dto.setPrice(product.getPrice()); //
        dto.setAverageRating(product.getAverageRating()); //
        dto.setBrand(product.getBrand()); //
        dto.setModel(product.getModel()); //
        // dto.setPrimaryImageUrl(...); // TODO: Add logic to get primary image URL if available
        return dto;
    }
}