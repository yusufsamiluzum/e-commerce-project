package com.ecommerce.mappers;

import org.mapstruct.Mapper;

import com.ecommerce.dto.DtoPaymentSummary;
import com.ecommerce.entities.Payment;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    // Map relevant fields from Payment entity to DtoPaymentSummary
    DtoPaymentSummary toDtoSummary(Payment payment);
}