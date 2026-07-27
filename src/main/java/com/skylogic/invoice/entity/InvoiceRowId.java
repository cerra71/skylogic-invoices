/**
 * Class used by JPA to represent the composite identifier
 * made up of loadingId and rowNum.
 */

package com.skylogic.invoice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRowId implements Serializable {

    private String loadingId;
    private Long rowNum;
}