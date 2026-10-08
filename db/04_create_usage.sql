-- ============================================================
-- USAGE_MERGE TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS public.usage (
    invoice_number              VARCHAR(4000),
    invoice_date                VARCHAR(4000),
    billing_account_number      VARCHAR(4000),
    end_customer_id             VARCHAR(4000),
    end_customer_name           VARCHAR(4000),
    site_connectivity_id        VARCHAR(4000),
    site_name                   VARCHAR(4000),
    network_slice_id            VARCHAR(4000),
    imsi                        VARCHAR(4000),
    additonal_imsi              VARCHAR(4000),
    product_offering_id         VARCHAR(4000),
    type                        VARCHAR(4000),
    shared_pool_id              VARCHAR(4000),
    usage_gb                    VARCHAR(4000),
    date                        VARCHAR(4000),
    ingest_time                 TIMESTAMPTZ,
    source_filename             VARCHAR(4000)

    CONSTRAINT pk_usage
    PRIMARY KEY (billing_account_number, site_connectivity_id, date)
);
