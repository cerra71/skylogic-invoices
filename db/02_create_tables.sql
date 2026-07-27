-- ============================================================
-- Script Name : 02_create_tables.sql
-- Database    : invoices
-- Schema      : public
--
-- Description :
--   Creates the following tables if they do not already exist:
--
--     - public.invoice_st
--       Staging table used to store data loaded from the source CSV.
--
--     - public.invoice
--       Main invoice table. Currently aligned with invoice_st,
--       with normalized column naming where required.
--
--     - public.invoice_discard
--       Stores CSV rows discarded during validation or loading,
--       together with the related error message.
--
-- Notes :
--   - invoice_st keeps the source column name "additonal_imsi"
--     to remain consistent with the original CSV header.
--   - invoice uses the corrected column name "additional_imsi".
--   - invoice_discard uses (loading_id, row_num) as composite PK.
--   - The script is idempotent thanks to CREATE ... IF NOT EXISTS.
--
-- Prerequisites :
--   - The "invoices" database must already exist.
--   - The script must be executed while connected to "invoices".
--
-- Example :
--   psql -d invoices -f 02_create_tables.sql
-- ============================================================

CREATE SCHEMA IF NOT EXISTS public;

-- ============================================================
-- STAGING TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS public.invoice_st (
    invoice_number              VARCHAR(4000),
    invoice_date                VARCHAR(4000),
    billing_account_number      VARCHAR(4000),
    end_customer_id             VARCHAR(4000),
    end_customer_name           VARCHAR(4000),
    site_connectivity_id        VARCHAR(4000),
    site_name                   VARCHAR(4000),
    order_number                VARCHAR(4000),
    po_reference                VARCHAR(4000),
    network_slice_id            VARCHAR(4000),
    service_id                  VARCHAR(4000),
    imsi                        VARCHAR(4000),
    additonal_imsi              VARCHAR(4000),
    apn                         VARCHAR(4000),
    product_identifier          VARCHAR(4000),
    product_offering_id         VARCHAR(4000),
    name                        VARCHAR(4000),
    type                        VARCHAR(4000),
    rate                        VARCHAR(4000),
    start_date                  VARCHAR(4000),
    end_date                    VARCHAR(4000),
    entitlement_gb              VARCHAR(4000),
    shared_pool_id              VARCHAR(4000),
    usage_gb                    VARCHAR(4000),
    date                        VARCHAR(4000),
    currency                    VARCHAR(4000),
    amount                      VARCHAR(4000),
    loading_id                  VARCHAR(32),
    row_num                     BIGINT,
    loading_time                TIMESTAMP
);

-- ============================================================
-- MAIN INVOICE TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS public.invoice (
    invoice_number              VARCHAR(4000),
    invoice_date                VARCHAR(4000),
    billing_account_number      VARCHAR(4000),
    end_customer_id             VARCHAR(4000),
    end_customer_name           VARCHAR(4000),
    site_connectivity_id        VARCHAR(4000),
    site_name                   VARCHAR(4000),
    order_number                VARCHAR(4000),
    po_reference                VARCHAR(4000),
    network_slice_id            VARCHAR(4000),
    service_id                  VARCHAR(4000),
    imsi                        VARCHAR(4000),
    additional_imsi             VARCHAR(4000),
    apn                         VARCHAR(4000),
    product_identifier          VARCHAR(4000),
    product_offering_id         VARCHAR(4000),
    name                        VARCHAR(4000),
    type                        VARCHAR(4000),
    rate                        VARCHAR(4000),
    start_date                  VARCHAR(4000),
    end_date                    VARCHAR(4000),
    entitlement_gb              VARCHAR(4000),
    shared_pool_id              VARCHAR(4000),
    usage_gb                    VARCHAR(4000),
    date                        VARCHAR(4000),
    currency                    VARCHAR(4000),
    amount                      VARCHAR(4000),
    loading_id                  VARCHAR(32),
    row_num                     BIGINT,
    loading_time                TIMESTAMP
);

-- ============================================================
-- DISCARD TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS public.invoice_discard (
    loading_id                  VARCHAR(32),
    row_num                     BIGINT,
    discarded_row               TEXT,
    error                       VARCHAR(4000),

    CONSTRAINT pk_invoice_discard
    PRIMARY KEY (loading_id, row_num)
);