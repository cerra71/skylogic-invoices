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
     * Trasforma le proprietà del DTO ricevuto in una lista di {@link InvoiceCheckResultDTO}.
     * Il metodo può ricevere sia un {@link InvoiceStDTO} sia un {@link InvoiceDTO}.
     * Ogni proprietà del DTO diventa una riga della tabella:
     *
     * fieldName  = nome della proprietà
     * fieldValue = valore della proprietà
     * result     = null, perché il check non è ancora stato eseguito
     */
    protected <T> List<InvoiceCheckResultDTO> toResults(T row) {

        // Lista che conterrà una riga per ogni proprietà del DTO.
        List<InvoiceCheckResultDTO> results = new ArrayList<>();

        // Se non è stato restituito nessun record viene restituita una lista vuota (e non un null)
        if (row == null) {
            return results;
        }

        try {

            /*
             * Leggiamo le proprietà della classe reale dell'oggetto.
             *
             * Se row è InvoiceStDTO, viene analizzata InvoiceStDTO.class.
             * Se row è InvoiceDTO, viene analizzata InvoiceDTO.class.
             *
             * Object.class serve a escludere le proprietà ereditate
             * dalla classe Object, come la proprietà "class".
             */
            PropertyDescriptor[] properties = Introspector.getBeanInfo(
                    row.getClass(),
                    Object.class
            ).getPropertyDescriptors();

            // Scorriamo tutte le proprietà trovate nel DTO.
            for (PropertyDescriptor property : properties) {

                // Recuperiamo il getter della specifica proprietà
                Method readMethod = property.getReadMethod();

                /*
                 * Se la proprietà non possiede un getter viene ignorata perché non
                 * è possibile leggerne il valore.
                 */
                if (readMethod == null) {
                    continue;
                }

                // Viene eseguito il getter sull'oggetto ricevuto
                Object value = readMethod.invoke(row);

                // Creiamo una nuova riga destinata alla tabella di details.html.
                InvoiceCheckResultDTO result = new InvoiceCheckResultDTO();

                // Salviamo il nome della proprietà.
                result.setFieldName(property.getName());

                /*
                 * Salviamo il valore della proprietà come String.
                 * Se il valore originale è null, anche fieldValue rimane null.
                 */
                result.setFieldValue(value != null ? value.toString() : null);

                /*
                 * Non impostiamo result.
                 * Durante Load Row il check non è ancora stato eseguito, quindi result deve restare null.
                 */

                // Aggiungiamo la riga alla lista finale.
                results.add(result);
                }

            } catch (Exception e) {

                // Se il caricamento fallisce, nel messaggio è indicato quale tipo di DTO ha causato l'errore
                log.error( "toResults - Errore durante la trasformazione di {}", row.getClass().getSimpleName(), e);
        }

            return results;
    }

}
