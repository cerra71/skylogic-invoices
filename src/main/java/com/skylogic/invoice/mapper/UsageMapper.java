package com.skylogic.invoice.mapper;

import com.skylogic.invoice.dto.UsageDTO;
import com.skylogic.invoice.entity.Usage;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for converting Usage entities into UsageDTO objects
 * and UsageDTO objects into Usage entities.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)

public abstract class UsageMapper extends AbstractMapper<Usage, UsageDTO> {
}

