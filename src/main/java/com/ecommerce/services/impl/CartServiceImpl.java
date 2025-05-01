// src/main/java/com/ecommerce/services/impl/CartServiceImpl.java
package com.ecommerce.services.impl;

import com.ecommerce.dto.DtoCart; //
import com.ecommerce.entities.cart.Cart; //
import com.ecommerce.entities.cart.CartItem; //
import com.ecommerce.entities.product.Product; //
import com.ecommerce.entities.user.Customer; //
import com.ecommerce.exceptions.CartOperationException;
import com.ecommerce.exceptions.InsufficientStockException;
import com.ecommerce.exceptions.ResourceNotFoundException;
import com.ecommerce.mappers.CartMapper;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.CustomerRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.services.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor // Lombok for constructor injection
@Transactional // Ensure atomicity for cart operations
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    @Override
    public DtoCart getCartByCustomerId(Long customerId) {
        Cart cart = findOrCreateCartByCustomerId(customerId);
        // Use the query that fetches items eagerly
        Cart cartWithItems = cartRepository.findByCustomerIdWithItems(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for customer ID: " + customerId));
        return CartMapper.toDtoCart(cartWithItems);
    }

    @Override
    public DtoCart addItemToCart(Long customerId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new CartOperationException("Quantity must be positive.");
        }

        Cart cart = findOrCreateCartByCustomerId(customerId);
        Product product = productRepository.findById(productId) //
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + productId));

        // Check stock
        if (product.getStockQuantity() < quantity) { //
            throw new InsufficientStockException("Insufficient stock for product: " + product.getName() + ". Available: " + product.getStockQuantity()); //
        }

        // Check if item already exists in cart
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartCartIdAndProductProductId(cart.getCartId(), productId); //

        if (existingItemOpt.isPresent()) {
            // Update quantity
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + quantity; //
            if (product.getStockQuantity() < newQuantity) {
                throw new InsufficientStockException("Insufficient stock for product: " + product.getName() + ". Requested total: " + newQuantity + ", Available: " + product.getStockQuantity()); //
            }
            existingItem.setQuantity(newQuantity); //
            cartItemRepository.save(existingItem);
        } else {
            // Add new item
            CartItem newItem = new CartItem(); //
            newItem.setCart(cart); //
            newItem.setProduct(product); //
            newItem.setQuantity(quantity); //
            cart.getItems().add(newItem); // // Important: maintain bidirectional relationship if needed by JPA/Hibernate state management
            cartItemRepository.save(newItem); // Save the new item
        }

        // It might be slightly more efficient to save the cart if the relationship manages items cascade persist,
        // but saving the item explicitly is clearer.
        // cartRepository.save(cart); // Usually not needed if CartItem is saved and cascades are set right

        // Refetch cart with all items for the response DTO
        return getCartByCustomerId(customerId);
    }

    @Override
    public DtoCart updateCartItemQuantity(Long customerId, Long cartItemId, int quantity) {
         if (quantity <= 0) {
            throw new CartOperationException("Quantity must be positive.");
        }

        Cart cart = findCartByCustomerId(customerId);
        CartItem item = cartItemRepository.findByCartCartIdAndCartItemId(cart.getCartId(), cartItemId) //
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with ID: " + cartItemId + " in cart for customer " + customerId));

        Product product = item.getProduct(); //
        if (product.getStockQuantity() < quantity) { //
             throw new InsufficientStockException("Insufficient stock for product: " + product.getName() + ". Requested: " + quantity + ", Available: " + product.getStockQuantity()); //
        }

        item.setQuantity(quantity); //
        cartItemRepository.save(item);

        return getCartByCustomerId(customerId);
    }

    @Override
    public DtoCart removeItemFromCart(Long customerId, Long cartItemId) {
        Cart cart = findCartByCustomerId(customerId);
        CartItem item = cartItemRepository.findByCartCartIdAndCartItemId(cart.getCartId(), cartItemId) //
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with ID: " + cartItemId + " in cart for customer " + customerId));

        // Optional: If Cart entity manages the list lifecycle with orphanRemoval=true, removing from the list might be enough
        // cart.getItems().remove(item);
        // cartRepository.save(cart);
        // However, explicitly deleting the CartItem is often clearer and safer
        cartItemRepository.delete(item);

        return getCartByCustomerId(customerId); // Refetch to get updated total and item list
    }

    @Override
    public DtoCart clearCart(Long customerId) {
        Cart cart = findCartByCustomerId(customerId);

        // Efficiently clear items - Option 1: If cascade remove is set on Cart.items
        // cart.getItems().clear();
        // cartRepository.save(cart);

        // Efficiently clear items - Option 2: Delete items directly (often safer)
        List<CartItem> itemsToDelete = List.copyOf(cart.getItems()); // Avoid ConcurrentModificationException //
        cartItemRepository.deleteAll(itemsToDelete);
        cart.getItems().clear(); // Clear the collection in the managed entity state //

        // Return an empty cart DTO based on the (now empty) cart entity
        return CartMapper.toDtoCart(cart);
    }

    // --- Helper Methods ---

    private Cart findCartByCustomerId(Long customerId) {
        return cartRepository.findByCustomerUserId(customerId) //
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for customer ID: " + customerId));
    }

     private Cart findOrCreateCartByCustomerId(Long customerId) {
        return cartRepository.findByCustomerUserId(customerId) //
                .orElseGet(() -> createCartForCustomer(customerId));
    }

    private Cart createCartForCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId) //
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with ID: " + customerId));

        // Check if customer already has a cart (e.g., due to a race condition)
         Optional<Cart> existingCart = cartRepository.findByCustomerUserId(customerId); //
         if(existingCart.isPresent()) {
             return existingCart.get();
         }

        Cart newCart = new Cart(); //
        newCart.setCustomer(customer); //
        return cartRepository.save(newCart);
    }
}
