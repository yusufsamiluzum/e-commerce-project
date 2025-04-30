package com.ecommerce.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoAttribute {
    private Long attributeId;
    private String name;
    private String value;
    private String unit;
    private String attributeGroup;
    // Maybe omit displayOrder, isKeySpec, isFilterable unless needed by frontend logic
}
