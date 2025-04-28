package com.ecommerce.services;

import java.util.List;
import java.util.Locale.Category;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.dto.DtoComparison;
import com.ecommerce.dto.DtoOrder;
import com.ecommerce.dto.DtoProduct;
import com.ecommerce.dto.DtoProfile;
import com.ecommerce.dto.DtoSetNewPassword;
import com.ecommerce.entities.Review;
import com.ecommerce.entities.Wishlist;
import com.ecommerce.entities.product.ProductComparison;
import com.ecommerce.entities.user.User;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.CustomerRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.ReviewRepository;
import com.ecommerce.repository.WishlistRepository;

import lombok.Data;


@Data
@Service
public class CustomerService implements ICustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private WishlistRepository wishlistRepository;

    @Override
    public String helloCustomer() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'helloCustomer'");
    }

    @Override
    public DtoProfile getCustomerProfile(User customer) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCustomerProfile'");
    }

    @Override
    public void updateCustomerProfile(DtoProfile dtoProfile) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateCustomerProfile'");
    }

    @Override
    public void updateCustomerPassword(DtoSetNewPassword newPassword) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateCustomerPassword'");
    }

    @Override
    public Category getCategory(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCategory'");
    }

    @Override
    public List<Category> getCategories() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCategories'");
    }

    @Override
    public Set<DtoProduct> filter(Set<Category> filter) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'filter'");
    }

    @Override
    public Set<DtoProduct> sort(Set<Category> filter, String sortBy, String orderBy) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sort'");
    }

    @Override
    public DtoProduct getProductDetails(DtoProduct productId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProductDetails'");
    }

    @Override
    public void addProductToComparison(DtoProduct product, DtoComparison comparison) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addProductToComparison'");
    }

    @Override
    public void removeProductFromComparison(DtoProduct product, DtoComparison comparison) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeProductFromComparison'");
    }

    @Override
    public ProductComparison getProductComparison(DtoComparison comparison) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProductComparison'");
    }

    @Override
    public List<ProductComparison> getProductComparisons() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProductComparisons'");
    }

    @Override
    public void saveProductComparison(DtoComparison comparison) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'saveProductComparison'");
    }

    @Override
    public void deleteProductComparison(DtoComparison comparison) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteProductComparison'");
    }

    @Override
    public void addProdcutToCart(DtoProduct product, int quantity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addProdcutToCart'");
    }

    @Override
    public void removeProductFromCart(DtoProduct product, int quantity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeProductFromCart'");
    }

    @Override
    public void updateProductInCart(DtoProduct product, int quantity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateProductInCart'");
    }

    @Override
    public void clearCart() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'clearCart'");
    }

    @Override
    public void checkoutCart() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'checkoutCart'");
    }

    @Override
    public DtoOrder getOrderDetails(DtoOrder order) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getOrderDetails'");
    }

    @Override
    public List<DtoOrder> getOrdersHistory() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getOrdersHistory'");
    }

    @Override
    public void addReview(DtoProduct product, String review) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addReview'");
    }

    @Override
    public void removeReview(DtoProduct product, String review) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeReview'");
    }

    @Override
    public void addRating(DtoProduct product, int rating) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addRating'");
    }

    @Override
    public void removeRating(DtoProduct product, int rating) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeRating'");
    }

    @Override
    public void getRewiews(DtoProduct product) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRewiews'");
    }

    @Override
    public void addWishlistItem(DtoProduct product) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addWishlistItem'");
    }

    @Override
    public void removeWishlistItem(DtoProduct product) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeWishlistItem'");
    }

    @Override
    public Wishlist getWishlist() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getWishlist'");
    }
}
