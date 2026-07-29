/**
 * DTO representing a summary of a single CSV loading, shown in the home page table.
 * Data is aggregated from invoice_st (loaded rows, loading time) and
 * invoice_discard (discarded rows).
 */

package com.skylogic.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoadingSummaryDTO {

    private String loadingId;
    private Integer reportingYear;
    private String reportingMonth;
    private String loadingTime;
    private Long loadedRows;
    private Long discardedRows;
}
