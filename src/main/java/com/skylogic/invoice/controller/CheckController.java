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
import com.skylogic.invoice.service.CheckService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class CheckController extends GenericController {
	
	@Autowired
	private CheckService checkService;

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
    	List<InvoiceCheckResultDTO> result = checkService.checkRow(loadingId, rowNumber, checks);
    	result.sort(Comparator.comparingInt(this::fieldEnumOrder));
    	
    	model.addAttribute("loadingId", loadingId);
        model.addAttribute("rowNumber", rowNumber); 
        model.addAttribute("fields", result);
        
		return "details"; // templates/home.html
	}

}
