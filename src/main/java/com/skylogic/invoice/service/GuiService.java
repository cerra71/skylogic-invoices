package com.skylogic.invoice.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.skylogic.invoice.dto.InvoiceCheckResultDTO;
import com.skylogic.invoice.dto.InvoiceDTO;
import com.skylogic.invoice.dto.InvoiceStDTO;
import com.skylogic.invoice.dto.LoadingSummaryDTO;
import com.skylogic.invoice.dto.LoadingSummaryInterface;
import com.skylogic.invoice.mapper.LoadingSummaryMapper;
import com.skylogic.invoice.repository.InvoiceDiscardRepository;
import com.skylogic.invoice.repository.InvoiceRepository;
import com.skylogic.invoice.repository.InvoiceStRepository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Validated
public class GuiService {

	// Repository autowired

	@Autowired
	private InvoiceStRepository invoiceStRepository;

	@Autowired
	private InvoiceRepository invoiceRepository;

	@Autowired
	private InvoiceDiscardRepository invoiceDiscardRepository;
	
	@Autowired
	private LoadingSummaryMapper loadingSummaryMapper;

	// //
	
	/**
	 * Restituisce la lista dei caricamenti, aggregando invoice_st (per loading_id,
	 * loading_time e conteggio righe) e invoice_discard (per conteggio righe scartate).
	 * Ordinati per loading_time descrescente.	 
	 * @return lista di {@link LoadingSummaryDTO} con i riepiloghi dei caricamenti.
	 */
	public List<LoadingSummaryDTO> getLoadings() {
		List<LoadingSummaryInterface> rows = invoiceStRepository.findLoadingSummaries();
		log.info("getLoadings - Found {} loading summaries", rows.size());

		List<LoadingSummaryDTO> result = loadingSummaryMapper.toDTOs(rows);
		
		for (LoadingSummaryDTO row : result)
			row.setDiscardedRows(invoiceDiscardRepository.countByLoadingId(row.getLoadingId()));

		log.info("getLoadings - END - Returning {} loading summaries", result.size());
		return result;
	}

	// Carica la riga dal DB in base a loadingId e rowNumber (sia da tabella invoice_st che da tabella invoice, overload)
	public InvoiceStDTO loadInvoiceStRow(@NotBlank String loadingId, @NotNull Integer rowNumber) {
		InvoiceStDTO dto = new InvoiceStDTO();
		dto.setInvoiceNumber("st 123");
		dto.setSiteName("st name");
		return dto;
	}
	
	public InvoiceDTO loadInvoiceRow(@NotBlank String loadingId, @NotNull Integer rowNumber) {
		InvoiceDTO dto = new InvoiceDTO();
		dto.setInvoiceNumber("123");
		dto.setSiteName("name");
		return dto;
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



