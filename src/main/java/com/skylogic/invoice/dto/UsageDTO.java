package com.skylogic.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsageDTO {

    private String invoiceNumber;
    private String invoiceDate;
    private String billingAccountNumber;
    private String endCustomerId;
    private String endCustomerName;
    private String siteConnectivityId;
    private String siteName;
    private String networkSliceId;
    private String imsi;
    private String additionalImsi;
    private String productOfferingId;
    private String type;
    private String sharedPoolId;
    private String usageGb;
    private String date;
    private OffsetDateTime ingestTime;
    private String sourceFilename;
}
