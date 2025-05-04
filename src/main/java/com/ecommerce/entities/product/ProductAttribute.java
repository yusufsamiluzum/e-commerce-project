package com.ecommerce.entities.product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ProductAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attributeId;
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    
    private String name;     // e.g., "Screen Size", "RAM", "Processor"
    private String value;    // e.g., "15.6 inches", "16GB", "Intel i7"
    private String unit;     // e.g., "inches", "GB", null

    @ManyToOne
    @JoinColumn(name = "variant_id") // Adjust the column name as needed
    private ProductVariant variant; // The field must be named 'variant'
    
    // Used for ordering attributes in comparison view
    private Integer displayOrder;
    
    // Whether this is a key spec to highlight in comparison
    private boolean isKeySpec = false;
    
    // Used for filtering in comparison
    private boolean isFilterable = false;
    
    // Group attributes (e.g., "Technical", "Physical", "Performance")
    private String attributeGroup;
}
