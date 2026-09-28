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
import java.util.Locale;
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
     * <p>
     * Nota: questo mapping statico viene usato COME FALLBACK se un header non
     * viene riconosciuto dalla mappa dinamica; inoltre riflette l'ordine delle
     * colonne dei file "storici" (27 colonne):
     * Invoice Number, Invoice Date, Billing Account Number, End Customer ID,
     * End Customer Name, Site Connectivity ID, Site Name, Order Number,
     * PO Reference, Network Slice ID, Service ID, IMSI, Additonal IMSI,
     * APN, Product Identifier, Product Offering ID, Name, Type, Rate,
     * Start date, End date, Entitlement (GB), Shared Pool ID, Usage (GB),
     * Date, Currency, Amount.
     */
    private Map<String, Integer> csvColumnIndex;

    /**
     * Alias noti per header non standard: chiave = nome normalizzato che
     * potrebbe uscire da {@link #normalizeHeaderName} per un header CSV
     * reale, valore = nome standard usato nel mapping/entity.
     * <p>
     * Esempio tipico: {@code Entitlement (GB)} → dopo normalizzazione dei
     * separatori diventa {@code entitlement_gb}, ma se nel file è scritto
     * in modo anomalo (es. senza spazio) vogliamo comunque ricondurlo.
     */
    private static final Map<String, String> HEADER_ALIASES;

    static {
        Map<String, String> a = new HashMap<>();

        // --- Correzioni comuni per suffisso (GB) o trattini ---
        a.put("entitlementgb",          "entitlement_gb");
        a.put("usagegb",                "usage_gb");
        a.put("entitlement",            "entitlement_gb");
        a.put("usage",                  "usage_gb");
        a.put("entitlementgbvalue",     "entitlement_gb");
        a.put("usagegbvalue",           "usage_gb");

        // --- Case/typo noti nella colonna IMSI aggiuntiva (DB ha typo "additonal") ---
        a.put("additional_imsi",        "additonal_imsi");
        a.put("additionalimsi",         "additonal_imsi");
        a.put("additional_imsi_value",  "additonal_imsi");
        a.put("additonalimsi",          "additonal_imsi");   // già ok, ma per sicurezza

        // --- Varianti camel-case / spazi tipiche di export Excel ---
        a.put("invoice_number",         "invoice_number");
        a.put("invoicenumber",          "invoice_number");
        a.put("invoice_date",           "invoice_date");
        a.put("invoicedate",            "invoice_date");
        a.put("billing_account_number", "billing_account_number");
        a.put("billingaccountnumber",   "billing_account_number");
        a.put("end_customer_id",        "end_customer_id");
        a.put("endcustomerid",          "end_customer_id");
        a.put("end_customer_name",      "end_customer_name");
        a.put("endcustomername",        "end_customer_name");
        a.put("site_connectivity_id",   "site_connectivity_id");
        a.put("siteconnectivityid",     "site_connectivity_id");
        a.put("site_name",              "site_name");
        a.put("sitename",               "site_name");
        a.put("order_number",           "order_number");
        a.put("ordernumber",            "order_number");
        a.put("po_reference",           "po_reference");
        a.put("poreference",            "po_reference");
        a.put("network_slice_id",       "network_slice_id");
        a.put("networksliceid",         "network_slice_id");
        a.put("service_id",             "service_id");
        a.put("serviceid",              "service_id");
        a.put("product_identifier",     "product_identifier");
        a.put("productidentifier",      "product_identifier");
        a.put("product_offering_id",    "product_offering_id");
        a.put("productofferingid",      "product_offering_id");
        a.put("shared_pool_id",         "shared_pool_id");
        a.put("sharedpoolid",           "shared_pool_id");
        a.put("start_date",             "start_date");
        a.put("startdate",              "start_date");
        a.put("end_date",               "end_date");
        a.put("enddate",                "end_date");
        a.put("loading_id",             "loading_id");
        a.put("loadingid",              "loading_id");
        a.put("loading_time",           "loading_time");
        a.put("loadingtime",            "loading_time");
        a.put("row_number",             "row_number");
        a.put("rownumber",              "row_number");
        a.put("row_num",                "row_number");
        a.put("rownum",                 "row_number");

        HEADER_ALIASES = java.util.Collections.unmodifiableMap(a);
    }

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

            Map<String, Integer> headerMap;
            if (csvHasHeader) {
                String[] headerRow = csvReader.readNext();
                headerMap = buildHeaderMap(headerRow);
                log.info("loadCsv - CSV with header: {} colonne riconosciute (fallback a statica per colonne non trovate)",
                        headerMap.size());
            } else {
                headerMap = csvColumnIndex;
                log.info("loadCsv - CSV without header (usa mapping statico): {} colonne", headerMap.size());
            }

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
                            .invoiceNumber(getField(currentRow, headerMap, "invoice_number"))
                            .invoiceDate(getField(currentRow, headerMap, "invoice_date"))
                            .billingAccountNumber(getField(currentRow, headerMap, "billing_account_number"))
                            .endCustomerId(getField(currentRow, headerMap, "end_customer_id"))
                            .endCustomerName(getField(currentRow, headerMap, "end_customer_name"))
                            .siteConnectivityId(getField(currentRow, headerMap, "site_connectivity_id"))
                            .siteName(getField(currentRow, headerMap, "site_name"))
                            .orderNumber(getField(currentRow, headerMap, "order_number"))
                            .poReference(getField(currentRow, headerMap, "po_reference"))
                            .networkSliceId(getField(currentRow, headerMap, "network_slice_id"))
                            .serviceId(getField(currentRow, headerMap, "service_id"))
                            .imsi(getField(currentRow, headerMap, "imsi"))
                            .additionalImsi(getField(currentRow, headerMap, "additonal_imsi"))
                            .apn(getField(currentRow, headerMap, "apn"))
                            .productIdentifier(getField(currentRow, headerMap, "product_identifier"))
                            .productOfferingId(getField(currentRow, headerMap, "product_offering_id"))
                            .name(getField(currentRow, headerMap, "name"))
                            .type(getField(currentRow, headerMap, "type"))
                            .rate(getField(currentRow, headerMap, "rate"))
                            .startDate(getField(currentRow, headerMap, "start_date"))
                            .endDate(getField(currentRow, headerMap, "end_date"))
                            .entitlementGb(getField(currentRow, headerMap, "entitlement_gb"))
                            .sharedPoolId(getField(currentRow, headerMap, "shared_pool_id"))
                            .usageGb(getField(currentRow, headerMap, "usage_gb"))
                            .date(getField(currentRow, headerMap, "date"))
                            .currency(getField(currentRow, headerMap, "currency"))
                            .amount(getField(currentRow, headerMap, "amount"))
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
     * Costruisce la mappa nome-colonna-normalizzato → indice a partire dalla riga di
     * intestazione del CSV caricato.
     * <p>
     * La normalizzazione rende il mapping insensibile a:
     * <ul>
     *   <li>maiuscole/minuscole</li>
     *   <li>spazi esterni</li>
     *   <li>spazi interi vs underscore (es. "Start Date" ↔ "start_date")</li>
     *   <li>trattini o punti separatori</li>
     * </ul>
     * Per colonne non trovate nella mappa dinamica, {@link #getField} ricade
     * automaticamente sulla mappa statica {@code csvColumnIndex}.
     */
    private Map<String, Integer> buildHeaderMap(String[] headerRow) {
        Map<String, Integer> dynamic = new HashMap<>();
        if (headerRow == null) {
            return dynamic;
        }
        for (int i = 0; i < headerRow.length; i++) {
            String raw = headerRow[i];
            if (raw == null) {
                continue;
            }
            String normalized = normalizeHeaderName(raw);
            if (normalized.isEmpty()) {
                continue;
            }
            // Non sovrascrivere per non perdere la prima occorrenza in caso di
            // colonne duplicate nel CSV.
            dynamic.putIfAbsent(normalized, i);

            // Se questo header, dopo normalizzazione, corrisponde a un alias
            // noto (es. additional_imsi → additonal_imsi, oppure
            // entitlement_gb è già standard ma nel CSV era scritto "Entitlement (GB)"
            // → normalized=entitlement_gb), scriviamo anche la chiave standard
            // per velocizzare il lookup successivo.
            String aliased = HEADER_ALIASES.get(normalized);
            if (aliased != null && !aliased.equals(normalized)) {
                dynamic.putIfAbsent(aliased, i);
            }
        }
        return dynamic;
    }

    /**
     * Trasforma un nome di colonna qualsiasi (case, spazi, trattini) nella forma
     * standard usata nel DB/entity (snake_case minuscolo). Esempi:
     * <pre>
     *   "Start Date"        → "start_date"
     *   " PRODUCT-OFFERING.ID " → "product_offering_id"
     *   "INVOICE_NUMBER"    → "invoice_number"
     *   "additonal Imsi"    → "additonal_imsi"
     * </pre>
     */
    private String normalizeHeaderName(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean underscorePending = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                if (underscorePending && sb.length() > 0) {
                    sb.append('_');
                    underscorePending = false;
                }
                sb.append(c);
            } else {
                underscorePending = true;
            }
        }
        return sb.toString();
    }

    /**
     * Restituisce il valore del campo CSV identificato da {@code columnName}.
     * Cerca prima nella mappa DINAMICA costruita dagli header reali del file
     * caricato, poi (se non trovato) ricade nella mappa STATIC hardcoded
     * {@code csvColumnIndex} (fallback per file senza header o colonne rinominate
     * in modo non standard).
     * Restituisce {@code null} se la colonna non esiste nel mapping o il valore è vuoto.
     */
    private String getField(String[] row, Map<String, Integer> dynamicHeaderMap, String columnName) {
        Integer idx = null;
        if (dynamicHeaderMap != null) {
            idx = dynamicHeaderMap.get(columnName);
            if (idx == null) {
                // Provo alias noti del nome campo (es. columnName="additonal_imsi"
                // ma qualcuno ha scritto nel mapping "additional_imsi").
                String alias = HEADER_ALIASES.get(columnName);
                if (alias != null) {
                    idx = dynamicHeaderMap.get(alias);
                }
            }
            // Ultimo tentativo: normalizzo columnName
            if (idx == null) {
                idx = dynamicHeaderMap.get(normalizeHeaderName(columnName));
            }
        }
        if (idx == null) {
            idx = csvColumnIndex.get(columnName);
        }
        if (idx == null || idx >= row.length) return null;
        String value = row[idx].trim();
        return value.isEmpty() ? null : value;
    }

}
