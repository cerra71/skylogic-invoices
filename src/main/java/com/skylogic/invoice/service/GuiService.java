package com.skylogic.invoice.service;


import java.util.List;

import com.skylogic.invoice.entity.Invoice;
import com.skylogic.invoice.entity.InvoiceRowId;
import com.skylogic.invoice.mapper.InvoiceMapper;
import com.skylogic.invoice.mapper.InvoiceStMapper;
import com.skylogic.invoice.mapper.InvoiceStToInvoiceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

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
import org.springframework.transaction.annotation.Transactional;

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

	@Autowired
	private InvoiceMapper invoiceMapper;

	@Autowired
	private InvoiceStMapper invoiceStMapper;

	@Autowired
	private InvoiceStToInvoiceMapper invoiceStToInvoiceMapper;

	// //
	
	/**
	 * Restituisce la lista dei caricamenti, aggregando invoice_st (per loading_id,
	 * loading_time e conteggio righe) e invoice_discard (per conteggio righe scartate).
	 * Ordinati per loading_time decrescente.
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
	// Usa come chiave il LoadingId del foglio caricato + il RowNumber della riga
	public InvoiceStDTO loadInvoiceStRow(@NotBlank String loadingId, @NotNull Integer rowNumber) {

		// Id da cercare nel db
		InvoiceRowId id = new InvoiceRowId(loadingId, rowNumber.longValue());

		// findById restituisce una entity Optional<Invoice>
		// Convertiamo Optional<Invoice> in InvoiceDTO oppure
		// Null se la riga non esiste (campi bianchi)
		return invoiceStRepository.findById(id)
				.map(entity -> invoiceStMapper.toDTO(entity))
				.orElse(null);
	}
	
	public InvoiceDTO loadInvoiceRow(@NotBlank String loadingId, @NotNull Integer rowNumber) {

		// Id da cercare nel db
		InvoiceRowId id = new InvoiceRowId(loadingId, rowNumber.longValue());

		log.info("Riga da cercare nel database: {}", id);

		// findById restituisce una entity Optional<Invoice>
		// Convertiamo Optional<Invoice> in InvoiceDTO oppure
		// Null se la riga non esiste (campi bianchi)
		return invoiceRepository.findById(id)
				.map(entity -> invoiceMapper.toDTO(entity))
				.orElse(null);
	}

	/**
	 * Elimina tutte le righe di un loading (da invoice_st e invoice_discard).
	 */
	@Transactional
	public void deleteLoading(@NotBlank String loadingId) {
		log.info("deleteLoading - START: loadingId: {}", loadingId);

		invoiceStRepository.deleteByLoadingId(loadingId);
		invoiceDiscardRepository.deleteByLoadingId(loadingId);

		log.info("deleteLoading - END: loadingId: {}", loadingId);
	}

	// Sposta un record da InvoiceSt a Invoice
	@Transactional
	public void moveRowToInvoice(@NotBlank String loadingId,
						         @NotNull Integer rowNumber,
						         @NotNull InvoiceStDTO invoiceStDTO) {

		log.info("moveRowToInvoice - START - loadingId: {}, rowNumber: {}, invoiceStDTO: {}", loadingId, rowNumber, invoiceStDTO);

		// 1. Conversione da InvoiceStDTO a InvoiceDTO
		InvoiceDTO invoiceDTO = invoiceStToInvoiceMapper.toDTO(invoiceStDTO);

		// 2. Conversione da InvoiceDTO a entity Invoice
		Invoice invoice = invoiceMapper.toEntity(invoiceDTO);

		// 3. Salvataggio del record nella tabella Invoice (loadingID e rowNumber già presenti)
		invoiceRepository.save(invoice);

		// 4. Costruzione della chiave composta
		InvoiceRowId id = new InvoiceRowId(loadingId, rowNumber.longValue());

		// 5. Elimina il record da invoiceSt (tramite id ricostruito)
		invoiceStRepository.deleteById(id);

		log.info("moveRowToInvoice - END");
	}

}



