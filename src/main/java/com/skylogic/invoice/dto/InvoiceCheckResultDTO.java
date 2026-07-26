package com.skylogic.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceCheckResultDTO {

    private String fieldName;

    private String fieldValue;
    
    private Boolean passed;
    
    private String checkFailed;
}
