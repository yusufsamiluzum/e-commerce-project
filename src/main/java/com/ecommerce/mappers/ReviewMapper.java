package com.ecommerce.mappers;

import com.ecommerce.dto.DtoReview;
import com.ecommerce.dto.DtoUserSummary;
import com.ecommerce.entities.Review;
import com.ecommerce.entities.user.Customer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Manually maps between Review entity and DtoReview DTO.
 * Consider using MapStruct for more complex scenarios.
 */
public class ReviewMapper {

    // Maps a Customer entity to a DtoUserSummary DTO.
    private static DtoUserSummary toDtoUserSummary(Customer customer) {
        if (customer == null) {
            return null;
        }
        return new DtoUserSummary(
                customer.getUserId(),
                customer.getUsername(),
                customer.getFirstName(),
                customer.getLastName()
        );
    }

    /**
     * Maps a Review entity to a DtoReview DTO.
     * @param review The Review entity.
     * @return The corresponding DtoReview DTO.
     */
    public static DtoReview toDtoReview(Review review) {
        if (review == null) {
            return null;
        }
        return new DtoReview(
                review.getReviewId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                toDtoUserSummary(review.getCustomer()), // Map customer summary
                review.getProduct() != null ? review.getProduct().getProductId() : null // Get product ID
        );
    }

    /**
     * Maps a list of Review entities to a list of DtoReview DTOs.
     * @param reviews The list of Review entities.
     * @return The list of corresponding DtoReview DTOs.
     */
    public static List<DtoReview> toDtoReviewList(List<Review> reviews) {
        if (reviews == null) {
            return null;
        }
        return reviews.stream()
                .map(ReviewMapper::toDtoReview)
                .collect(Collectors.toList());
    }

    // Note: Mapping from DtoReview to Review entity is often handled
    // within the service layer when creating/updating, as you need
    // to fetch related entities (Customer, Product).
}
