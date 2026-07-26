package com.skylogic.invoice.service;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.skylogic.invoice.dto.InvoiceStDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Validated
public class GuiService {
	
	// Da implementare: carica la riga dal DB in base a loadingId e rowNumber
	public InvoiceStDTO loadRow(@NotBlank String loadingId, 
                                @NotNull Integer rowNumber) {
		InvoiceStDTO invoiceStDTO = new InvoiceStDTO();
		invoiceStDTO.setField1("");
		return invoiceStDTO;
	}
	
	// Da implementare: salva la riga nel DB in invoice e la toglie da staging
	public void moveRowToInvoice(@NotBlank String loadingId, 
						         @NotNull Integer rowNumber,
						         @NotNull InvoiceStDTO invoiceStDTO) {
		log.info("moveRowToInvoice - START - loadingId: {}, rowNumber: {}, invoiceStDTO: {}", loadingId, rowNumber, invoiceStDTO);
		// Da implementare: salva la riga nel DB in invoice e la toglie da staging
		log.info("moveRowToInvoice - END");
	}

}
