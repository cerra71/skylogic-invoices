package com.skylogic.invoice.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.skylogic.invoice.entity.InvoiceDiscard;
import com.skylogic.invoice.entity.InvoiceSt;
import com.skylogic.invoice.repository.InvoiceDiscardRepository;
import com.skylogic.invoice.repository.InvoiceRepository;
import com.skylogic.invoice.repository.InvoiceStRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@Validated
public class LoaderService {

    private static final int MAX_ERROR_LENGTH = 4000;
    private static final int BATCH_SIZE = 100;

    @Value("${invoices.csv.header:true}")
    private boolean csvHasHeader;

    @Value("${invoices.csv.encoding:UTF-8}")
    private String csvEncoding;

    @Autowired
    private InvoiceStRepository invoiceStRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceDiscardRepository invoiceDiscardRepository;

    /**
     * Mappa statica colonna CSV (nome snake_case) → indice posizionale.
     * Costruita una sola volta al bootstrap del service tramite @PostConstruct.
     * L'ordine riflette le colonne del file CSV sorgente.
     */
    private Map<String, Integer> csvColumnIndex;

    @PostConstruct
    private void init() {
        csvColumnIndex = new HashMap<>();
        csvColumnIndex.put("invoice_number",        0);
        csvColumnIndex.put("invoice_date",          1);
        csvColumnIndex.put("billing_account_number",2);
        csvColumnIndex.put("end_customer_id",       3);
        csvColumnIndex.put("end_customer_name",     4);
        csvColumnIndex.put("site_connectivity_id",  5);
        csvColumnIndex.put("site_name",             6);
        csvColumnIndex.put("order_number",          7);
        csvColumnIndex.put("po_reference",          8);
        csvColumnIndex.put("network_slice_id",      9);
        csvColumnIndex.put("service_id",           10);
        csvColumnIndex.put("imsi",                 11);
        csvColumnIndex.put("additonal_imsi",       12); // typo intenzionale: coerente col CSV sorgente
        csvColumnIndex.put("apn",                  13);
        csvColumnIndex.put("product_identifier",   14);
        csvColumnIndex.put("product_offering_id",  15);
        csvColumnIndex.put("name",                 16);
        csvColumnIndex.put("type",                 17);
        csvColumnIndex.put("rate",                 18);
        csvColumnIndex.put("start_date",           19);
        csvColumnIndex.put("end_date",             20);
        csvColumnIndex.put("entitlement_gb",       21);
        csvColumnIndex.put("shared_pool_id",       22);
        csvColumnIndex.put("usage_gb",             23);
        csvColumnIndex.put("date",                 24);
        csvColumnIndex.put("currency",             25);
        csvColumnIndex.put("amount",               26);
        log.info("LoaderService - csvColumnIndex inizializzata: {} colonne", csvColumnIndex.size());
    }

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
                new InputStreamReader(file.getInputStream(), Charset.forName(csvEncoding)));
             CSVReader csvReader = new CSVReaderBuilder(bufferedReader).build()) {

            // Salta l'intestazione solo se il flag è attivo
            if (csvHasHeader)
                csvReader.readNext();     

            String[] row;
            long rowNum = 1;
            List<InvoiceSt> batch = new ArrayList<>(BATCH_SIZE);

            while ((row = csvReader.readNext()) != null) {
                final String[] currentRow = row;
                final long currentRowNum = rowNum++;

                try {
                    InvoiceSt entity = InvoiceSt.builder()
                            .loadingId(loadingId)
                            .rowNum(currentRowNum)
                            .loadingTime(loadingTime)
                            .invoiceNumber(getField(currentRow, "invoice_number"))
                            .invoiceDate(getField(currentRow, "invoice_date"))
                            .billingAccountNumber(getField(currentRow, "billing_account_number"))
                            .endCustomerId(getField(currentRow, "end_customer_id"))
                            .endCustomerName(getField(currentRow, "end_customer_name"))
                            .siteConnectivityId(getField(currentRow, "site_connectivity_id"))
                            .siteName(getField(currentRow, "site_name"))
                            .orderNumber(getField(currentRow, "order_number"))
                            .poReference(getField(currentRow, "po_reference"))
                            .networkSliceId(getField(currentRow, "network_slice_id"))
                            .serviceId(getField(currentRow, "service_id"))
                            .imsi(getField(currentRow, "imsi"))
                            .additionalImsi(getField(currentRow, "additonal_imsi"))
                            .apn(getField(currentRow, "apn"))
                            .productIdentifier(getField(currentRow, "product_identifier"))
                            .productOfferingId(getField(currentRow, "product_offering_id"))
                            .name(getField(currentRow, "name"))
                            .type(getField(currentRow, "type"))
                            .rate(getField(currentRow, "rate"))
                            .startDate(getField(currentRow, "start_date"))
                            .endDate(getField(currentRow, "end_date"))
                            .entitlementGb(getField(currentRow, "entitlement_gb"))
                            .sharedPoolId(getField(currentRow, "shared_pool_id"))
                            .usageGb(getField(currentRow, "usage_gb"))
                            .date(getField(currentRow, "date"))
                            .currency(getField(currentRow, "currency"))
                            .amount(getField(currentRow, "amount"))
                            .build();

                    batch.add(entity);

                    if (batch.size() >= BATCH_SIZE) {
                        invoiceStRepository.saveAll(batch);
                        batch.clear();
                        log.debug("loadCsv - Salvate {} righe (ultima: {})", BATCH_SIZE, currentRowNum);
                    }

                } catch (Exception e) {
                    log.warn("loadCsv - Riga {} scartata: {}", currentRowNum, e.getMessage());

                    // Svuota il batch corrente prima di salvare lo scarto,
                    // così le righe valide precedenti non vengono perse
                    if (!batch.isEmpty()) {
                        invoiceStRepository.saveAll(batch);
                        batch.clear();
                    }

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

            // Salva le righe rimaste nell'ultimo batch (< BATCH_SIZE)
            if (!batch.isEmpty()) {
                invoiceStRepository.saveAll(batch);
                log.debug("loadCsv - Salvate ultime {} righe", batch.size());
            }

        } catch (Exception e) {
            log.error("loadCsv - Errore fatale nella lettura del file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Errore nel caricamento del file CSV: " + e.getMessage(), e);
        }

        log.info("loadCsv - END: loadingId: {}", loadingId);
    }

    /**
     * Restituisce il valore del campo CSV identificato da {@code columnName}
     * usando la mappa statica {@code csvColumnIndex}, oppure {@code null}
     * se la colonna non esiste nel mapping o il valore è vuoto.
     */
    private String getField(String[] row, String columnName) {
        Integer idx = csvColumnIndex.get(columnName);
        if (idx == null || idx >= row.length) return null;
        String value = row[idx].trim();
        return value.isEmpty() ? null : value;
    }

}
