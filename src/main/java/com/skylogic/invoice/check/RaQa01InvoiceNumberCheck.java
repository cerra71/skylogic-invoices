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

// Query SQL utilizzata per eseguire il controllo del KPI.

// 1. seleziona la riga corrente dalla tabella invoice_st;
// 2. identifica la riga tramite loading_id e row_num;
// 3. estrae tutte le sequenze numeriche presenti in invoice_number;
// 4. conta quante sequenze numeriche distinte sono presenti;
// 5. restituisce "Passed" se è presente al massimo un numero distinto, altrimenti restituisce "Fail".


@Component
@Slf4j
@Getter
@Setter
public class RaQa01InvoiceNumberCheck extends GenericCheck implements CheckI {

	// Costante = Codice SQL
	private static final String SQL = """
    SELECT
        tabella_invoice_st.invoice_number
        CASE
            WHEN (
                SELECT COUNT(DISTINCT (matches.numero)[1])
                FROM regexp_matches(
                    COALESCE(tabella_invoice_st.invoice_number, ''),
                    '([0-9]+)',
                    'g'
                ) AS matches(numero)
            ) <= 1
            THEN 'Passed'
            ELSE 'Fail'
        END AS result

    FROM invoice_st AS tabella_invoice_st
    WHERE tabella_invoice_st.loading_id = :loadingId
      AND tabella_invoice_st.row_num = :rowNumber
    """;

	// Inizializza i metadati del controllo dopo la creazione del bean Spring.
    @PostConstruct
    private void init() {
    	setName("Check RA-QA-01: KPI_48_INVOICE_NUMBER_CONTAINS_MORE_THAN_1_UNIQUE_NUMBER");
    	setDescription("Individuare le righe in cui INVOICE_NUMBER contiene più di un numero distinto, cioè più di una sequenza numerica secondo la documentazione del KPI.");
    	setOrder(1);
    	setField(FieldEnum.invoiceNumber);
    	setCategory(CheckCategoryEnum.valueCheck);
    }

    @Override
    public InvoiceCheckResultDTO check(InvoiceStDTO row) {

		// 1. Recupera il valore del campo invoiceNumber della riga da controllare.
    	String fieldValue = row.getInvoiceNumber();
    	
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
    	}
    	catch(Exception e) {

    		log.error("Check - ERROR - name: {}, order: {}, field: {}, error: {}", getName(), getOrder(), getField().getValue(), e.getMessage());
    		setPassed(false);     		
    	}
    	
    	log.info("Check - END - name: {} - passed: {}", getName(), isPassed());	
    	
    	// Crea il FieldDTO con il risultato del check
    	return createCheckResult(getField(), fieldValue);
    }
}