/**
 * Repository responsible for persistence operations on {@link InvoiceSt} entities.
 * The entity uses a composite primary key represented by
 * {@link InvoiceRowId}. Each staging row is uniquely identified by the
 * combination of {@code loadingId} and {@code rowNum}, rather than by a
 * single identifier.
 */

package com.skylogic.invoice.repository;

import com.skylogic.invoice.entity.InvoiceRowId;
import com.skylogic.invoice.entity.InvoiceSt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceStRepository extends JpaRepository<InvoiceSt, InvoiceRowId> {

    /**
     * Restituisce una riga per ogni loading_id con loading_time e conteggio righe,
     * ordinati per loading_time discendente.
     * Colonne risultato: [0] loading_id (String), [1] loading_time (Timestamp), [2] count (Long)
     */
    @Query(value = "SELECT loading_id, loading_time, COUNT(*) FROM public.invoice_st GROUP BY loading_id, loading_time ORDER BY loading_time DESC",
           nativeQuery = true)
    List<Object[]> findLoadingSummaries();
}