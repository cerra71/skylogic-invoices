/**
 * Repository responsible for persistence operations on {@link InvoiceSt} entities.
 * The entity uses a composite primary key represented by
 * {@link InvoiceRowId}. Each staging row is uniquely identified by the
 * combination of {@code loadingId} and {@code rowNum}, rather than by a
 * single identifier.
 */

package com.skylogic.invoice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.skylogic.invoice.dto.LoadingSummaryInterface;
import com.skylogic.invoice.entity.InvoiceRowId;
import com.skylogic.invoice.entity.InvoiceSt;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface InvoiceStRepository extends JpaRepository<InvoiceSt, InvoiceRowId> {

    /**
     * Restituisce una riga per ogni loading_id con loading_time e conteggio righe,
     * ordinati per loading_time discendente.
     * Colonne risultato: [0] loading_id (String), [1] loading_time (Timestamp), [2] count (Long)
     */
    @Query(value = "SELECT loading_id, SUBSTRING(MAX(invoice_date), 0, 5) AS reportingYear, SUBSTRING(MAX(invoice_date), 6, 2) AS reportingMonth, MAX(loading_time) as loading_time, COUNT(*) as loadedRows FROM public.invoice_st GROUP BY loading_id ORDER BY MAX(loading_time) DESC",
           nativeQuery = true)
    List<LoadingSummaryInterface> findLoadingSummaries();

    /**
     * Restituisce tutte le righe di staging associate al loading indicato,
     * ordinate per numero di riga crescente.
     *
     * @param loadingId identificativo del CSV caricato
     * @return righe presenti in invoice_st per il loading selezionato
     */
    List<InvoiceSt> findByLoadingIdOrderByRowNumAsc(String loadingId);

    /**
     * Conta il numero di righe in invoice_st per un determinato loading_id
     * usando una query nativa (più sicura con @IdClass composito).
     *
     * @param loadingId identificativo del caricamento
     * @return conteggio righe invoice_st per il loading selezionato
     */
    @Query(value = "SELECT COUNT(*) FROM public.invoice_st WHERE loading_id = :loadingId", nativeQuery = true)
    long countByLoadingId(@Param("loadingId") String loadingId);

    /**
     * Elimina tutte le righe di staging associate a un dato loading_id.
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM public.invoice_st WHERE loading_id = :loadingId", nativeQuery = true)
    void deleteByLoadingId(@Param("loadingId") String loadingId);


    /**
     * Recupera una pagina delle righe del CSV selezionato,
     * ordinate per numero di riga crescente.
     *
     * @param loadingId identificativo del CSV caricato
     * @param pageable pagina richiesta e numero di righe per pagina
     * @return righe della pagina e informazioni sul totale dei risultati
     */
    Page<InvoiceSt> findByLoadingIdOrderByRowNumAsc(
            String loadingId,
            Pageable pageable
    );

    /**
     * Cerca le righe di invoice_st applicando solo i filtri compilati.
     * I filtri si combinano e i risultati sono paginati.
     */
    @Query("""
    SELECT i
    FROM InvoiceSt i
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
            @Param("entitlementGb") String entitlementGb,
            @Param("usageGb") String usageGb,
            Pageable pageable
    );

    String NUMERIC_FILTER_SQL = """
    FROM public.invoice_st i
    CROSS JOIN LATERAL (
        SELECT
            CASE
                WHEN TRIM(i.entitlement_gb) ~ '^[+-]?[0-9]+$'
                THEN CAST(TRIM(i.entitlement_gb) AS NUMERIC)
                ELSE NULL
            END AS entitlement_value,
            CASE
                WHEN TRIM(i.usage_gb) ~ '^[+-]?[0-9]+$'
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
          CAST(:entitlementGb AS NUMERIC) IS NULL
          OR CASE :entitlementOperator
              WHEN 'EQ' THEN
                  gb.entitlement_value = CAST(:entitlementGb AS NUMERIC)
              WHEN 'GT' THEN
                  gb.entitlement_value > CAST(:entitlementGb AS NUMERIC)
                  OR (:entitlementInclusive AND
                      gb.entitlement_value = CAST(:entitlementGb AS NUMERIC))
              WHEN 'LT' THEN
                  gb.entitlement_value < CAST(:entitlementGb AS NUMERIC)
                  OR (:entitlementInclusive AND
                      gb.entitlement_value = CAST(:entitlementGb AS NUMERIC))
              ELSE FALSE
          END
      )

      AND (
          CAST(:usageGb AS NUMERIC) IS NULL
          OR CASE :usageOperator
              WHEN 'EQ' THEN
                  gb.usage_value = CAST(:usageGb AS NUMERIC)
              WHEN 'GT' THEN
                  gb.usage_value > CAST(:usageGb AS NUMERIC)
                  OR (:usageInclusive AND
                      gb.usage_value = CAST(:usageGb AS NUMERIC))
              WHEN 'LT' THEN
                  gb.usage_value < CAST(:usageGb AS NUMERIC)
                  OR (:usageInclusive AND
                      gb.usage_value = CAST(:usageGb AS NUMERIC))
              ELSE FALSE
          END
      )
    """;

    /**
     * Cerca le righe di invoice_st con confronti numerici sui GB.
     * Un valore GB null disattiva il relativo filtro.
     */
    @Query(
            value = "SELECT i.* " + NUMERIC_FILTER_SQL + " ORDER BY i.row_num ASC",
            countQuery = "SELECT COUNT(*) " + NUMERIC_FILTER_SQL,
            nativeQuery = true
    )
    Page<InvoiceSt> searchInvoiceStRowsNumeric(
            @Param("loadingId") String loadingId,
            @Param("billingAccountNumber") String billingAccountNumber,
            @Param("siteConnectivityId") String siteConnectivityId,
            @Param("entitlementGb") Long entitlementGb,
            @Param("entitlementOperator") String entitlementOperator,
            @Param("entitlementInclusive") boolean entitlementInclusive,
            @Param("usageGb") Long usageGb,
            @Param("usageOperator") String usageOperator,
            @Param("usageInclusive") boolean usageInclusive,
            Pageable pageable
    );


}