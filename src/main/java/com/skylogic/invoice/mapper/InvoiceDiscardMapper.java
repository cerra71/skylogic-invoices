/**
 * MapStruct mapper responsible for converting InvoiceDiscard entities
 * into InvoiceDiscardDTO objects and InvoiceDiscardDTO objects
 * into InvoiceDiscard entities.
 */

package com.skylogic.invoice.mapper;

import com.skylogic.invoice.dto.InvoiceDiscardDTO;
import com.skylogic.invoice.entity.InvoiceDiscard;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting InvoiceDiscard entities
 * into InvoiceDiscardDTO objects and InvoiceDiscardDTO objects
 * into InvoiceDiscard entities.
 */
@Mapper(componentModel = "spring")
public abstract class InvoiceDiscardMapper extends AbstractMapper<InvoiceDiscard, InvoiceDiscardDTO> {
}