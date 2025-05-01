package com.ecommerce.mappers;

import java.util.List;

import org.mapstruct.Mapper;

import com.ecommerce.dto.DtoShipmentSummary;
import com.ecommerce.entities.Shipment;

@Mapper(componentModel = "spring")
public interface ShipmentMapper {
     // Map relevant fields from Shipment entity to DtoShipmentSummary
    DtoShipmentSummary toDtoSummary(Shipment shipment);
    List<DtoShipmentSummary> toDtoSummaryList(List<Shipment> shipments);
}
