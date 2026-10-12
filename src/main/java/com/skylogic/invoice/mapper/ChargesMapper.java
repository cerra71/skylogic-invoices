package com.skylogic.invoice.mapper;

import com.skylogic.invoice.dto.ChargesDTO;
import com.skylogic.invoice.entity.Charges;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for converting Usage entities into UsageDTO objects
 * and UsageDTO objects into Usage entities.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)

public abstract class ChargesMapper extends AbstractMapper<Charges, ChargesDTO> {
}
