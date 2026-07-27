/**
 * Entity representing the invoice table.
 * It contains invoice data used for subsequent processing
 * and application-level checks.
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

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice", schema = "public")
@IdClass(InvoiceRowId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Column(name = "invoice_number", length = 4000)
    private String invoiceNumber;

    @Column(name = "invoice_date", length = 4000)
    private String invoiceDate;

    @Column(name = "billing_account_number", length = 4000)
    private String billingAccountNumber;

    @Column(name = "end_customer_id", length = 4000)
    private String endCustomerId;

    @Column(name = "end_customer_name", length = 4000)
    private String endCustomerName;

    @Column(name = "site_connectivity_id", length = 4000)
    private String siteConnectivityId;

    @Column(name = "site_name", length = 4000)
    private String siteName;

    @Column(name = "order_number", length = 4000)
    private String orderNumber;

    @Column(name = "po_reference", length = 4000)
    private String poReference;

    @Column(name = "network_slice_id", length = 4000)
    private String networkSliceId;

    @Column(name = "service_id", length = 4000)
    private String serviceId;

    @Column(name = "imsi", length = 4000)
    private String imsi;

    @Column(name = "additional_imsi", length = 4000)
    private String additionalImsi;

    @Column(name = "apn", length = 4000)
    private String apn;

    @Column(name = "product_identifier", length = 4000)
    private String productIdentifier;

    @Column(name = "product_offering_id", length = 4000)
    private String productOfferingId;

    @Column(name = "name", length = 4000)
    private String name;

    @Column(name = "type", length = 4000)
    private String type;

    @Column(name = "rate", length = 4000)
    private String rate;

    @Column(name = "start_date", length = 4000)
    private String startDate;

    @Column(name = "end_date", length = 4000)
    private String endDate;

    @Column(name = "entitlement_gb", length = 4000)
    private String entitlementGb;

    @Column(name = "shared_pool_id", length = 4000)
    private String sharedPoolId;

    @Column(name = "usage_gb", length = 4000)
    private String usageGb;

    @Column(name = "date", length = 4000)
    private String date;

    @Column(name = "currency", length = 4000)
    private String currency;

    @Column(name = "amount", length = 4000)
    private String amount;

    @Id
    @Column(name = "loading_id", length = 32)
    private String loadingId;

    @Id
    @Column(name = "row_num")
    private Long rowNum;

    @Column(name = "loading_time")
    private LocalDateTime loadingTime;
}