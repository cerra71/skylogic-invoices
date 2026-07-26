package com.skylogic.invoice.check;

import com.skylogic.invoice.dto.FieldEnum;
import com.skylogic.invoice.dto.InvoiceCheckResultDTO;
import com.skylogic.invoice.dto.InvoiceStDTO;

public interface CheckI {

	// Check the invoice row and return the result
	public InvoiceCheckResultDTO check(InvoiceStDTO row);

	public String getName();
	
	public String getDescription();
	
	public Integer getOrder();
	
	public FieldEnum getField();
	
	public boolean isPassed();
}
