/**
 * Entity representing a single record of usage table.
 * It contains the day-by-day usage of a single IMSI.
 */
package com.skylogic.invoice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "usage", schema = "public")
@IdClass(UsageId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usage {

    @Column(name = "invoice_number", length = 4000)
    private String invoiceNumber;

    @Column(name = "invoice_date", length = 4000)
    private String invoiceDate;

    @Id
    @Column(name = "billing_account_number", length = 4000)
    private String billingAccountNumber;

    @Column(name = "end_customer_id", length = 4000)
    private String endCustomerId;

    @Column(name = "end_customer_name", length = 4000)
    private String endCustomerName;

    @Id
    @Column(name = "site_connectivity_id", length = 4000)
    private String siteConnectivityId;

    @Column(name = "site_name", length = 4000)
    private String siteName;

    @Column(name = "order_number", length = 4000)
    private String orderNumber;

    @Column(name = "network_slice_id", length = 4000)
    private String networkSliceId;

    @Column(name = "imsi", length = 4000)
    private String imsi;

    @Column(name = "additional_imsi", length = 4000)
    private String additionalImsi;

    @Column(name = "product_offering_id", length = 4000)
    private String productOfferingId;

    @Column(name = "type", length = 4000)
    private String type;

    @Column(name = "shared_pool_id", length = 4000)
    private String sharedPoolId;

    @Column(name = "usage_gb", length = 4000)
    private String usageGb;

    @Id
    @Column(name = "date", length = 4000)
    private String date;

    @Column(name = "ingest_time")
    private OffsetDateTime ingestTime;

    @Column(name = "source_filename", length = 4000)
    private String sourceFilename;
}

