package com.skylogic.invoice.service;


import java.util.List;

import com.skylogic.invoice.entity.Invoice;
import com.skylogic.invoice.entity.InvoiceRowId;
import com.skylogic.invoice.entity.InvoiceSt;
import com.skylogic.invoice.mapper.InvoiceMapper;
import com.skylogic.invoice.mapper.InvoiceStMapper;
import com.skylogic.invoice.mapper.InvoiceStToInvoiceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import com.skylogic.invoice.repository.KpiDocumentationRepository;

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
	private KpiDocumentationRepository kpiDocumentationRepository;
	
	@Autowired
	private LoadingSummaryMapper loadingSummaryMapper;

	@Autowired
	private InvoiceMapper invoiceMapper;

	@Autowired
	private InvoiceStMapper invoiceStMapper;

	@Autowired
	private InvoiceStToInvoiceMapper invoiceStToInvoiceMapper;

	
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
	 * Carica tutte le righe di invoice_st per un loading.
	 */
	public List<InvoiceStDTO> loadInvoiceStRows(@NotBlank String loadingId) {
		return invoiceStRepository.findByLoadingIdOrderByRowNumAsc(loadingId)
				.stream()
				.map(invoiceStMapper::toDTO)
				.toList();
	}

	/**
	 * Recupera una pagina di 1000 righe dalla invoice_st selezionata
	 * convertendo le entity in DTO.
	 *
	 * @param loadingId identificativo del CSV caricato
	 * @param page numero della pagina, a partire da zero
	 * @return pagina di DTO con informazioni sul totale dei risultati
	 */
	public Page<InvoiceStDTO> loadInvoiceStRows(@NotBlank String loadingId,	int page) {

		PageRequest pageable = PageRequest.of(Math.max(page, 0), 1000);

		return invoiceStRepository
				.findByLoadingIdOrderByRowNumAsc(loadingId, pageable)
				.map(invoiceStMapper::toDTO);
	}

	/**
	 * Cerca le righe della invoice_st applicando filtri testuali e numerici.
	 * Restituisce pagine da 1000 righe.
	 */
	public Page<InvoiceStDTO> searchInvoiceStRows(
			@NotBlank String loadingId,
			String billingAccountNumber,
			String siteConnectivityId,
			String entitlementGb,
			String entitlementOperator,
			boolean entitlementInclusive,
			String usageGb,
			String usageOperator,
			boolean usageInclusive,
			int page) {

		String billingNorm = billingAccountNumber == null ? "" : billingAccountNumber.trim();
		String siteNorm = siteConnectivityId == null ? "" : siteConnectivityId.trim();
		Long entitlementValue = parseGbFilter(entitlementGb, "Entitlement GB");
		Long usageValue = parseGbFilter(usageGb, "Usage GB");

		// L'operatore serve soltanto quando è presente un valore.
		if (entitlementValue != null) {
			validateGbOperator(entitlementOperator);
		}

		if (usageValue != null) {
			validateGbOperator(usageOperator);
		}

		PageRequest pageable = PageRequest.of(Math.max(page, 0), 1000);

		// --- FAST PATH ---
		// Se TUTTI i filtri sono "vuoti"/default, non scomodiamo la query nativa
		// CON parametri (che qualche volta da binding NULL strani e torna 0 righe).
		// Usiamo invece la findByLoadingId semplice (stessa identica paginazione
		// 1000 righe) — cosi' la ricerca default torna SEMPRE risultati corretti.
		final boolean allFiltersEmpty =
				billingNorm.isEmpty()
						&& siteNorm.isEmpty()
						&& entitlementValue == null
						&& usageValue == null;
		if (allFiltersEmpty) {
			return invoiceStRepository
					.findByLoadingIdOrderByRowNumAsc(loadingId, pageable)
					.map(invoiceStMapper::toDTO);
		}

		return invoiceStRepository.searchInvoiceStRowsNumeric(
				loadingId,
				billingNorm,
				siteNorm,
				entitlementValue,
				entitlementOperator,
				entitlementInclusive,
				usageValue,
				usageOperator,
				usageInclusive,
				pageable
		).map(invoiceStMapper::toDTO);
	}

	/**
	 * Metodo di supporto: converte il filtro in un intero (Long) usato dal
	 * repository. Un campo vuoto disattiva il filtro.
	 * <p>
	 * Accetta anche numeri decimali (punto o virgola come separatore): il valore
	 * viene convertito in Long per compatibilita' con la firma del repository,
	 * mentre la query SQL (tramite {@code CAST(:param AS NUMERIC)}) tratta
	 * comunque il valore come numero a virgola mobile.
	 */
	private Long parseGbFilter(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			return null;
		}

		String normalized = value.trim();
		// Supporta sia il punto che la virgola come separatore decimale.
		normalized = normalized.replace(',', '.');

		// Regex: intero oppure numero decimale (es. "81", "81.937825", "-3.14").
		if (!normalized.matches("[+-]?[0-9]+(\\.[0-9]+)?")) {
			throw new IllegalArgumentException(
					fieldName + ": inserire un numero intero o decimale."
			);
		}

		try {
			// Il repository si aspetta un Long, ma poiche' nella query c'e'
			// CAST(:param AS NUMERIC) possiamo tranquillamente passare la
			// parte intera (troncata).
			return Double.valueOf(normalized).longValue();
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException(
					fieldName + ": valore fuori dall'intervallo supportato."
			);
		}
	}

	/**
	 * Metodo di supporto: accetta soltanto gli operatori previsti dal form.
	 */
	private void validateGbOperator(String operator) {
		if (!"EQ".equals(operator)
				&& !"GT".equals(operator)
				&& !"LT".equals(operator)) {

			throw new IllegalArgumentException(
					"Operatore di confronto non valido."
			);
		}
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

	// Sposta un record da Invoice a InvoiceSt
	@Transactional
	public void moveRowToStaging(@NotBlank String loadingId,
								 @NotNull Integer rowNumber,
								 @NotNull InvoiceDTO invoiceDTO) {

		log.info("moveRowToStaging - START - loadingId: {}, rowNumber: {}, invoiceDTO: {}", loadingId, rowNumber, invoiceDTO);

		// 1. Conversione da InvoiceDTO a InvoiceStDTO
		InvoiceStDTO invoiceStDTO = invoiceStToInvoiceMapper.toEntity(invoiceDTO);

		// 2. Conversione da InvoiceStDTO a entity InvoiceSt
		InvoiceSt invoiceSt = invoiceStMapper.toEntity(invoiceStDTO);

		// 3. Salvataggio del record nella tabella InvoiceSt
		invoiceStRepository.save(invoiceSt);

		// 4. Costruzione della chiave composta
		InvoiceRowId id = new InvoiceRowId(loadingId, rowNumber.longValue());

		// 5. Eliminazione del record da Invoice tramite la chiave
		invoiceRepository.deleteById(id);

		log.info("moveRowToStaging - END");
	}



}



