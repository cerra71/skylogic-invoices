package com.skylogic.invoice.repository;

import com.skylogic.invoice.entity.Charges;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargesRepository extends JpaRepository<Charges, Long> {

    List<Charges> findByBillingAccountNumber(String billingAccountNumber);

    List<Charges> findBySiteConnectivityId(String siteConnectivityId);

    List<Charges> findByInvoiceNumber(String invoiceNumber);

    Page<Charges> findByLoadingIdOrderByRowNumAsc(
            String loadingId,
            Pageable pageable
    );


    @Query(value = """
        SELECT *
        FROM charges c
        WHERE (:billingAccountNumber IS NULL OR :billingAccountNumber = '' OR UPPER(c.billing_account_number) LIKE UPPER(CONCAT('%', :billingAccountNumber, '%')))
          AND (:siteConnectivityId IS NULL OR :siteConnectivityId = '' OR UPPER(c.site_connectivity_id) LIKE UPPER(CONCAT('%', :siteConnectivityId, '%')))
          AND (:invoiceNumber IS NULL OR :invoiceNumber = '' OR UPPER(c.invoice_number) LIKE UPPER(CONCAT('%', :invoiceNumber, '%')))
        ORDER BY c.charges_pk
        """, nativeQuery = true)
    List<Charges> search(@Param("billingAccountNumber") String billingAccountNumber,
                         @Param("siteConnectivityId") String siteConnectivityId,
                         @Param("invoiceNumber") String invoiceNumber);




    /**
     * Cerca le righe di charges applicando solo i filtri compilati.
     * I filtri si combinano e i risultati sono paginati.
     */
    @Query("""
    SELECT i
    FROM Charges i
    WHERE i.loadingId = :loadingId
      AND (:billingAccountNumber = ''
           OR LOWER(i.billingAccountNumber)
              LIKE LOWER(CONCAT('%', :billingAccountNumber, '%')))
      AND (:siteConnectivityId = ''
           OR LOWER(i.siteConnectivityId)
              LIKE LOWER(CONCAT('%', :siteConnectivityId, '%')))
      AND (:usageGb = ''
           OR i.usageGb = :usageGb)
    ORDER BY i.rowNum ASC
    """)
    Page<Charges> searchChargesRows(
            @Param("loadingId") String loadingId,
            @Param("billingAccountNumber") String billingAccountNumber,
            @Param("siteConnectivityId") String siteConnectivityId,
            @Param("usageGb") String usageGb,
            Pageable pageable
    );

    String NUMERIC_FILTER_SQL = """
    FROM public.charges i
    CROSS JOIN LATERAL (
        SELECT
            CASE
                WHEN TRIM(i.entitlement_gb) ~ '^[+-]?[0-9]+(\\\\.[0-9]+)?$'
                THEN CAST(TRIM(i.entitlement_gb) AS NUMERIC)
                ELSE NULL
            END AS entitlement_value,
    
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
          :entitlementGb IS NULL
          OR CASE :entitlementOperator
              WHEN 'EQ' THEN
                  gb.entitlement_value = CAST(:entitlementGb AS NUMERIC)
              WHEN 'GT' THEN
                  gb.entitlement_value > CAST(:entitlementGb AS NUMERIC)
                  OR (CAST(:entitlementInclusive AS BOOLEAN) IS TRUE
                      AND gb.entitlement_value = CAST(:entitlementGb AS NUMERIC))
              WHEN 'LT' THEN
                  gb.entitlement_value < CAST(:entitlementGb AS NUMERIC)
                  OR (CAST(:entitlementInclusive AS BOOLEAN) IS TRUE
                      AND gb.entitlement_value = CAST(:entitlementGb AS NUMERIC))
              ELSE FALSE
          END
      )

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
     * Cerca le righe di charges con confronti numerici sui GB.
     * Un valore GB null disattiva il relativo filtro.
     */
    @Query(
            value = "SELECT i.* " + NUMERIC_FILTER_SQL + " ORDER BY i.row_num ASC",
            countQuery = "SELECT COUNT(*) " + NUMERIC_FILTER_SQL,
            nativeQuery = true
    )
    Page<Charges> searchChargesRowsNumeric(
            @Param("loadingId") String loadingId,
            @Param("billingAccountNumber") String billingAccountNumber,
            @Param("siteConnectivityId") String siteConnectivityId,
            @Param("entitlementGb") Long entitlement,
            @Param("entitlementOperator") String entitlementOperator,
            @Param("entitlementInclusive") boolean entitlementInclusive,
            @Param("usageGb") Long usageGb,
            @Param("usageOperator") String usageOperator,
            @Param("usageInclusive") boolean usageInclusive,
            Pageable pageable
    );
}
