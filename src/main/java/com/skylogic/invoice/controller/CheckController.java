package com.skylogic.invoice.controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.skylogic.invoice.check.CheckI;
import com.skylogic.invoice.dto.FileCheckSummaryDTO;
import com.skylogic.invoice.dto.InvoiceCheckResultDTO;
import com.skylogic.invoice.dto.InvoiceStDTO;
import com.skylogic.invoice.service.CheckService;
import com.skylogic.invoice.service.GuiService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class CheckController extends GenericController {
	
	@Autowired
	private CheckService checkService;
	
	@Autowired
	private GuiService guiService;

	@Autowired
	private List<CheckI> checks;

	/**
     * Effettua il check di una riga
     *
     * @return il nome della view {@code details}
     */
    @PostMapping("/checkRow")
	public String checkRow(@RequestParam(name = "loadingId", required = true) String loadingId,
			               @RequestParam(name = "rowNumber", required = true) Integer rowNumber,
		                   Model model) {
    	
    	// 1. Caricamento di un record InvoiceStDTO dal DB
        InvoiceStDTO row = guiService.loadInvoiceStRow(loadingId, rowNumber);

		// 2. Applica la funzione di controllo e restituisce una List di InvoiceCheckResultDTO
		List<InvoiceCheckResultDTO> checkResult = checkService.checkRow(loadingId, rowNumber, row, checks);

		// 3. Trasformazione di tutti i campi del DTO in una lista da mostrare nella pagina details.html
		List<InvoiceCheckResultDTO> result = toResults(row);

    	// 4. Sostituisce, per fieldName (case-insensitive) i campi controllati con gli elementi
		// che contengono l'esito del check
    	for (int i = 0; i < result.size(); i++) {
    		InvoiceCheckResultDTO field = result.get(i);
    		for (InvoiceCheckResultDTO checked : checkResult) {
    			if (checked.getFieldName().equalsIgnoreCase(field.getFieldName())) {
    				result.set(i, checked);
    				break;
    			}
    		}
    	}

		// 5.  Ordina i campi secondo l'ordine definito nell'Enum FieldEnum
    	result.sort(Comparator.comparingInt(this::fieldEnumOrder));

    	for(InvoiceCheckResultDTO field : result) {
			log.info("checkRow - fieldName: {}, fieldValue: {}, checkFailed: {}", 
					field.getFieldName(), field.getFieldValue(), field.getCheckFailed());
		}

		// 6. Aggiorna i valori della pagina con quelli del record
    	model.addAttribute("loadingId", loadingId);
        model.addAttribute("rowNumber", rowNumber); 
        model.addAttribute("fields", result);

		// 7.  Aggiorna stato Pulsanti in base a quale tabella si trova il record
		model.addAttribute("checkEnabled",
				guiService.loadInvoiceStRow(loadingId, rowNumber) != null
		);

		model.addAttribute("pushBackEnabled",
				guiService.loadInvoiceRow(loadingId, rowNumber) != null
		);

		// 8. Div di dettaglio: mostra solo la riga controllata
		model.addAttribute("showRowDetails", true);
		model.addAttribute("showFileDetails", false);

		return "details"; // templates/home.html
	}

    /**
     * Effettua il check dell'intero file a partire dalla pagina HOME (lista
     * loading). Esegue i controlli su TUTTE le righe ancora presenti in
     * invoice_st per il loadingId selezionato e al termine ritorna alla home
     * per aggiornare i totali della tabella.
     *
     * @return redirect verso {@code /home}
     */
    @PostMapping("/checkFileLoading")
    public String checkFileLoading(@RequestParam(name = "loadingId", required = true) String loadingId,
                                   RedirectAttributes redirectAttributes) {

        log.info("checkFileLoading - START - loadingId: {}", loadingId);

        FileCheckSummaryDTO summary = checkService.checkFile(loadingId, checks);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                String.format(
                        "Check file %s completed: checked %d rows (%d passed, %d failed). " +
                                "Already moved to invoice: %d. Duration: %.2f s",
                        loadingId,
                        summary.getRowsChecked(),
                        summary.getRowsPassed(),
                        summary.getRowsFailed(),
                        summary.getRowsAlreadyMoved(),
                        summary.getDurationMs() / 1000.0
                )
        );

        log.info(
                "checkFileLoading - END - loadingId: {}, checked: {}, passed: {}, failed: {}, alreadyMoved: {}",
                loadingId, summary.getRowsChecked(), summary.getRowsPassed(),
                summary.getRowsFailed(), summary.getRowsAlreadyMoved()
        );

        return "redirect:/home";
    }

	/**
     * Effettua il check dell'intero file dalla pagina LOADING RAW (/checksall).
     * Non richiede rowNumber.
     *
     * @return il nome della view {@code checksall} con il summary e la tabella
     *         dei controlli falliti per ogni riga.
     */
    @PostMapping("/checkFile")
	public String checkFile(@RequestParam(name = "loadingId", required = true) String loadingId,
		                    Model model) {

        log.info("checkFile - START from loading raw page: loadingId: {}", loadingId);

        FileCheckSummaryDTO summary = checkService.checkFile(loadingId, checks);

        model.addAttribute("loadingId", loadingId);
        model.addAttribute("fileSummary", summary);

        // Lista ordinata per ROW NUMBER dei check falliti per ogni riga
        List<Map.Entry<Integer, List<String>>> failedRows = new ArrayList<>(summary.getRowFailedChecks().entrySet());
        failedRows.sort(Map.Entry.comparingByKey());
        model.addAttribute("failedRows", failedRows);

        // Ricostruisce la tabella rows (prima pagina, senza filtri) per mostrare
        // in contemporanea i campi del file con le nuove info di check
        Page<InvoiceStDTO> rowsPage = guiService.searchInvoiceStRows(
                loadingId, "", "", "", "EQ", false, "", "EQ", false, 0
        );
        model.addAttribute("rows", rowsPage.getContent());
        model.addAttribute("currentPage", rowsPage.getNumber());
        model.addAttribute("totalPages", rowsPage.getTotalPages());
        model.addAttribute("totalRows", rowsPage.getTotalElements());
        model.addAttribute("hasPrevious", rowsPage.hasPrevious());
        model.addAttribute("hasNext", rowsPage.hasNext());

        model.addAttribute("billingAccountNumber", "");
        model.addAttribute("siteConnectivityId", "");
        model.addAttribute("entitlementGb", "");
        model.addAttribute("usageGb", "");
        model.addAttribute("entitlementOperator", "EQ");
        model.addAttribute("entitlementInclusive", false);
        model.addAttribute("usageOperator", "EQ");
        model.addAttribute("usageInclusive", false);

        log.info(
                "checkFile - END: loadingId: {}, rows: {}, passed: {}, failed: {}, failures: {}",
                loadingId, summary.getRowsChecked(), summary.getRowsPassed(),
                summary.getRowsFailed(), failedRows.size()
        );

		return "checksall";
	}

	/**
     * Esporta il confronto in un file Excel
     *
     * @return il nome della view {@code details}
     */
    @PostMapping("/exportComparisonExcel")
	public String exportComparisonExcel(@RequestParam(name = "loadingId", required = true) String loadingId,
			                             @RequestParam(name = "rowNumber", required = false, defaultValue = "1") Integer rowNumber,
		                                 Model model) {

		model.addAttribute("loadingId", loadingId);
		model.addAttribute("rowNumber", rowNumber);

		// Div di dettaglio: mostra il riepilogo del file
		model.addAttribute("showRowDetails", false);
		model.addAttribute("showFileDetails", true);

		return "details"; // templates/home.html
	}

	/**
     * Esporta le osservazioni in un file Excel
     *
     * @return il nome della view {@code details}
     */
    @PostMapping("/exportObservationExcel")
	public String exportObservationExcel(@RequestParam(name = "loadingId", required = true) String loadingId,
			                              @RequestParam(name = "rowNumber", required = false, defaultValue = "1") Integer rowNumber,
		                                  Model model) {

		model.addAttribute("loadingId", loadingId);
		model.addAttribute("rowNumber", rowNumber);

		// Div di dettaglio: mostra il riepilogo del file
		model.addAttribute("showRowDetails", false);
		model.addAttribute("showFileDetails", true);

		return "details"; // templates/home.html
	}

}
