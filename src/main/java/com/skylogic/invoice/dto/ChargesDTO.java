package com.skylogic.invoice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChargesDTO {

    private Long chargesPk;
    private String invoiceNumber;
    private String invoiceDate;
    private String billingAccountNumber;
    private String billingAccountName;
    private String endCustomerId;
    private String endCustomerName;
    private String siteConnectivityId;
    private String siteName;
    private String orderNumber;
    private String poReference;
    private String networkSliceId;
    private String serviceId;
    private String imsi;
    private String additionalImsi;
    private String apn;
    private String productIdentifier;
    private String productOfferingId;
    private String name;
    private String type;
    private String rate;
    private String qty;
    private String startDate;
    private String endDate;
    private String entitlementGb;
    private String sharedPoolId;
    private String usageGb;
    private String date;
    private String currency;
    private String amount;
    private String loadingId;
    private LocalDateTime loadingTime;
}
