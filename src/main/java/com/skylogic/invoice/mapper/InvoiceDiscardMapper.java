/**
 * MapStruct mapper responsible for converting InvoiceDiscard entities
 * into InvoiceDiscardDTO objects and InvoiceDiscardDTO objects
 * into InvoiceDiscard entities.
 */

package com.skylogic.invoice.mapper;

import com.skylogic.invoice.dto.InvoiceDiscardDTO;
import com.skylogic.invoice.entity.InvoiceDiscard;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for converting InvoiceDiscard entities
 * into InvoiceDiscardDTO objects and InvoiceDiscardDTO objects
 * into InvoiceDiscard entities.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class InvoiceDiscardMapper extends AbstractMapper<InvoiceDiscard, InvoiceDiscardDTO> {
}