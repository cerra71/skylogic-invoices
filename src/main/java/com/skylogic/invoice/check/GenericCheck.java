package com.skylogic.invoice.check;

import com.skylogic.invoice.dto.FieldEnum;
import com.skylogic.invoice.dto.InvoiceCheckResultDTO;
import com.skylogic.invoice.dto.InvoiceStDTO;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public abstract class GenericCheck {
	
	protected String name ="Undefined";
	protected String description = "Undefined";	
	protected Integer order = 0;
	protected FieldEnum field;
	
	protected boolean passed = false;
	
	public abstract InvoiceCheckResultDTO check(InvoiceStDTO row);
	
	protected InvoiceCheckResultDTO createCheckResult(FieldEnum field, 
			                                          String fieldValue) {
		InvoiceCheckResultDTO result = new InvoiceCheckResultDTO();
		result.setFieldName(field.getValue());
		result.setFieldValue(fieldValue);
		result.setPassed(isPassed());
		result.setCheckFailed(getName());
		return result;
	}

}
