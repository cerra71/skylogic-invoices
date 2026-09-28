package com.skylogic.invoice.check;

import com.skylogic.invoice.dto.CheckCategoryEnum;
import com.skylogic.invoice.dto.FieldEnum;
import com.skylogic.invoice.dto.InvoiceCheckResultDTO;
import com.skylogic.invoice.dto.InvoiceStDTO;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Slf4j
@Getter
@Setter
public abstract class GenericCheck {
	
	protected String name ="Undefined";
	protected String description = "Undefined";		
	protected FieldEnum field;
	protected CheckCategoryEnum category;
	protected String controlId;
	
	protected Integer order = 0;
	
	protected boolean passed = false;
	
	public abstract InvoiceCheckResultDTO check(InvoiceStDTO row);

	/**
	 * Restituisce la SQL parametrica usata da questo controllo per la singola riga.
	 * La query deve:
	 *   - usare i parametri nominali :loadingId e :rowNumber
	 *   - proiettare una colonna "result" con valori "Passed" / "Fail"
	 *   - riferirsi alla riga singola (WHERE loading_id = :loadingId AND row_num = :rowNumber)
	 * (Implementata in modo nativo da ciascun check; serve per il batch-checking.)
	 */
	public abstract String getSql();
	
	protected InvoiceCheckResultDTO createCheckResult(FieldEnum field, String fieldValue) {

		InvoiceCheckResultDTO result = new InvoiceCheckResultDTO();
		result.setFieldName(field.getValue());
		result.setFieldValue(fieldValue);
		result.setPassed(isPassed());
		result.setCheckFailed(getName());
		result.setControlId(getControlId());
		return result;
	}

	// Per far eseguire SQL a tutti i Check con parametri nominali
	@Autowired
	protected NamedParameterJdbcTemplate jdbcTemplate;

}
