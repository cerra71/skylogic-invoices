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
     * Elimina tutte le righe di staging associate a un dato loading_id.
     */
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM public.invoice_st WHERE loading_id = :loadingId", nativeQuery = true)
    void deleteByLoadingId(@Param("loadingId") String loadingId);
}