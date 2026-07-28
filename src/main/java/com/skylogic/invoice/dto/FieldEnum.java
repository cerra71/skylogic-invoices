package com.skylogic.invoice.dto;


public enum FieldEnum {
    invoiceNumber("invoiceNumber"),
    invoiceDate("invoiceDate"),
    billingAccountNumber("billingAccountNumber"),
    endCustomerId("endCustomerId"),
    endCustomerName("endCustomerName"),
    siteConnectivityId("siteConnectivityId"),
    siteName("siteName"),
    orderNumber("orderNumber"),
    poReference("poReference"),
    networkSliceId("networkSliceId"),
    serviceId("serviceId"),
    imsi("imsi"),
    additionalImsi("additionalImsi"),
    apn("apn"),
    productIdentifier("productIdentifier"),
    productOfferingId("productOfferingId"),
    name("name"),
    type("type"),
    rate("rate"),
    startDate("startDate"),
    endDate("endDate"),
    entitlementGb("entitlementGb"),
    sharedPoolId("sharedPoolId"),
    usageGb("usageGb"),
    date("date"),
    currency("currency"),
    amount("amount"),
    loadingId("loadingId"),
    rowNum("rowNum"),
    loadingTime("loadingTime");

    private final String value;

    FieldEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
