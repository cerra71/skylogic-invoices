package com.skylogic.invoice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "charges", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Charges {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "charges_pk")
    private Long chargesPk;

    @Column(name = "invoice_number", length = 4000)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDateTime invoiceDate;

    @Column(name = "billing_account_number", length = 4000)
    private String billingAccountNumber;

    @Column(name = "billing_account_name", length = 4000)
    private String billingAccountName;

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

    @Column(name = "qty", length = 4000)
    private String qty;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "entitlement_gb", length = 4000)
    private String entitlementGb;

    @Column(name = "shared_pool_id", length = 4000)
    private String sharedPoolId;

    @Column(name = "usage_gb", length = 4000)
    private String usageGb;

    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "currency", length = 4000)
    private String currency;

    @Column(name = "amount", length = 4000)
    private String amount;
}