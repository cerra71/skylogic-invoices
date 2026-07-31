/**
 * DTO representing a summary of a single CSV loading, shown in the home page table.
 * Data is aggregated from invoice_st (loaded rows, loading time) and
 * invoice_discard (discarded rows).
 */

package com.skylogic.invoice.dto;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoadingSummaryInterface {

    public String loadingId = null;
    public String reportingYear = null;
    public String reportingMonth = null;
    public Timestamp loadingTime = null;
    public Long loadedRows = null;
	
}
