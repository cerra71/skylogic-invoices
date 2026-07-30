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
	 *
	 * 1. Recupera da invoice_st il riepilogo dei caricamenti.
	 * 2. Crea una lista vuota di LoadingSummaryDTO.
	 * 3. Prepara il formato della data.
	 * 4. Scorre ciascun caricamento.
	 * 5. Estrae loadingId, data e conteggio dall'Object[].
	 * 6. Conta le righe scartate con lo stesso loadingId.
	 * 7. Costruisce un LoadingSummaryDTO.
	 * 8. Aggiunge il DTO alla lista.
	 * 9. Restituisce la lista completa.
	 */
	public List<LoadingSummaryDTO> getLoadings() {

		log.info("getLoadings - START");

		/*
		 * Esegue la query definita in InvoiceStRepository.
		 *
		 * Il repository restituisce una lista di array Object[] chiamata rows.
		 * Ogni array rappresenta un caricamento aggregato e contiene:
		 *
		 * row[0] = loadingId
		 * row[1] = loadingTime
		 * row[2] = numero di righe caricate
		 */
		List<Object[]> rows = invoiceStRepository.findLoadingSummaries();

		log.info("getLoadings - Found {} loading summaries", rows.size());

		/*
		 * Crea la lista per ora vuota che conterrà i DTO finali chiamata results.
		 *
		 * Questa lista verrà popolata durante il ciclo
		 * e infine restituita al controller.
		 */
		List<LoadingSummaryDTO> result = new ArrayList<>();

		/*
		 * Definisce il formato testuale da usare per la data e l'ora.
		 * Esempio:
		 * 30/07/2026 14:35:20
		 */
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

		/*
		 * Scorre tutti i risultati restituiti dal repository.
		 *
		 * Ogni elemento "row" è un Object[] contenente
		 * i dati aggregati di un singolo loadingId.
		 */
		for (Object[] row : rows) {

			/*
			 * Recupera il loadingId, la data e il numero di righe dall'array.
			 * Il cast è necessario perché row[n] viene visto genericamente come Object.
			 */
			String loadingId = (String) row[0];
			LocalDateTime loadingTime = (LocalDateTime) row[1];
			Long loadedRows = (Long) row[2];

			/*
			 * Cerca nella tabella invoice_discard quante righe appartengono allo stesso loadingId.
			 */
			long discardedRows = invoiceDiscardRepository.countByLoadingId(loadingId);

			/*
			 * Crea un nuovo DTO destinato alla GUI.
			 * Ogni ciclo costruisce il riepilogo relativo a un singolo caricamento.
			 * Vengono impostati tutti i valori del DTO.
			 */
			LoadingSummaryDTO dto = new LoadingSummaryDTO();
			dto.setLoadingId(loadingId);
			dto.setReportingYear(loadingTime.getYear());
			dto.setReportingMonth(loadingTime.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
			dto.setLoadingTime(loadingTime.format(formatter));
			dto.setLoadedRows(loadedRows);
			dto.setDiscardedRows(discardedRows);

			/*
			 * Aggiunge il DTO appena costruito alla lista dei risultati.
			 */
			result.add(dto);
		}

		log.info("getLoadings - END - Returning {} loading summaries", result.size());

		/*
		 * Restituisce la lista completa dei riepiloghi.
		 * Se il repository non trova caricamenti,
		 * viene restituita una lista vuota, non null.
		 */
		return result;
	}

	// Da implementare: carica la riga dal DB in base a loadingId e rowNumber
	public InvoiceStDTO loadRow(@NotBlank String loadingId, @NotNull Integer rowNumber) {

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
