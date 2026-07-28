/**
 * MapStruct mapper responsible for converting InvoiceSt entities
 * into InvoiceStDTO objects and InvoiceStDTO objects into InvoiceSt entities.
 */

package com.skylogic.invoice.mapper;

import com.skylogic.invoice.dto.InvoiceStDTO;
import com.skylogic.invoice.entity.InvoiceSt;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for converting InvoiceSt entities into InvoiceStDTO objects
 * and InvoiceStDTO objects into InvoiceSt entities.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public abstract class InvoiceStMapper extends AbstractMapper<InvoiceSt, InvoiceStDTO> {
}