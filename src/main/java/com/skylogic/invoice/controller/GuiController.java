package com.skylogic.invoice.controller;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.skylogic.invoice.dto.FieldEnum;
import com.skylogic.invoice.dto.InvoiceCheckResultDTO;
import com.skylogic.invoice.dto.InvoiceStDTO;
import com.skylogic.invoice.service.GuiService;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Controller
public class GuiController extends GenericController {

	@Autowired
	private GuiService guiService;

    /**
     * Mostra la pagina home
     * 
     * @return il nome della view {@code home}
     */
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserDetails userDetails, 
    		           Model model) {
        log.info("home - START: Utente autenticato: {}", userDetails.getUsername());
                
        //model.addAttribute("tickets", filteredTickets.getContent());

        return "home"; // templates/home.html
    }
    
    /**
     * Mostra la pagina details
     * 
     * @return il nome della view {@code details}
     */
    @GetMapping("/details")
    public String details(@AuthenticationPrincipal UserDetails userDetails, 
    		              Model model,
    		              @RequestParam(name = "loadingId", required = false) String loadingId) {
        log.info("details - START");
                
        model.addAttribute("loadingId", loadingId);
        model.addAttribute("rowNumber", "1");        

        return "details"; // templates/home.html
    }

    /**
     * Carica la riga richiesta e la mostra nella pagina details, trasformando
     * l'{@link InvoiceStDTO} in un {@link InvoiceCheckResultDTO} per ciascun campo
     * (senza esito di check, che viene valorizzato solo da {@code /checkRow}).
     *
     * @return il nome della view {@code details}
     */
    @PostMapping("/loadRow")
    public String loadRow(@AuthenticationPrincipal UserDetails userDetails,
    		               Model model,
    		               @RequestParam("loadingId") String loadingId,
    		               @RequestParam("rowNumber") Integer rowNumber) {
        log.info("loadRow - START: loadingId: {}, rowNumber: {}", loadingId, rowNumber);

        InvoiceStDTO row = guiService.loadRow(loadingId, rowNumber);
        List<InvoiceCheckResultDTO> fields = toResults(row);
        fields.sort(Comparator.comparingInt(this::fieldEnumOrder));

        model.addAttribute("loadingId", loadingId);
        model.addAttribute("rowNumber", rowNumber);
        model.addAttribute("fields", fields);

        log.info("loadRow - END");
        return "details"; // templates/details.html
    }

    /**
     * Trasforma i campi dell'{@link InvoiceStDTO} in una lista di {@link InvoiceCheckResultDTO},
     * una riga per ciascuna proprietà, con il risultato del check ancora non valorizzato.
     */
    private List<InvoiceCheckResultDTO> toResults(InvoiceStDTO row) {
        List<InvoiceCheckResultDTO> results = new ArrayList<>();
        try {
            PropertyDescriptor[] properties = Introspector.getBeanInfo(InvoiceStDTO.class, Object.class).getPropertyDescriptors();
            for (PropertyDescriptor property : properties) {
                Object value = property.getReadMethod().invoke(row);
                InvoiceCheckResultDTO result = new InvoiceCheckResultDTO();
                result.setFieldName(property.getName());
                result.setFieldValue(value != null ? value.toString() : null);
                results.add(result);
            }
        } catch (Exception e) {
            log.error("toResults - Errore durante la trasformazione di InvoiceStDTO", e);
        }
        return results;
    }

    

}
