CREATE TABLE charges (
    charges_pk SERIAL PRIMARY KEY,

    invoice_number VARCHAR(4000),
    invoice_date DATE,

    billing_account_number VARCHAR(4000),
    billing_account_name VARCHAR(4000),

    end_customer_id VARCHAR(4000),
    end_customer_name VARCHAR(4000),

    site_connectivity_id VARCHAR(4000),
    site_name VARCHAR(4000),

    order_number VARCHAR(4000),
    po_reference VARCHAR(4000),

    network_slice_id VARCHAR(4000),
    service_id VARCHAR(4000),

    imsi VARCHAR(4000),
    additional_imsi VARCHAR(4000),

    apn VARCHAR(4000),

    product_identifier VARCHAR(4000),
    product_offering_id VARCHAR(4000),

    name VARCHAR(4000),
    type VARCHAR(4000),

    rate VARCHAR(4000),
    qty VARCHAR(4000),

    start_date DATE,
    end_date DATE,

    entitlement_gb VARCHAR(4000),
    shared_pool_id VARCHAR(4000),

    usage_gb VARCHAR(4000),

    date DATE,

    currency VARCHAR(4000),
    amount VARCHAR(4000)
);