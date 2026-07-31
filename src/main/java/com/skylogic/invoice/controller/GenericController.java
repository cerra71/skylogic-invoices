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
    
    /**
     * Trasforma le proprietà del DTO ricevuto in una lista di {@link InvoiceCheckResultDTO}.
     * Il metodo può ricevere sia un {@link InvoiceStDTO} sia un {@link InvoiceDTO}.
     * Ogni proprietà del DTO diventa una riga della tabella:
     *
     * fieldName  = nome della proprietà
     * fieldValue = valore della proprietà
     * result     = null, perché il check non è ancora stato eseguito
     */
/*    protected <T> List<InvoiceCheckResultDTO> toResults(T row) {

        // Lista che conterrà una riga per ogni proprietà del DTO.
        List<InvoiceCheckResultDTO> results = new ArrayList<>();

        // Se non è stato restituito nessun record viene restituita una lista vuota (e non un null)
        if (row == null) {
            return results;
        }

        try {

            
            PropertyDescriptor[] properties = Introspector.getBeanInfo(
                    row.getClass(),
                    Object.class
            ).getPropertyDescriptors();

            // Scorriamo tutte le proprietà trovate nel DTO.
            for (PropertyDescriptor property : properties) {

                // Recuperiamo il getter della specifica proprietà
                Method readMethod = property.getReadMethod();

                
                if (readMethod == null) {
                    continue;
                }

                // Viene eseguito il getter sull'oggetto ricevuto
                Object value = readMethod.invoke(row);

                // Creiamo una nuova riga destinata alla tabella di details.html.
                InvoiceCheckResultDTO result = new InvoiceCheckResultDTO();

                // Salviamo il nome della proprietà.
                result.setFieldName(property.getName());

                
                result.setFieldValue(value != null ? value.toString() : null);

                

                // Aggiungiamo la riga alla lista finale.
                results.add(result);
                }

            } catch (Exception e) {

                // Se il caricamento fallisce, nel messaggio è indicato quale tipo di DTO ha causato l'errore
                log.error( "toResults - Errore durante la trasformazione di {}", row.getClass().getSimpleName(), e);
        }

            return results;
    }*/

}
