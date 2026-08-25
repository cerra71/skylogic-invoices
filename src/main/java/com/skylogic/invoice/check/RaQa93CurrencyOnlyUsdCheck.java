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
public class RaQa93CurrencyOnlyUsdCheck extends GenericCheck implements CheckI {

	private static final String SQL = """
    SELECT
        tabella_invoice_st.currency,
        CASE
            WHEN tabella_invoice_st.type IS NULL
                 OR tabella_invoice_st.type <> 'Site Connectivity MRC'
                THEN 'Passed'
            WHEN tabella_invoice_st.currency IS NULL
                 OR TRIM(tabella_invoice_st.currency) = ''
                THEN 'Fail'
            WHEN UPPER(TRIM(tabella_invoice_st.currency)) = 'USD'
                THEN 'Passed'
            ELSE 'Fail'
        END AS result
    FROM invoice_st AS tabella_invoice_st
    WHERE tabella_invoice_st.loading_id = :loadingId
      AND tabella_invoice_st.row_num = :rowNumber
    """;

	@PostConstruct
	private void init() {
		setName("Check RA-QA-93: KPI_35_CURRENCY_COLUMN_CONTAINS_BLANK_SITECONNECTIVITY_MRC");
		setDescription("Controllo di qualita del dato (KPI) relativo al campo CURRENCY. Verifica: RA-QA-93 - 1) TO VALIDATE WHETHER COLUMN CONTAINS USD ALONE OR NOT??. La view restituisce tutte le righe anomale che non rispettano la regola definita dal KPI, consentendo di identificare e correggere i dati di fatturazione non conformi.");
		setOrder(21);
		setField(FieldEnum.currency);
		setCategory(CheckCategoryEnum.valueCheck);
	}

	@Override
	public InvoiceCheckResultDTO check(InvoiceStDTO row) {

		String fieldValue = row.getCurrency();

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
