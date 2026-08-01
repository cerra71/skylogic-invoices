package com.skylogic.invoice.controller;

import java.util.Collections;
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

import com.skylogic.invoice.dto.InvoiceCheckResultDTO;
import com.skylogic.invoice.dto.InvoiceDTO;
import com.skylogic.invoice.dto.InvoiceStDTO;
import com.skylogic.invoice.dto.LoadingSummaryDTO;
import com.skylogic.invoice.mapper.InvoiceStToInvoiceMapper;
import com.skylogic.invoice.service.GuiService;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Controller
public class GuiController extends GenericController {

    @Autowired
    private GuiService guiService;
    
    @Autowired
    private InvoiceStToInvoiceMapper invoiceStToInvoiceMapper;

    /**
     * Mostra la pagina home
     *
     * @return il nome della view {@code home}
     */
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserDetails userDetails, 
    		           Model model) {

        log.info("home - START: Utente autenticato: {}", userDetails.getUsername());

        List<LoadingSummaryDTO> loadings = guiService.getLoadings();
        model.addAttribute("loadings", loadings);

        return "home"; // templates/home.html
    }

    /**
     * Mostra la pagina details
     *
     * @return il nome della view {@code details}
     */
    @GetMapping("/details")
    public String details(Model model,
                          @RequestParam(name = "loadingId", required = false) String loadingId) {

        log.info("details - START");

        // Valori standard della pagina al caricamento
        model.addAttribute("loadingId", loadingId);
        model.addAttribute("rowNumber", "1");
        model.addAttribute("checkEnabled", false);

        return "details"; // templates/home.html
    }
    
    @PostMapping("/loadRow")
    public String loadRow(Model model,
                          @RequestParam("loadingId") String loadingId,
                          @RequestParam("rowNumber") Integer rowNumber) {

        log.info("loadRow - START: loadingId: {}, rowNumber: {}", loadingId, rowNumber);

        InvoiceStDTO row = new InvoiceStDTO(); // Inizializziamo row a un oggetto vuoto per evitare null pointer exception
        List<InvoiceCheckResultDTO> fields = Collections.emptyList();

        // Prima ricerca: cerchiamo il record nella tabella invoice_st.
        InvoiceStDTO invoiceStRow = guiService.loadInvoiceStRow(loadingId, rowNumber);
        log.debug("loadRow - invoiceStRow: {}", invoiceStRow);

        // Se il record viene trovato in invoice_st
        if (invoiceStRow != null) {
        	row = invoiceStRow;
            model.addAttribute("checkEnabled", true);
        } else {
            InvoiceDTO invoiceRow = guiService.loadInvoiceRow(loadingId, rowNumber);
            log.debug("loadRow - invoiceRow: {}", invoiceRow);

            // Il record non esiste in nessuna delle due tabelle.
            if (invoiceRow == null) {
                //model.addAttribute("fields", Collections.emptyList());
                model.addAttribute("checkEnabled", false);
                model.addAttribute("errorMessage", "Nessun record trovato per Loading ID " + loadingId + " e Row Number " + rowNumber);

                log.warn("Record not found: loadingId: {}, rowNumber: {}", loadingId, rowNumber);
            // Il record viene trovato nella tabella invoice (= ha già superato i check)
            } else {
            	row = invoiceStToInvoiceMapper.toEntity(invoiceRow);
                // Il record è già stato controllato, quindi il pulsante Run Check è disabilitato.
                model.addAttribute("checkEnabled", false);
            }
        }
        
        // Il risultato è passato a toResults()
        fields = toResults(row);
        log.debug("loadRow - fields: {}", fields);
        
        /*
         * A questo punto fields è costruito:
         *
         * - da InvoiceStDTO, se il record era in invoice_st;
         * - da InvoiceDTO, se il record era in invoice.
         *
         * In entrambi i casi il risultato finale è lo stesso tipo: List<InvoiceCheckResultDTO>.
         *
         */
        fields.sort(Comparator.comparingInt(this::fieldEnumOrder));

        // Aggiorna i valori
        model.addAttribute("loadingId", loadingId);
        model.addAttribute("rowNumber", rowNumber);
        model.addAttribute("fields", fields);

        log.info("loadRow - END");

        return "details";   // templates/details.html
    }

    /**
     * 
     * @param model
     * @param loadingId
     * @param rowNumber
     * @return
     */
    @PostMapping("/moveRowToStaging")
    public String moveRowToStaging(Model model,
                                   @RequestParam("loadingId") String loadingId,
                                   @RequestParam("rowNumber") Integer rowNumber) {

        log.info("moveRowToStaging - START: loadingId: {}, rowNumber: {}", loadingId, rowNumber);

        guiService.moveRowToStaging(loadingId, rowNumber);

        log.info("moveRowToStaging - END");

        return loadRow(model, loadingId, rowNumber);
    }
}