package com.skylogic.invoice.controller;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.skylogic.invoice.dto.FieldEnum;
import com.skylogic.invoice.dto.InvoiceCheckResultDTO;
import com.skylogic.invoice.dto.InvoiceStDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    
    /**
     * Trasforma i campi dell'{@link InvoiceStDTO} in una lista di {@link InvoiceCheckResultDTO},
     * una riga per ciascuna proprietà, con il risultato del check ancora non valorizzato.
     */
    protected List<InvoiceCheckResultDTO> toResults(InvoiceStDTO row) {
        List<InvoiceCheckResultDTO> results = new ArrayList<>();
        try {
            PropertyDescriptor[] properties = Introspector.getBeanInfo(InvoiceStDTO.class, Object.class).getPropertyDescriptors();
            for (PropertyDescriptor property : properties) {
                Object value = property.getReadMethod().invoke(row);
                InvoiceCheckResultDTO result = new InvoiceCheckResultDTO();
                result.setFieldName(property.getName());
                result.setFieldValue(value != null ? value.toString() : null);
                results.add(result);
            }
        } catch (Exception e) {
            log.error("toResults - Errore durante la trasformazione di InvoiceStDTO", e);
        }
        return results;
    }
 
}
