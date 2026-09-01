package com.skylogic.invoice.check;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

import com.skylogic.invoice.dto.CheckCategoryEnum;
import com.skylogic.invoice.dto.FieldEnum;
import com.skylogic.invoice.dto.InvoiceCheckResultDTO;
import com.skylogic.invoice.dto.InvoiceStDTO;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Component
@Slf4j
@Getter
@Setter
public class RaQa02InvoiceDateBillRunCheck extends GenericCheck implements CheckI {

	private static final String SQL = """
    WITH distinct_dates AS (
        SELECT COUNT(DISTINCT NULLIF(TRIM(invoice_date), '')) AS cnt
        FROM invoice_st
        WHERE loading_id = :loadingId
    ),
    single_date AS (
        SELECT DISTINCT NULLIF(TRIM(invoice_date), '') AS d
        FROM invoice_st
        WHERE loading_id = :loadingId
          AND NULLIF(TRIM(invoice_date), '') IS NOT NULL
    )
    SELECT
        tabella_invoice_st.invoice_date,
        CASE
            WHEN tabella_invoice_st.invoice_date IS NULL
                 OR TRIM(tabella_invoice_st.invoice_date) = ''
                THEN 'Fail'
            WHEN (SELECT cnt FROM distinct_dates) <> 1
                THEN 'Fail'
            WHEN NULLIF(TRIM(tabella_invoice_st.invoice_date), '') = (SELECT d FROM single_date)
                THEN 'Passed'
            ELSE 'Fail'
        END AS result
    FROM invoice_st AS tabella_invoice_st
    WHERE tabella_invoice_st.loading_id = :loadingId
      AND tabella_invoice_st.row_num = :rowNumber
    """;

	@PostConstruct
	private void init() {
		setName("Check RA-QA-02: KPI_41_INVOICE_DATE_IS_NOT_SAME_AS_THE_BILL_RUN_MONTH");
		setDescription("Controllo di qualita del dato (KPI) relativo al campo INVOICE DATE. Verifica: RA-QA-02 - TO VERIFY WHETHER IT HAS SAME DATE AS OF CURRENT BILL RUN OR NOT??. La view restituisce tutte le righe anomale che non rispettano la regola definita dal KPI, consentendo di identificare e correggere i dati di fatturazione non conformi.");
		setOrder(11);
		setField(FieldEnum.invoiceDate);
		setCategory(CheckCategoryEnum.valueCheck);
	}

	@Override
	public InvoiceCheckResultDTO check(InvoiceStDTO row) {

		String fieldValue = row.getInvoiceDate();

		try {
			log.debug("Check - START - name: {}, order: {}, field: {}", getName(), getOrder(), getField().getValue());

			MapSqlParameterSource params = new MapSqlParameterSource()
					.addValue("loadingId", row.getLoadingId())
					.addValue("rowNumber", row.getRowNum());

			Map<String, Object> queryResult = jdbcTemplate.queryForMap(SQL, params);

			String result = (String) queryResult.get("result");

			setPassed("Passed".equals(result));

			log.debug("Check - fieldName: {} - fieldValue: {} - result: {}", getField().getValue(), fieldValue, result);
		}
		catch (Exception e) {
			log.error("Check - ERROR - name: {}, order: {}, field: {}, error: {}", getName(), getOrder(), getField().getValue(), e.getMessage());
			setPassed(false);
		}

		log.info("Check - END - name: {} - passed: {}", getName(), isPassed());

		return createCheckResult(getField(), fieldValue);
	}
}
