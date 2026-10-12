package com.skylogic.invoice.repository;

import com.skylogic.invoice.entity.Charges;
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

    @Query(value = """
        SELECT * FROM public.charges c
        WHERE (:billingAccountNumber IS NULL OR :billingAccountNumber = '' OR UPPER(c.billing_account_number) LIKE UPPER(CONCAT('%', :billingAccountNumber, '%')))
          AND (:siteConnectivityId IS NULL OR :siteConnectivityId = '' OR UPPER(c.site_connectivity_id) LIKE UPPER(CONCAT('%', :siteConnectivityId, '%')))
          AND (:invoiceNumber IS NULL OR :invoiceNumber = '' OR UPPER(c.invoice_number) LIKE UPPER(CONCAT('%', :invoiceNumber, '%')))
        ORDER BY c.charges_pk
        """, nativeQuery = true)
    List<Charges> search(@Param("billingAccountNumber") String billingAccountNumber,
                         @Param("siteConnectivityId") String siteConnectivityId,
                         @Param("invoiceNumber") String invoiceNumber);
}
