package com.skylogic.invoice.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.skylogic.invoice.entity.InvoiceDiscard;
import com.skylogic.invoice.entity.InvoiceSt;
import com.skylogic.invoice.repository.InvoiceDiscardRepository;
import com.skylogic.invoice.repository.InvoiceRepository;
import com.skylogic.invoice.repository.InvoiceStRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@Validated
public class LoaderService {

    private static final int MAX_ERROR_LENGTH = 4000;

    @Autowired
    private InvoiceStRepository invoiceStRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceDiscardRepository invoiceDiscardRepository;

    // //

    /**
     * Legge il file CSV riga per riga e carica i record in invoice_st.
     * In caso di errore su una riga, il record viene salvato in invoice_discard
     * con il messaggio di errore (troncato a 4000 caratteri).
     * Tutti i record del caricamento condividono lo stesso loading_id e loading_time.
     */
    public void loadCsv(MultipartFile file) {
        log.info("loadCsv - START: file: {}", file.getOriginalFilename());

        String loadingId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime loadingTime = LocalDateTime.now();

        log.info("loadCsv - loadingId: {}", loadingId);

        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReaderBuilder(bufferedReader).build()) {

            // Prima riga = intestazione
            String[] headers = csvReader.readNext();
            if (headers == null) {
                log.warn("loadCsv - File vuoto o senza intestazione: {}", file.getOriginalFilename());
                return;
            }

            // Mappa nome colonna (lowercase, trimmed) → indice
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerIndex.put(headers[i].trim().toLowerCase(), i);
            }

            String[] row;
            long rowNum = 1;

            while ((row = csvReader.readNext()) != null) {
                final String[] currentRow = row;
                final long currentRowNum = rowNum++;

                try {
                    InvoiceSt entity = InvoiceSt.builder()
                            .loadingId(loadingId)
                            .rowNum(currentRowNum)
                            .loadingTime(loadingTime)
                            .invoiceNumber(getField(currentRow, headerIndex, "invoice_number"))
                            .invoiceDate(getField(currentRow, headerIndex, "invoice_date"))
                            .billingAccountNumber(getField(currentRow, headerIndex, "billing_account_number"))
                            .endCustomerId(getField(currentRow, headerIndex, "end_customer_id"))
                            .endCustomerName(getField(currentRow, headerIndex, "end_customer_name"))
                            .siteConnectivityId(getField(currentRow, headerIndex, "site_connectivity_id"))
                            .siteName(getField(currentRow, headerIndex, "site_name"))
                            .orderNumber(getField(currentRow, headerIndex, "order_number"))
                            .poReference(getField(currentRow, headerIndex, "po_reference"))
                            .networkSliceId(getField(currentRow, headerIndex, "network_slice_id"))
                            .serviceId(getField(currentRow, headerIndex, "service_id"))
                            .imsi(getField(currentRow, headerIndex, "imsi"))
                            .additionalImsi(getField(currentRow, headerIndex, "additonal_imsi")) // typo intenzionale: coerente col CSV sorgente
                            .apn(getField(currentRow, headerIndex, "apn"))
                            .productIdentifier(getField(currentRow, headerIndex, "product_identifier"))
                            .productOfferingId(getField(currentRow, headerIndex, "product_offering_id"))
                            .name(getField(currentRow, headerIndex, "name"))
                            .type(getField(currentRow, headerIndex, "type"))
                            .rate(getField(currentRow, headerIndex, "rate"))
                            .startDate(getField(currentRow, headerIndex, "start_date"))
                            .endDate(getField(currentRow, headerIndex, "end_date"))
                            .entitlementGb(getField(currentRow, headerIndex, "entitlement_gb"))
                            .sharedPoolId(getField(currentRow, headerIndex, "shared_pool_id"))
                            .usageGb(getField(currentRow, headerIndex, "usage_gb"))
                            .date(getField(currentRow, headerIndex, "date"))
                            .currency(getField(currentRow, headerIndex, "currency"))
                            .amount(getField(currentRow, headerIndex, "amount"))
                            .build();

                    invoiceStRepository.save(entity);

                } catch (Exception e) {
                    log.warn("loadCsv - Riga {} scartata: {}", currentRowNum, e.getMessage());

                    String error = e.getMessage();
                    if (error != null && error.length() > MAX_ERROR_LENGTH) {
                        error = error.substring(0, MAX_ERROR_LENGTH);
                    }

                    InvoiceDiscard discard = InvoiceDiscard.builder()
                            .loadingId(loadingId)
                            .rowNum(currentRowNum)
                            .discardedRow(String.join(",", currentRow))
                            .error(error)
                            .build();

                    invoiceDiscardRepository.save(discard);
                }
            }

        } catch (Exception e) {
            log.error("loadCsv - Errore fatale nella lettura del file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Errore nel caricamento del file CSV: " + e.getMessage(), e);
        }

        log.info("loadCsv - END: loadingId: {}", loadingId);
    }

    /**
     * Restituisce il valore del campo CSV identificato da {@code columnName},
     * oppure {@code null} se la colonna non esiste o il valore è vuoto.
     */
    private String getField(String[] row, Map<String, Integer> headerIndex, String columnName) {
        Integer idx = headerIndex.get(columnName);
        if (idx == null || idx >= row.length) return null;
        String value = row[idx].trim();
        return value.isEmpty() ? null : value;
    }

}
