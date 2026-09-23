package com.skylogic.invoice.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.skylogic.invoice.check.CheckI;
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
     * Effettua il check dell'intero file
     *
     * @return il nome della view {@code details}
     */
    @PostMapping("/checkFile")
	public String checkFile(@RequestParam(name = "loadingId", required = true) String loadingId,
			                 @RequestParam(name = "rowNumber", required = true) Integer rowNumber,
		                     Model model) {

		model.addAttribute("loadingId", loadingId);
		model.addAttribute("rowNumber", rowNumber);

		// Div di dettaglio: mostra solo il riepilogo del file
		model.addAttribute("showRowDetails", false);
		model.addAttribute("showFileDetails", true);

		return "details"; // templates/home.html
	}

	/**
     * Esporta il confronto in un file Excel
     *
     * @return il nome della view {@code details}
     */
    @PostMapping("/exportComparisonExcel")
	public String exportComparisonExcel(@RequestParam(name = "loadingId", required = true) String loadingId,
			                             @RequestParam(name = "rowNumber", required = true) Integer rowNumber,
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
			                              @RequestParam(name = "rowNumber", required = true) Integer rowNumber,
		                                  Model model) {

		model.addAttribute("loadingId", loadingId);
		model.addAttribute("rowNumber", rowNumber);

		// Div di dettaglio: mostra il riepilogo del file
		model.addAttribute("showRowDetails", false);
		model.addAttribute("showFileDetails", true);

		return "details"; // templates/home.html
	}

}
