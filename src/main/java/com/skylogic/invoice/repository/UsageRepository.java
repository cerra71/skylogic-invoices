package com.skylogic.invoice.repository;

import com.skylogic.invoice.entity.InvoiceSt;
import com.skylogic.invoice.entity.Usage;
import com.skylogic.invoice.entity.UsageId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsageRepository extends JpaRepository<Usage, UsageId> {

    /**
     * Recupera una pagina delle righe del CSV selezionato,
     * ordinate per numero di riga crescente.
     *
     * @param loadingId identificativo del CSV caricato
     * @param pageable pagina richiesta e numero di righe per pagina
     * @return righe della pagina e informazioni sul totale dei risultati
     */
    Page<Usage> findByLoadingIdOrderByRowNumAsc(
            String loadingId,
            Pageable pageable
    );

    /**
     * Cerca le righe di invoice_st applicando solo i filtri compilati.
     * I filtri si combinano e i risultati sono paginati.
     */
    @Query("""
    SELECT i
    FROM Usage i
    WHERE i.loadingId = :loadingId
      AND (:billingAccountNumber = ''
           OR LOWER(i.billingAccountNumber)
              LIKE LOWER(CONCAT('%', :billingAccountNumber, '%')))
      AND (:siteConnectivityId = ''
           OR LOWER(i.siteConnectivityId)
              LIKE LOWER(CONCAT('%', :siteConnectivityId, '%')))
      AND (:entitlementGb = ''
           OR i.entitlementGb = :entitlementGb)
      AND (:usageGb = ''
           OR i.usageGb = :usageGb)
    ORDER BY i.rowNum ASC
    """)
    Page<InvoiceSt> searchInvoiceStRows(
            @Param("loadingId") String loadingId,
            @Param("billingAccountNumber") String billingAccountNumber,
            @Param("siteConnectivityId") String siteConnectivityId,
            @Param("usageGb") String usageGb,
            Pageable pageable
    );

    String NUMERIC_FILTER_SQL = """
    FROM public.usage i
    CROSS JOIN LATERAL (
        SELECT
            CASE
                WHEN TRIM(i.usage_gb) ~ '^[+-]?[0-9]+(\\.[0-9]+)?$'
                THEN CAST(TRIM(i.usage_gb) AS NUMERIC)
                ELSE NULL
            END AS usage_value
    ) gb
    WHERE i.loading_id = :loadingId
      AND (:billingAccountNumber = ''
           OR i.billing_account_number
              ILIKE CONCAT('%', :billingAccountNumber, '%'))
      AND (:siteConnectivityId = ''
           OR i.site_connectivity_id
              ILIKE CONCAT('%', :siteConnectivityId, '%'))

      AND (
          :usageGb IS NULL
          OR CASE :usageOperator
              WHEN 'EQ' THEN
                  gb.usage_value = CAST(:usageGb AS NUMERIC)
              WHEN 'GT' THEN
                  gb.usage_value > CAST(:usageGb AS NUMERIC)
                  OR (CAST(:usageInclusive AS BOOLEAN) IS TRUE
                      AND gb.usage_value = CAST(:usageGb AS NUMERIC))
              WHEN 'LT' THEN
                  gb.usage_value < CAST(:usageGb AS NUMERIC)
                  OR (CAST(:usageInclusive AS BOOLEAN) IS TRUE
                      AND gb.usage_value = CAST(:usageGb AS NUMERIC))
              ELSE FALSE
          END
      )
    """;

    /**
     * Cerca le righe di usage con confronti numerici sui GB.
     * Un valore GB null disattiva il relativo filtro.
     */
    @Query(
            value = "SELECT i.* " + NUMERIC_FILTER_SQL + " ORDER BY i.row_num ASC",
            countQuery = "SELECT COUNT(*) " + NUMERIC_FILTER_SQL,
            nativeQuery = true
    )
    Page<Usage> searchUsageRowsNumeric(
            @Param("loadingId") String loadingId,
            @Param("billingAccountNumber") String billingAccountNumber,
            @Param("siteConnectivityId") String siteConnectivityId,
            @Param("usageGb") Long usageGb,
            @Param("usageOperator") String usageOperator,
            @Param("usageInclusive") boolean usageInclusive,
            Pageable pageable
    );



}