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
    	
    	// Caricamento Row da DB
        //InvoiceStDTO row = guiService.loadInvoiceStRow(loadingId, rowNumber);
        InvoiceStDTO row = new InvoiceStDTO();
        row.setInvoiceNumber("valore Filed1 di test");
        row.setInvoiceDate("valore Filed2 di test");
        row.setInvoiceNumber("invoiceNumber di test");    
        row.setBillingAccountNumber("billingAccountNumber di test");
        
    	List<InvoiceCheckResultDTO> checkResult = checkService.checkRow(loadingId, rowNumber, row, checks);
    	List<InvoiceCheckResultDTO> result = toResults(row);

    	// Sostituisce, per fieldName (case-insensitive), il bean base con quello con l'esito del check
    	for (int i = 0; i < result.size(); i++) {
    		InvoiceCheckResultDTO field = result.get(i);
    		for (InvoiceCheckResultDTO checked : checkResult) {
    			if (checked.getFieldName().equalsIgnoreCase(field.getFieldName())) {
    				result.set(i, checked);
    				break;
    			}
    		}
    	}

    	result.sort(Comparator.comparingInt(this::fieldEnumOrder));
    	
    	for(InvoiceCheckResultDTO field : result) {
			log.info("checkRow - fieldName: {}, fieldValue: {}, checkFailed: {}", 
					field.getFieldName(), field.getFieldValue(), field.getCheckFailed());
		}
    	
    	model.addAttribute("loadingId", loadingId);
        model.addAttribute("rowNumber", rowNumber); 
        model.addAttribute("fields", result);
        
		return "details"; // templates/home.html
	}

}
