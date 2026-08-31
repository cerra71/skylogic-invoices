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
public class RaQa07ToCheckForEmptyCellsApartExceptionsCheck extends GenericCheck implements CheckI {

    // Costante = Codice SQL
    private static final String SQL = """
        SELECT
            tabella_invoice_st.site_connectivity_id,
            tabella_invoice_st.type,
            CASE
                WHEN NULLIF(TRIM(tabella_invoice_st.site_connectivity_id), '') IS NULL
                     AND tabella_invoice_st.type NOT IN (
                         'Cross Connect Interconnect Onetime Charge',
                         'Interconnect MRC',
                         'Network Slice Onetime Activation Fee',
                         'Take or Pay MRC',
                         'PoP-PoP Discount',
                         'Revenue Share Discount',
                         'Mini POP MRC'
                     )
                     AND tabella_invoice_st.type NOT LIKE 'Sale of Satellite Connectivity %'
                    THEN 'Fail'
                ELSE 'Passed'
            END AS result
        FROM invoice_st AS tabella_invoice_st
        WHERE tabella_invoice_st.loading_id = :loadingId
          AND tabella_invoice_st.row_num = :rowNumber;
    """;

    @PostConstruct
    private void init() {
        setName("Check RA-QA-07: KPI_7_TO_CHECK_FOR_ANY_EMPTY_CELLS_FOR_SITE_CONNECTIVITY_ID");
        setDescription("Individuare le righe in cui Site Connectivity Id è Null, Blank o fatto da soli spazi");
        setOrder(51);
        setField(FieldEnum.siteConnectivityId);
        setCategory(CheckCategoryEnum.valueCheck);
    }

    @Override
    public InvoiceCheckResultDTO check(InvoiceStDTO row) {

        // 1. Campo controllato dal check
        String fieldValue = row.getSiteConnectivityId();

        try {

            log.debug("Check - START - name: {}, order: {}, field: {}", getName(), getOrder(), getField().getValue());

            // 2. Parametri utilizzati dalla query SQL per identificare la riga.
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("loadingId", row.getLoadingId())
                    .addValue("rowNumber", row.getRowNum());

            // 3. Esegue il codice SQL della query: queryForMap viene usato perché la SELECT restituisce più colonne.
            Map<String, Object> queryResult = jdbcTemplate.queryForMap(SQL, params);

            // 4. Recupera la colonna "result" restituita dalla query SQL.
            String result = (String) queryResult.get("result");

            // 5. Converte il risultato SQL nel booleano utilizzato dall'applicazione.
            setPassed("Passed".equals(result));

            log.debug("Check - fieldName: {} - fieldValue: {} - result: {}", getField().getValue(), fieldValue, result);

        } catch (Exception e) {

            log.error("Check - ERROR - name: {}, order: {}, field: {}, error: {}", getName(), getOrder(), getField().getValue(), e.getMessage());

            setPassed(false);
        }

        log.info("Check - END - name: {} - passed: {}", getName(), isPassed());

        // Crea il FieldDTO con il risultato del check
        return createCheckResult(getField(), fieldValue);
    }
}
