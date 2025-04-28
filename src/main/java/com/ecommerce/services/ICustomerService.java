package com.ecommerce.services;

import java.util.List;
import java.util.Set;
import java.util.Locale.Category;

import com.ecommerce.dto.DtoComparison;
import com.ecommerce.dto.DtoOrder;
import com.ecommerce.dto.DtoProduct;
import com.ecommerce.dto.DtoProfile;
import com.ecommerce.dto.DtoSetNewPassword;
import com.ecommerce.entities.Wishlist;
import com.ecommerce.entities.product.ProductComparison;
import com.ecommerce.entities.user.User;

public interface ICustomerService {

    public String helloCustomer(); 

    public DtoProfile getCustomerProfile(User customer);

    public void updateCustomerProfile(DtoProfile dtoProfile);

    public void updateCustomerPassword(DtoSetNewPassword newPassword);

    public Category getCategory(String name);

    public List<Category> getCategories();

    public Set<DtoProduct> filter(Set<Category> filter);

    public Set<DtoProduct> sort(Set<Category> filter, String sortBy, String orderBy);//?????????????????????

    public DtoProduct getProductDetails(DtoProduct productId);

    public void addProductToComparison(DtoProduct product,DtoComparison comparison);

    public void removeProductFromComparison(DtoProduct product,DtoComparison comparison);

    public ProductComparison getProductComparison(DtoComparison comparison);

    public List<ProductComparison> getProductComparisons();

    public void saveProductComparison(DtoComparison comparison);

    public void deleteProductComparison(DtoComparison comparison);

    public void addProdcutToCart(DtoProduct product, int quantity);

    public void removeProductFromCart(DtoProduct product, int quantity);

    public void updateProductInCart(DtoProduct product, int quantity);

    public void clearCart();

    public void checkoutCart();

    public DtoOrder getOrderDetails(DtoOrder order);

    public List<DtoOrder> getOrdersHistory();

    public void addReview(DtoProduct product, String review);

    public void removeReview(DtoProduct product, String review);

    public void addRating(DtoProduct product, int rating);

    public void removeRating(DtoProduct product, int rating);

    public void getRewiews(DtoProduct product);

    public void addWishlistItem(DtoProduct product);

    public void removeWishlistItem(DtoProduct product);

    public Wishlist getWishlist();
}
