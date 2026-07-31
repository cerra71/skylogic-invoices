/**
 * MapStruct mapper responsible for converting InvoiceDiscard entities
 * into InvoiceDiscardDTO objects and InvoiceDiscardDTO objects
 * into InvoiceDiscard entities.
 */

package com.skylogic.invoice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.skylogic.invoice.dto.LoadingSummaryDTO;
import com.skylogic.invoice.dto.LoadingSummaryInterface;

/**
 * MapStruct mapper for converting InvoiceDiscard entities
 * into InvoiceDiscardDTO objects and InvoiceDiscardDTO objects
 * into InvoiceDiscard entities.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class LoadingSummaryMapper extends AbstractMapper<LoadingSummaryInterface, LoadingSummaryDTO> {
}