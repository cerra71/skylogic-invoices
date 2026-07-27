/**
 * Entity representing the invoice_discard table.
 * It contains rows discarded during data loading,
 * together with the related error information.
 */

package com.skylogic.invoice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "invoice_discard", schema = "public")
@IdClass(InvoiceRowId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDiscard {

    @Id
    @Column(name = "loading_id", length = 32, nullable = false)
    private String loadingId;

    @Id
    @Column(name = "row_num", nullable = false)
    private Long rowNum;

    @Column(name = "discarded_row", columnDefinition = "TEXT")
    private String discardedRow;

    @Column(name = "error", length = 4000)
    private String error;
}