/**
 * Repository responsible for persistence operations on {@link InvoiceDiscard} entities.
 * The entity uses a composite primary key represented by
 * {@link InvoiceRowId}. Each discarded invoice row is uniquely identified by
 * the combination of {@code loadingId} and {@code rowNum}, rather than by a
 * single identifier.
 */

package com.skylogic.invoice.repository;

import com.skylogic.invoice.entity.InvoiceDiscard;
import com.skylogic.invoice.entity.InvoiceRowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceDiscardRepository extends JpaRepository<InvoiceDiscard, InvoiceRowId> {

    long countByLoadingId(String loadingId);
}