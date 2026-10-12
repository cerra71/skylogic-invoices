package com.skylogic.invoice.service;

import java.util.List;

import com.skylogic.invoice.dto.*;
import com.skylogic.invoice.entity.Invoice;
import com.skylogic.invoice.entity.InvoiceRowId;
import com.skylogic.invoice.entity.InvoiceSt;
import com.skylogic.invoice.mapper.*;
import com.skylogic.invoice.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

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

	// Charges e Usage
	@Autowired
	private ChargesRepository chargesRepository;

	@Autowired
	private UsageRepository usageRepository;

	@Autowired
	private ChargesMapper chargesMapper;

	@Autowired
	private UsageMapper usageMapper;


	/**
	 * Restituisce la lista dei caricamenti, aggregando invoice_st (per loading_id,
	 * loading_time e conteggio righe) e invoice_discard (per conteggio righe scartate).
	 * Ordinati per loading_time decrescente.
	 *
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
	 * Recupera una pagina di 1000 righe dalla invoice_st selezionata,
	 * convertendo le entity in DTO.
	 *
	 * @param loadingId identificativo del CSV caricato
	 * @param page      numero della pagina, a partire da zero
	 * @return pagina di DTO con informazioni sul totale dei risultati
	 */
	public Page<InvoiceStDTO> loadInvoiceStRows(
			@NotBlank String loadingId,
			int page) {

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

		// L'operatore serve soltanto quando è presente il relativo valore.
		if (entitlementValue != null) {
			validateGbOperator(entitlementOperator);
		}

		if (usageValue != null) {
			validateGbOperator(usageOperator);
		}

		PageRequest pageable = PageRequest.of(Math.max(page, 0), 1000);

		boolean allFiltersEmpty = billingNorm.isEmpty() && siteNorm.isEmpty() && entitlementValue == null && usageValue == null;

		// Con tutti i filtri vuoti usa la query semplice.
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


	// --- METODI DI SUPPORTO PER I FILTRI NUMERICI ---

	/**
	 * Converte il filtro in un Long usato dal repository.
	 * Un campo vuoto disattiva il filtro.
	 * Accetta il punto o la virgola come separatore decimale.
	 */
	private Long parseGbFilter(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			return null;
		}

		String normalized = value.trim().replace(',', '.');

		if (!normalized.matches("[+-]?[0-9]+(\\.[0-9]+)?")) {
			throw new IllegalArgumentException(
					fieldName + ": inserire un numero intero o decimale."
			);
		}

		try {
			// La conversione in Long tronca la parte decimale.
			return Double.valueOf(normalized).longValue();
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException(
					fieldName + ": valore fuori dall'intervallo supportato.",
					ex
			);
		}
	}

	/**
	 * Accetta soltanto gli operatori previsti dal form.
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


	// Elimina tutte le righe di un loading da invoice_st e invoice_discard.
	@Transactional
	public void deleteLoading(@NotBlank String loadingId) {
		log.info("deleteLoading - START: loadingId: {}", loadingId);

		invoiceStRepository.deleteByLoadingId(loadingId);
		invoiceDiscardRepository.deleteByLoadingId(loadingId);

		log.info("deleteLoading - END: loadingId: {}", loadingId);
	}

	// Sposta un record da InvoiceSt a Invoice.
	@Transactional
	public void moveRowToInvoice(
			@NotBlank String loadingId,
			@NotNull Integer rowNumber,
			@NotNull InvoiceStDTO invoiceStDTO) {

		log.info("moveRowToInvoice - START - loadingId: {}, rowNumber: {}, invoiceStDTO: {}",
				loadingId, rowNumber, invoiceStDTO);


		// Converte InvoiceStDTO in InvoiceDTO.
		InvoiceDTO invoiceDTO = invoiceStToInvoiceMapper.toDTO(invoiceStDTO);

		// Converte InvoiceDTO nell'entity Invoice.
		Invoice invoice = invoiceMapper.toEntity(invoiceDTO);

		// Salva il record nella tabella Invoice.
		invoiceRepository.save(invoice);

		// Costruisce la chiave composta del record originale.
		InvoiceRowId id = new InvoiceRowId(loadingId, rowNumber.longValue());

		// Elimina il record da InvoiceSt.
		invoiceStRepository.deleteById(id);

		log.info("moveRowToInvoice - END");
	}


	// Sposta un record da Invoice a InvoiceSt.
	@Transactional
	public void moveRowToStaging(
			@NotBlank String loadingId,
			@NotNull Integer rowNumber,
			@NotNull InvoiceDTO invoiceDTO) {

		log.info("moveRowToStaging - START - loadingId: {}, rowNumber: {}, invoiceDTO: {}",
				loadingId, rowNumber, invoiceDTO);


		// Converte InvoiceDTO in InvoiceStDTO.
		InvoiceStDTO invoiceStDTO = invoiceStToInvoiceMapper.toEntity(invoiceDTO);

		// Converte InvoiceStDTO nell'entity InvoiceSt.
		InvoiceSt invoiceSt = invoiceStMapper.toEntity(invoiceStDTO);

		// Salva il record nella tabella InvoiceSt.
		invoiceStRepository.save(invoiceSt);

		// Costruisce la chiave composta del record originale.
		InvoiceRowId id = new InvoiceRowId(loadingId, rowNumber.longValue());

		// Elimina il record da Invoice.
		invoiceRepository.deleteById(id);

		log.info("moveRowToStaging - END");
	}


	// --- SERVICE METODI TABELLA CHARGES ---

	// Recupera una pagina di 1000 righe dalla tabella Charges.
	public Page<ChargesDTO> loadChargesRows(@NotBlank String loadingId, int page) {

		PageRequest pageable = PageRequest.of(Math.max(page, 0), 1000);

		return chargesRepository
				.findByLoadingIdOrderByRowNumAsc(loadingId, pageable)
				.map(chargesMapper::toDTO);
	}

	// Cerca le righe della tabella Charges applicando filtri testuali e numerici.
	public Page<ChargesDTO> searchChargesRows(
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

		if (entitlementValue != null) {
			validateGbOperator(entitlementOperator);
		}

		if (usageValue != null) {
			validateGbOperator(usageOperator);
		}

		PageRequest pageable = PageRequest.of(Math.max(page, 0), 1000);

		boolean allFiltersEmpty = billingNorm.isEmpty() && siteNorm.isEmpty() && entitlementValue == null && usageValue == null;

		if (allFiltersEmpty) {
			return chargesRepository
					.findByLoadingIdOrderByRowNumAsc(loadingId, pageable)
					.map(chargesMapper::toDTO);
		}

		return chargesRepository.searchChargesRowsNumeric(
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
		).map(chargesMapper::toDTO);
	}


	// --- SERVICE METODI TABELLA USAGE ---

	// Recupera una pagina di 1000 righe dalla tabella Usage.
	public Page<UsageDTO> loadUsageRows(@NotBlank String loadingId, int page) {

		PageRequest pageable = PageRequest.of(Math.max(page, 0), 1000);

		return usageRepository
				.findByLoadingIdOrderByRowNumAsc(loadingId, pageable)
				.map(usageMapper::toDTO);
	}

	// Cerca le righe della tabella Usage applicando filtri testuali e il filtro numerico Usage GB.
	public Page<UsageDTO> searchUsageRows(
			@NotBlank String loadingId,
			String billingAccountNumber,
			String siteConnectivityId,
			String usageGb,
			String usageOperator,
			boolean usageInclusive,
			int page) {

		String billingNorm = billingAccountNumber == null ? "" : billingAccountNumber.trim();
		String siteNorm = siteConnectivityId == null ? "" : siteConnectivityId.trim();
		Long usageValue = parseGbFilter(usageGb, "Usage GB");

		if (usageValue != null) {
			validateGbOperator(usageOperator);
		}

		PageRequest pageable = PageRequest.of(Math.max(page, 0), 1000);

		boolean allFiltersEmpty = billingNorm.isEmpty() && siteNorm.isEmpty() && usageValue == null;

		if (allFiltersEmpty) {
			return usageRepository
					.findByLoadingIdOrderByRowNumAsc(loadingId, pageable)
					.map(usageMapper::toDTO);
		}

		return usageRepository.searchUsageRowsNumeric(
				loadingId,
				billingNorm,
				siteNorm,
				usageValue,
				usageOperator,
				usageInclusive,
				pageable
		).map(usageMapper::toDTO);
	}
}