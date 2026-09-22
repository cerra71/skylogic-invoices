package com.skylogic.invoice.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KpiDocumentationDTO {

    @NotBlank(message = "KPI ID is required")
    private String kpiId;

    @NotBlank(message = "Control ID is required")
    private String controlId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Field is required")
    private String field;
}