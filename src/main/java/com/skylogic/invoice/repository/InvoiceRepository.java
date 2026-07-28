/**
 * Repository responsible for persistence operations on {@link Invoice} entities.
 * The entity uses a composite primary key represented by
 * {@link InvoiceRowId}. Each invoice row is therefore identified by the
 * combination of {@code loadingId} and {@code rowNum}, rather than by a
 * single identifier.
 */

package com.skylogic.invoice.repository;

import com.skylogic.invoice.entity.Invoice;
import com.skylogic.invoice.entity.InvoiceRowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, InvoiceRowId> {
}