package com.skylogic.invoice.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import com.skylogic.invoice.advice.ZipNonValidoException;
import com.skylogic.invoice.entity.Charges;
import com.skylogic.invoice.entity.InvoiceDiscard;
import com.skylogic.invoice.entity.InvoiceSt;
import com.skylogic.invoice.entity.Usage;
import com.skylogic.invoice.repository.ChargesRepository;
import com.skylogic.invoice.repository.InvoiceDiscardRepository;
import com.skylogic.invoice.repository.InvoiceRepository;
import com.skylogic.invoice.repository.InvoiceStRepository;
import com.skylogic.invoice.repository.UsageRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.springframework.util.NumberUtils.parseNumber;

@Service
@Slf4j
@Validated
public class LoaderService {

    private static final int MAX_ERROR_LENGTH = 4000;
    private static final int BATCH_SIZE = 100;

    private static final int MAX_ENTRIES = 1_000;
    private static final long MAX_ENTRY_SIZE = 50L * 1024 * 1024;
    private static final long MAX_TOTAL_SIZE = 500L * 1024 * 1024;

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

    @Autowired
    private UsageRepository usageRepository;

    @Autowired
    private ChargesRepository chargesRepository;

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
    @Transactional(rollbackFor = Exception.class)
    public void loadCsv(MultipartFile file) {
        log.info("loadCsv - START: file: {}", file.getOriginalFilename());

        String loadingId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime loadingTime = LocalDateTime.now();

        log.info("loadCsv - loadingId: {}", loadingId);

        try (InputStream is = file.getInputStream()) {
            parseAndLoadCsv(is, loadingId, loadingTime);
        } catch (Exception e) {
            log.error("loadCsv - Errore fatale nella lettura del file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Errore nel caricamento del file CSV: " + e.getMessage(), e);
        }

        log.info("loadCsv - END: loadingId: {}", loadingId);
    }

    /**
     * Apre un file ZIP, itera sulle entry e carica ogni file CSV trovato
     * nella tabella di destinazione in base al nome del file:
     * <ul>
     *   <li>se il nome contiene {@code USAGE} (case-insensitive) → tabella {@code usage}</li>
     *   <li>altrimenti → tabella {@code charges}</li>
     * </ul>
     * Tutti i CSV contenuti nello ZIP condividono lo stesso {@code loadingId} e
     * {@code loadingTime}, così nella pagina home risulteranno come un singolo
     * caricamento con il totale aggregato delle righe.
     * <p>
     * Protezioni attive (anti zip-bomb):
     * <ul>
     *   <li>numero massimo di entry: {@link #MAX_ENTRIES}</li>
     *   <li>dimensione massima decompressa per singola entry: {@link #MAX_ENTRY_SIZE}</li>
     *   <li>dimensione massima decompressa totale dello ZIP: {@link #MAX_TOTAL_SIZE}</li>
     * </ul>
     */
    @Transactional(rollbackFor = Exception.class)
    public void loadZip(MultipartFile file) {
        log.info("loadZip - START: file: {}", file.getOriginalFilename());

        String loadingId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime loadingTime = LocalDateTime.now();

        log.info("loadZip - loadingId condiviso: {}", loadingId);

        long totale = 0;
        int count = 0;
        String currentEntry = null;

        try (ZipInputStream zis = new ZipInputStream(file.getInputStream(), Charset.forName(csvEncoding))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                if (++count > MAX_ENTRIES) {
                    throw new ZipNonValidoException("Troppi file nell'archivio");
                }

                String entryName = entry.getName();
                currentEntry = entryName;
                log.info("loadZip - entry: {}", entryName);

                if (entryName == null || !entryName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
                    log.error("loadZip - Entry non valida (non CSV): '{}'. Caricamento abortito.", entryName);
                    throw new ZipNonValidoException(
                            "File non supportato nell'archivio ZIP (sono ammessi solo file .csv): " + entryName);
                }

                LimitedInputStream limitato = new LimitedInputStream(zis, MAX_ENTRY_SIZE, entryName);

                boolean isUsage = entryName.toUpperCase(Locale.ROOT).contains("USAGE");
                if (isUsage) {
                    log.info("loadZip - instradamento a tabella USAGE: {}", entryName);
                    parseAndLoadUsage(limitato, loadingId, loadingTime, entryName);
                } else {
                    log.info("loadZip - instradamento a tabella CHARGES: {}", entryName);
                    parseAndLoadCharges(limitato, loadingId, loadingTime);
                }

                totale += limitato.getLetti();
                if (totale > MAX_TOTAL_SIZE) {
                    throw new ZipNonValidoException("Contenuto decompresso troppo grande");
                }

                zis.closeEntry();
                currentEntry = null;
            }
        } catch (ZipNonValidoException e) {
            log.error("loadZip - ZIP non valido durante l'elaborazione di '{}': {}", currentEntry, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("loadZip - File rifiutato: '{}'. Caricamento ZIP abortito: {}", currentEntry, e.getMessage(), e);
            throw new RuntimeException("Errore nel caricamento del file ZIP. File rifiutato: " + currentEntry
                    + " - " + e.getMessage(), e);
        }

        log.info("loadZip - END: {} entry processate, loadingId: {}", count, loadingId);
    }

