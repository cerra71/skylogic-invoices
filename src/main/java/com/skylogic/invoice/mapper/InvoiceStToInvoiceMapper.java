/**
 * Mapper for converting InvoiceStDTO objects into InvoiceDTO objects
 * and InvoiceDTO objects back into InvoiceStDTO objects.
 */

package com.skylogic.invoice.mapper;

import com.skylogic.invoice.dto.InvoiceDTO;
import com.skylogic.invoice.dto.InvoiceStDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;


@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public abstract class InvoiceStToInvoiceMapper extends AbstractMapper<InvoiceStDTO, InvoiceDTO> {
}