package com.skylogic.invoice.controller;

import com.skylogic.invoice.dto.FieldEnum;
import com.skylogic.invoice.dto.InvoiceCheckResultDTO;

public abstract class GenericController {
	
	/**
     * Ricava la posizione del campo nell'enum {@link FieldEnum} (case-insensitive rispetto
     * al fieldName, es. "field1" vs "Field1"), usata per ordinare i risultati.
     * Se il campo non è presente nell'enum, viene messo in fondo.
     */
    protected int fieldEnumOrder(InvoiceCheckResultDTO field) {
        for (FieldEnum value : FieldEnum.values()) {
            if (value.name().equalsIgnoreCase(field.getFieldName())) {
                return value.ordinal();
            }
        }
        return Integer.MAX_VALUE;
    }

}
