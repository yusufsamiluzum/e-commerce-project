package com.ecommerce.repository;

import com.ecommerce.entities.product.Category;
import com.ecommerce.entities.product.ProductComparison;
import com.ecommerce.entities.user.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductComparisonRepository extends JpaRepository<ProductComparison, Long> {

    // Find comparisons for a logged-in customer
    List<ProductComparison> findByCustomer(Customer customer);

    // Find comparisons for a guest session
    List<ProductComparison> findBySessionId(String sessionId);

    // Find a specific comparison, fetching products eagerly for detailed view
    @Query("SELECT pc FROM ProductComparison pc LEFT JOIN FETCH pc.products WHERE pc.comparisonId = :id")
    Optional<ProductComparison> findByIdWithProducts(@Param("id") Long id);

    // Find potentially existing comparison for a customer/session and category
    Optional<ProductComparison> findByCustomerAndCategory(Customer customer, Category category);
    Optional<ProductComparison> findBySessionIdAndCategory(String sessionId, Category category);

    // Find by name for a specific customer (name is unique per comparison, maybe per customer?)
    Optional<ProductComparison> findByCustomerAndName(Customer customer, String name);
    // Potentially find by name for a session? Depends on requirements.
}