    /**
     * Logica di parsing CSV condivisa tra caricamento diretto ({@link #loadCsv})
     * e caricamento da ZIP ({@link #loadZip}).
     * <p>
     * <b>Non chiude {@code inputStream}</b>: è di proprietà del chiamante
     * (nel caso ZIP appartiene allo {@link ZipInputStream}, chiuderlo qui
     * impedirebbe di passare alla entry successiva).
     */
    private void parseAndLoadCsv(InputStream inputStream, String loadingId, LocalDateTime loadingTime) throws IOException, CsvValidationException {
        InputStream shielded = new CloseShieldInputStream(inputStream);
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(shielded, Charset.forName(csvEncoding)));
        CSVReader csvReader = new CSVReaderBuilder(bufferedReader).build();

        try {
            Map<String, Integer> headerMap;
            if (csvHasHeader) {
                String[] headerRow = csvReader.readNext();
                headerMap = buildHeaderMap(headerRow);
                log.info("parseAndLoadCsv - CSV with header: {} colonne riconosciute (fallback a statica per colonne non trovate)",
                        headerMap.size());
            } else {
                headerMap = csvColumnIndex;
                log.info("parseAndLoadCsv - CSV without header (usa mapping statico): {} colonne", headerMap.size());
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
                        log.debug("parseAndLoadCsv - Salvate {} righe (ultima: {})", BATCH_SIZE, currentRowNum);
                    }

                } catch (Exception e) {
                    log.warn("parseAndLoadCsv - Riga {} scartata: {}", currentRowNum, e.getMessage());

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
                            .sourceType("INVOICE")
                            .build();

                    invoiceDiscardRepository.save(discard);
                }
            }

            if (!batch.isEmpty()) {
                invoiceStRepository.saveAll(batch);
                log.debug("parseAndLoadCsv - Salvate ultime {} righe", batch.size());
            }
        } finally {
            try {
                csvReader.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Parsing CSV per la tabella {@code usage}.
     * <p>
     * I campi data sono memorizzati come stringhe (coerentemente con lo schema
     * della tabella), mentre {@code ingestTime} viene valorizzato con l'istante
     * corrente e {@code sourceFilename} con il nome del file CSV di origine.
     * <p>
     * <b>Non chiude {@code inputStream}</b>: è di proprietà del chiamante.
     */
    private void parseAndLoadUsage(InputStream inputStream, String loadingId, LocalDateTime loadingTime,
                                   String sourceFilename) throws IOException, CsvValidationException {
        InputStream shielded = new CloseShieldInputStream(inputStream);
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(shielded, Charset.forName(csvEncoding)));
        CSVReader csvReader = new CSVReaderBuilder(bufferedReader).build();

        try {
            Map<String, Integer> headerMap;
            if (csvHasHeader) {
                String[] headerRow = csvReader.readNext();
                headerMap = buildHeaderMap(headerRow);
                log.info("parseAndLoadUsage - CSV with header: {} colonne riconosciute", headerMap.size());
            } else {
                headerMap = csvColumnIndex;
                log.info("parseAndLoadUsage - CSV without header (usa mapping statico): {} colonne", headerMap.size());
            }

            String[] row;
            long rowNum = 1;
            List<Usage> batch = new ArrayList<>(BATCH_SIZE);

            while ((row = csvReader.readNext()) != null) {
                final String[] currentRow = row;
                final long currentRowNum = rowNum++;

                try {
                    Usage entity = Usage.builder()
                            .loadingId(loadingId)
                            .rowNum(currentRowNum)
                            .loadingTime(loadingTime)
                            .sourceFilename(sourceFilename)
                            .ingestTime(java.time.OffsetDateTime.now())
                            .invoiceNumber(getField(currentRow, headerMap, "invoice_number"))
                            .invoiceDate(getField(currentRow, headerMap, "invoice_date"))
                            .billingAccountNumber(getField(currentRow, headerMap, "billing_account_number"))
                            .endCustomerId(getField(currentRow, headerMap, "end_customer_id"))
                            .endCustomerName(getField(currentRow, headerMap, "end_customer_name"))
                            .siteConnectivityId(getField(currentRow, headerMap, "site_connectivity_id"))
                            .siteName(getField(currentRow, headerMap, "site_name"))
                            .networkSliceId(getField(currentRow, headerMap, "network_slice_id"))
                            .imsi(getField(currentRow, headerMap, "imsi"))
                            .additionalImsi(getField(currentRow, headerMap, "additonal_imsi"))
                            .productOfferingId(getField(currentRow, headerMap, "product_offering_id"))
                            .type(getField(currentRow, headerMap, "type"))
                            .sharedPoolId(getField(currentRow, headerMap, "shared_pool_id"))
                            .usageGb(getField(currentRow, headerMap, "usage_gb"))
                            .date(getField(currentRow, headerMap, "date"))
                            .build();

                    batch.add(entity);

                    if (batch.size() >= BATCH_SIZE) {
                        usageRepository.saveAll(batch);
                        batch.clear();
                        log.debug("parseAndLoadUsage - Salvate {} righe (ultima: {})", BATCH_SIZE, currentRowNum);
                    }

                } catch (Exception e) {
                    log.warn("parseAndLoadUsage - Riga {} scartata: {}", currentRowNum, e.getMessage());

                    if (!batch.isEmpty()) {
                        usageRepository.saveAll(batch);
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
                            .sourceType("USAGE")
                            .build();

                    invoiceDiscardRepository.save(discard);
                }
            }

            if (!batch.isEmpty()) {
                usageRepository.saveAll(batch);
                log.debug("parseAndLoadUsage - Salvate ultime {} righe", batch.size());
            }
        } finally {
            try {
                csvReader.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Parsing CSV per la tabella {@code charges}.
     * <p>
     * I campi data ({@code invoiceDate}, {@code startDate}, {@code endDate},
     * {@code date}) vengono parsati da stringa a {@link LocalDateTime} usando
     * formati comuni; se il parsing fallisce il campo viene impostato a
     * {@code null} per non bloccare il caricamento.
     * <p>
     * <b>Non chiude {@code inputStream}</b>: è di proprietà del chiamante.
     */
    private void parseAndLoadCharges(InputStream inputStream, String loadingId, LocalDateTime loadingTime) throws IOException, CsvValidationException {
        InputStream shielded = new CloseShieldInputStream(inputStream);
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(shielded, Charset.forName(csvEncoding)));
        CSVReader csvReader = new CSVReaderBuilder(bufferedReader).build();

        try {
            Map<String, Integer> headerMap;
            if (csvHasHeader) {
                String[] headerRow = csvReader.readNext();
                headerMap = buildHeaderMap(headerRow);
                log.info("parseAndLoadCharges - CSV with header: {} colonne riconosciute", headerMap.size());
            } else {
                headerMap = csvColumnIndex;
                log.info("parseAndLoadCharges - CSV without header (usa mapping statico): {} colonne", headerMap.size());
            }

            String[] row;
            long rowNum = 1;
            List<Charges> batch = new ArrayList<>(BATCH_SIZE);

            while ((row = csvReader.readNext()) != null) {
                final String[] currentRow = row;
                final long currentRowNum = rowNum++;

                try {
                    Charges entity = Charges.builder()
                            .invoiceNumber(getField(currentRow, headerMap, "invoice_number"))
                            .invoiceDate(parseLocalDateTime(getField(currentRow, headerMap, "invoice_date")))
                            .billingAccountNumber(getField(currentRow, headerMap, "billing_account_number"))
                            .billingAccountName(getField(currentRow, headerMap, "billing_account_name"))
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
                            .qty(getField(currentRow, headerMap, "qty"))
                            .startDate(getField(currentRow, headerMap, "start_date"))
                            .endDate(getField(currentRow, headerMap, "end_date"))
                            .entitlementGb(getField(currentRow, headerMap, "entitlement_gb"))
                            .sharedPoolId(getField(currentRow, headerMap, "shared_pool_id"))
                            .usageGb(getField(currentRow, headerMap, "usage_gb"))
                            .date(getField(currentRow, headerMap, "date"))
                            .currency(getField(currentRow, headerMap, "currency"))
                            .amount(getField(currentRow, headerMap, "amount"))
                            .loadingId(loadingId)
                            .loadingTime(loadingTime)
                            .rowNum(currentRowNum)
                            .build();

                    batch.add(entity);

                    if (batch.size() >= BATCH_SIZE) {
                        chargesRepository.saveAll(batch);
                        batch.clear();
                        log.debug("parseAndLoadCharges - Salvate {} righe (ultima: {})", BATCH_SIZE, currentRowNum);
                    }

                } catch (Exception e) {
                    log.warn("parseAndLoadCharges - Riga {} scartata: {}", currentRowNum, e.getMessage());

                    if (!batch.isEmpty()) {
                        chargesRepository.saveAll(batch);
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
                            .sourceType("CHARGES")
                            .build();

                    invoiceDiscardRepository.save(discard);
                }
            }

            if (!batch.isEmpty()) {
                chargesRepository.saveAll(batch);
                log.debug("parseAndLoadCharges - Salvate ultime {} righe", batch.size());
            }
        } finally {
            try {
                csvReader.close();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Prova a parsare una stringa in {@link LocalDateTime} usando una lista di
     * formati comuni. Restituisce {@code null} se il valore è vuoto o se nessun
     * formato corrisponde, così da non bloccare il caricamento.
     */
    private LocalDateTime parseLocalDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();

        String[] patterns = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd",
                "dd/MM/yyyy HH:mm:ss",
                "dd/MM/yyyy",
                "MM/dd/yyyy HH:mm:ss",
                "MM/dd/yyyy"
        };
        for (String pattern : patterns) {
            try {
                DateTimeFormatter f = DateTimeFormatter.ofPattern(pattern);
                if (pattern.contains("HH") || pattern.contains("mm") || pattern.contains("ss")) {
                    return LocalDateTime.parse(trimmed, f);
                } else {
                    return java.time.LocalDate.parse(trimmed, f).atStartOfDay();
                }
            } catch (DateTimeParseException ignored) {
                // prova il formato successivo
            }
        }
        // Ultimo tentativo con formato ISO
        try {
            return java.time.LocalDate.parse(trimmed, DateTimeFormatter.ISO_DATE).atStartOfDay();
        } catch (DateTimeParseException ignored) {
            log.warn("parseLocalDateTime - impossibile parsare la data: {}", trimmed);
            return null;
        }
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

    /**
     * Wrapper che ignora la chiamata a {@link #close()}.
     * Serve per evitare che la chiusura di {@link com.opencsv.CSVReader}
     * si propaghi fino allo stream sottostante (es. {@link ZipInputStream}),
     * che invece deve restare aperto per processare le entry successive.
     */
    private static class CloseShieldInputStream extends FilterInputStream {

        CloseShieldInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() {
            // no-op: non propagare la close allo stream reale
        }
    }

    /**
     * Conta i byte decompressi e si blocca oltre il limite impostato
     * (protezione anti zip-bomb).
     */
    private static class LimitedInputStream extends FilterInputStream {

        private final long max;
        private final String nome;
        private long letti;

        LimitedInputStream(InputStream in, long max, String nome) {
            super(in);
            this.max = max;
            this.nome = nome;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b != -1) conta(1);
            return b;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            int n = super.read(buf, off, len);
            if (n > 0) conta(n);
            return n;
        }

        private void conta(int n) {
            letti += n;
            if (letti > max) {
                throw new ZipNonValidoException("File troppo grande: " + nome);
            }
        }

        long getLetti() {
            return letti;
        }
    }

}
