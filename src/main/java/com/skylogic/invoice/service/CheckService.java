package com.skylogic.invoice.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.skylogic.invoice.repository.InvoiceDiscardRepository;
import com.skylogic.invoice.repository.InvoiceRepository;
import com.skylogic.invoice.repository.InvoiceStRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import com.skylogic.invoice.check.CheckI;
import com.skylogic.invoice.dto.InvoiceCheckResultDTO;
import com.skylogic.invoice.dto.InvoiceStDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Validated
public class CheckService {

    // Repository autowired

    @Autowired
    private InvoiceStRepository invoiceStRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceDiscardRepository invoiceDiscardRepository;

    // //

    // Service autowired
	
	@Autowired
	private GuiService guiService;

    /**
     * Esegue i controlli sulla riga identificata da loadingId e rowNumber.
     *
     * @param loadingId identificativo del loading
     * @param rowNumber numero di riga
     */
    public List<InvoiceCheckResultDTO> checkRow(@NotBlank String loadingId, 
    		                                    @NotNull Integer rowNumber,
    		                                    InvoiceStDTO row,
    		                                    List<CheckI> checks) {
        log.info("checkRow - START - loadingId: {}, rowNumber: {}", loadingId, rowNumber);

        // Ordina i check in base all'ordine definito
        checks.sort(Comparator.comparing(CheckI::getOrder));
        
        // Esegue i check sulla riga
        List<InvoiceCheckResultDTO> results = new java.util.ArrayList<>();
        for (CheckI check : checks) {
			log.info("checkRow - Eseguo check: " + check.getName() + " - order: " + check.getOrder());
			InvoiceCheckResultDTO result = check.check(row);
			results.add(result);
		}

        // Deduplica i risultati con lo stesso fieldName, accorpando su quello mantenuto
        // il checkFailed di quello scartato
        Map<String, InvoiceCheckResultDTO> mergedResults = new LinkedHashMap<>();
        for (InvoiceCheckResultDTO result : results) {
            InvoiceCheckResultDTO existing = mergedResults.get(result.getFieldName());
            if (existing == null) {
                mergedResults.put(result.getFieldName(), result);
            } else if (result.getCheckFailed() != null) {
                existing.setCheckFailed(existing.getCheckFailed() == null
                        ? result.getCheckFailed()
                        : existing.getCheckFailed() + " - " + result.getCheckFailed());
            }
        }

        List<InvoiceCheckResultDTO> result = new ArrayList<>(mergedResults.values());
        
        if(allPassed(result)) {
			log.info("checkRow - Tutti i check sono passati -> sposto il record in INVOICES");
			guiService.moveRowToInvoice(loadingId, rowNumber, row);
		} else {
			log.info("checkRow - Alcuni check non sono passati");
		}

        log.info("checkRow - END");
        return result;
    }

    /**
     * Verifica se tutti i risultati hanno passed a true.
     *
     * @param results i risultati dei check
     * @return true se tutti i risultati hanno passed = true
     */
    private boolean allPassed(List<InvoiceCheckResultDTO> results) {
        return results.stream().allMatch(r -> Boolean.TRUE.equals(r.getPassed()));
    }
}
