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
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceStRepository extends JpaRepository<InvoiceSt, InvoiceRowId> {
}