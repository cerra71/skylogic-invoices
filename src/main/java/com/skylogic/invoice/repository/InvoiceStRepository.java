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
     * Returns one row per loading_id with its loading_time and total row count,
     * ordered by loading_time descending.
     * Result columns: [0] loadingId (String), [1] loadingTime (LocalDateTime), [2] count (Long)
     */
    @Query("SELECT i.loadingId, i.loadingTime, COUNT(i) FROM InvoiceSt i GROUP BY i.loadingId, i.loadingTime ORDER BY i.loadingTime DESC")
    List<Object[]> findLoadingSummaries();
}