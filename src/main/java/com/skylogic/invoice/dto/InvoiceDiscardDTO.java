/**
 * Data Transfer Object for InvoiceDiscard.
 * It contains discarded invoice rows together with
 * the related loading and error information.
 */

package com.skylogic.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDiscardDTO {

    private String loadingId;
    private Long rowNum;
    private String discardedRow;
    private String error;
}