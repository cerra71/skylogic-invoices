CREATE TABLE IF NOT EXISTS kpi_documentation (
    id              BIGSERIAL PRIMARY KEY,
    kpi_id          VARCHAR(255) NOT NULL,
    control_id      VARCHAR(255) NOT NULL,
    title           TEXT NOT NULL,
    field           VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kpi_documentation_kpi_id     ON kpi_documentation(kpi_id);
CREATE INDEX IF NOT EXISTS idx_kpi_documentation_control_id ON kpi_documentation(control_id);
CREATE INDEX IF NOT EXISTS idx_kpi_documentation_field      ON kpi_documentation(field);

-- TUTTI i controlli implementati finora


INSERT INTO kpi_documentation (kpi_id, control_id, title, field)
VALUES
    (
        'KPI_48',
        'RA-QA-01',
        'INVOICE_NUMBER_CONTAINS_MORE_THAN_1_UNIQUE_NUMBER - Individuare le righe in cui INVOICE_NUMBER contiene più di un numero distinto, cioè più di una sequenza numerica secondo la documentazione del KPI.',
        'invoiceNumber'
    ),
    (
        'KPI_41',
        'RA-QA-02',
        'INVOICE_DATE_IS_NOT_SAME_AS_THE_BILL_RUN_MONTH - Controllo qualità del campo INVOICE DATE: verificare che la data sia la stessa per TUTTE le righe del bill run corrente (una sola data distinta per loading_id).',
        'invoiceDate'
    ),
    (
        'KPI_35',
        'RA-QA-93',
        'CURRENCY_COLUMN_CONTAINS_BLANK_SITECONNECTIVITY_MRC - Validazione che la colonna CURRENCY contenga USD per le righe Site Connectivity MRC e non sia vuota.',
        'currency'
    ),
    (
        'KPI_4_1',
        'RA-QA-04',
        'END_CUSTOMER_ID_IS_NULL - Individuare le righe in cui END_CUSTOMER_ID è Null, Blank o composto da soli spazi quando è presente SITE_CONNECTIVITY_ID.',
        'endCustomerId'
    ),
    (
        'KPI_7',
        'RA-QA-07',
        'TO_CHECK_FOR_ANY_EMPTY_CELLS_FOR_SITE_CONNECTIVITY_ID - Individuare le righe in cui Site Connectivity Id è Null, Blank o composto da soli spazi, ad eccezione delle tipologie di riga escluse (Cross Connect Interconnect, Interconnect MRC, Network Slice Onetime, Take or Pay, PoP-PoP Discount, Revenue Share Discount, Mini POP MRC, Sale of Satellite Connectivity *).',
        'siteConnectivityId'
    )
ON CONFLICT DO NOTHING;
