/**
 * MapStruct mapper responsible for converting Invoice entities
 * into InvoiceDTO objects and InvoiceDTO objects into Invoice entities.
 */

package com.skylogic.invoice.mapper;

import com.skylogic.invoice.dto.InvoiceDTO;
import com.skylogic.invoice.entity.Invoice;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting Invoice entities into InvoiceDTO objects
 * and InvoiceDTO objects into Invoice entities.
 */
@Mapper(componentModel = "spring")
public abstract class InvoiceMapper extends AbstractMapper<Invoice, InvoiceDTO> {
}