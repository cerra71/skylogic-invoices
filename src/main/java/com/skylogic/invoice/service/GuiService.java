package com.skylogic.invoice.service;

import com.skylogic.invoice.dto.LoadingSummaryDTO;
import com.skylogic.invoice.repository.InvoiceDiscardRepository;
import com.skylogic.invoice.repository.InvoiceRepository;
import com.skylogic.invoice.repository.InvoiceStRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.skylogic.invoice.dto.InvoiceStDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

	// //
	
	/**
	 * Restituisce la lista dei caricamenti, aggregando invoice_st (per loading_id,
	 * loading_time e conteggio righe) e invoice_discard (per conteggio righe scartate).
	 * Ordinati per loading_time descrescente.
	 */
	public List<LoadingSummaryDTO> getLoadings() {
		List<Object[]> rows = invoiceStRepository.findLoadingSummaries();
		List<LoadingSummaryDTO> result = new ArrayList<>();

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

		for (Object[] row : rows) {
			String loadingId = (String) row[0];
			// Con query nativa PostgreSQL restituisce Timestamp; convertiamo a LocalDateTime
			LocalDateTime loadingTime = ((Timestamp) row[1]).toLocalDateTime();
			// COUNT(*) può essere restituito come Long o BigInteger a seconda del driver
			Long loadedRows = ((Number) row[2]).longValue();
			long discardedRows = invoiceDiscardRepository.countByLoadingId(loadingId);

			LoadingSummaryDTO dto = new LoadingSummaryDTO();
			dto.setLoadingId(loadingId);
			dto.setReportingYear(loadingTime.getYear());
			dto.setReportingMonth(loadingTime.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
			dto.setLoadingTime(loadingTime.format(formatter));
			dto.setLoadedRows(loadedRows);
			dto.setDiscardedRows(discardedRows);

			result.add(dto);
		}

		return result;
	}

	// Da implementare: carica la riga dal DB in base a loadingId e rowNumber
	public InvoiceStDTO loadRow(@NotBlank String loadingId,
                                @NotNull Integer rowNumber) {
		// TODO: implementare il recupero della riga da invoice_st
		return new InvoiceStDTO();
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
