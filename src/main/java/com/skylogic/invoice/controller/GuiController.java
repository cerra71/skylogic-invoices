package com.skylogic.invoice.controller;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.skylogic.invoice.dto.*;
import com.skylogic.invoice.service.KpiDocumentationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.skylogic.invoice.check.CheckI;
import com.skylogic.invoice.entity.KpiDocumentation;
import com.skylogic.invoice.mapper.InvoiceStToInvoiceMapper;
import com.skylogic.invoice.service.GuiService;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Controller
public class GuiController extends GenericController {

    @Autowired
    private GuiService guiService;

    @Autowired
    private KpiDocumentationService kpiDocumentationService;
    
    @Autowired
    private InvoiceStToInvoiceMapper invoiceStToInvoiceMapper;

    @Autowired
    private List<CheckI> checks;

    /**
     * Mostra la pagina home
     *
     * @return il nome della view {@code home}
     */
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal UserDetails userDetails, Model model) {

        log.info("home - START: Utente autenticato: {}", userDetails.getUsername());

        List<LoadingSummaryDTO> loadings = guiService.getLoadings();
        model.addAttribute("loadings", loadings);

        return "home"; // templates/home.html
    }

    /**
     * Mostra la pagina checks con l'elenco di tutti i controlli ordinati per order
     *
     * @return il nome della view {@code checks}
     */
    @GetMapping("/checks")
    public String checks(@AuthenticationPrincipal UserDetails userDetails, Model model) {

        log.info("checks - START: Utente autenticato: {}", userDetails.getUsername());

        List<CheckI> sortedChecks = checks.stream()
                .sorted(Comparator.comparing(CheckI::getOrder))
                .toList();
        model.addAttribute("checks", sortedChecks);

        return "checks"; // templates/checks.html
    }

    /**
     * Mostra la pagina documentation con form di ricerca e tabella risultati.
     * Accetta parametri opzionali: se presenti esegue la ricerca.
     *
     * @return il nome della view {@code documentation}
     */
    @GetMapping("/documentation")
    public String documentation(@AuthenticationPrincipal UserDetails userDetails,Model model,
                                @RequestParam(name = "kpiId", required = false) String kpiId,
                                @RequestParam(name = "controlId", required = false) String controlId,
                                @RequestParam(name = "field", required = false) String field) {

        log.info("documentation - START: Utente autenticato: {}, kpiId: {}, controlId: {}, field: {}",
                userDetails.getUsername(), kpiId, controlId, field);

        model.addAttribute("fieldEnums", FieldEnum.values());
        model.addAttribute("kpiId", kpiId);
        model.addAttribute("controlId", controlId);
        model.addAttribute("field", field);

        List<KpiDocumentation> results = kpiDocumentationService.searchKpiDocumentation(kpiId, controlId, field);

        model.addAttribute("results", results);

        return "documentation"; // templates/documentation.html
    }

    /**
     * Inserisce una nuova documentazione KPI.
     */
    @PostMapping("/documentation")
    public String createDocumentation( @Valid KpiDocumentationDTO documentationDTO, BindingResult bindingResult){

        // Impedisce lato server i campi vuoti o con soli spazi e ritorna alla pagina
        if (bindingResult.hasErrors()) {
            return "redirect:/documentation";
        }

        KpiDocumentation documentation = KpiDocumentation.builder()
                .kpiId(documentationDTO.getKpiId().trim())
                .controlId(documentationDTO.getControlId().trim())
                .title(documentationDTO.getTitle().trim())
                .field(documentationDTO.getField().trim())
                .build();

        kpiDocumentationService.saveKpiDocumentation(documentation);

        return "redirect:/documentation";
    }

    /**
     * Mostra la pagina details
     *
     * @return il nome della view {@code details}
     */
    @GetMapping("/details")
    public String details(@AuthenticationPrincipal UserDetails userDetails, Model model,
                          @RequestParam(name = "loadingId", required = false) String loadingId) {

        log.info("details - START");

        // Valori standard della pagina al caricamento
        model.addAttribute("loadingId", loadingId);
        model.addAttribute("rowNumber", "1");
        model.addAttribute("checkEnabled", false);
        model.addAttribute("showRowDetails", false);
        model.addAttribute("showFileDetails", false);

        return "details"; // templates/home.html
    }

    /**
     * Elimina tutte le righe di un loading (invoice_st + invoice_discard) e torna alla home.
     */
    @PostMapping("/deleteLoading")
    public String deleteLoading(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam("loadingId") String loadingId) {
        log.info("deleteLoading - START: loadingId: {}", loadingId);
        guiService.deleteLoading(loadingId);
        return "redirect:/home";
    }

    /**
     * Carica la riga richiesta e la mostra nella pagina details, trasformando
     * l'{@link InvoiceStDTO} e l'{@link InvoiceDTO}  in un {@link InvoiceCheckResultDTO} per ciascun campo
     * (senza esito di check, che viene valorizzato solo da {@code /checkRow}).
     *
     * @return il nome della view {@code details}
     */
    @PostMapping("/loadRow")
    public String loadRow(@AuthenticationPrincipal UserDetails userDetails,
                          Model model,
                          @RequestParam("loadingId") @NotBlank(message = "Loading ID is required") String loadingId,
                          @RequestParam("rowNumber") @Min(value = 1, message = "Row Number must be at least 1") Integer rowNumber) {

        log.info("loadRow - START: loadingId: {}, rowNumber: {}", loadingId, rowNumber);

        InvoiceStDTO row = new InvoiceStDTO(); // Inizializziamo row a un oggetto vuoto per evitare null pointer exception
        List<InvoiceCheckResultDTO> fields = Collections.emptyList();

        // Pulsanti
        model.addAttribute("checkEnabled", false);
        model.addAttribute("pushBackEnabled", false);

        // Div di dettaglio: non visibili al caricamento della riga
        model.addAttribute("showRowDetails", false);
        model.addAttribute("showFileDetails", false);

        // Prima ricerca: cerchiamo il record nella tabella invoice_st.
        InvoiceStDTO invoiceStRow = guiService.loadInvoiceStRow(loadingId, rowNumber);
        log.debug("loadRow - invoiceStRow: {}", invoiceStRow);

        // Se il record viene trovato in invoice_st
        if (invoiceStRow != null) {
        	row = invoiceStRow;

            // Pulsanti
            model.addAttribute("checkEnabled", true);
            model.addAttribute("pushBackEnabled", false);

        } else {
            InvoiceDTO invoiceRow = guiService.loadInvoiceRow(loadingId, rowNumber);
            log.debug("loadRow - invoiceRow: {}", invoiceRow);

            // Il record non esiste in nessuna delle due tabelle.
            if (invoiceRow == null) {
                model.addAttribute("fields", Collections.emptyList());
                model.addAttribute("errorMessage", "No record found for Loading ID " + loadingId + " and Row number " + rowNumber);

                // Pulsanti
                model.addAttribute("checkEnabled", false);
                model.addAttribute("pushBackEnabled", false);

                log.warn("Record not found: loadingId: {}, rowNumber: {}", loadingId, rowNumber);

                // Aggiorna i valori
                model.addAttribute("loadingId", loadingId);
                model.addAttribute("rowNumber", rowNumber);
                model.addAttribute("fields", fields);

                return "details"; // templates/details.html

                // Il record viene trovato nella tabella invoice (= ha già superato i check)

            } else {
            	row = invoiceStToInvoiceMapper.toEntity(invoiceRow);

                // Pulsanti: il record è già stato controllato, quindi il pulsante Run Check è disabilitato
                // e il pulsante Push Back abilitato.
                model.addAttribute("checkEnabled", false);
                model.addAttribute("pushBackEnabled", true);
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
        
        // 8. Div di dettaglio: mostra solo la riga controllata
     	model.addAttribute("showRowDetails", true);
     	model.addAttribute("showFileDetails", false);

        log.info("loadRow - END");

        return "details";   // templates/details.html
    }


    /**
     * Sposta una riga dalla tabella invoice alla tabella invoice_st
     * e aggiorna la pagina details con il nuovo stato del record.
     *
     * @return il nome della view {@code details}
     */
    @PostMapping("/moveRowToStaging")
    public String moveRowToStaging( @RequestParam("loadingId") String loadingId,
            @RequestParam("rowNumber") Integer rowNumber,
            Model model) {

        // 1. Carica il record dalla tabella invoice
        InvoiceDTO invoiceDTO = guiService.loadInvoiceRow(loadingId, rowNumber);

        // 2. Sposta il record da invoice a invoice_st
        guiService.moveRowToStaging(loadingId, rowNumber, invoiceDTO);

        // 3. Ricarica il record dalla tabella invoice_st
        InvoiceStDTO row = guiService.loadInvoiceStRow(loadingId, rowNumber);

        // 4. Trasforma i campi del record in una lista da visualizzare nella pagina details
        List<InvoiceCheckResultDTO> fields = toResults(row);

        // 5. Ordina i campi secondo l'ordine definito in FieldEnum
        fields.sort(Comparator.comparingInt(this::fieldEnumOrder));

        // 6. Ripopola il Model con i dati del record appena riportato in staging
        model.addAttribute("loadingId", loadingId);
        model.addAttribute("rowNumber", rowNumber);
        model.addAttribute("fields", fields);

        // 7. Aggiorna stato Pulsanti (record in staging)
        model.addAttribute("checkEnabled", true);
        model.addAttribute("pushBackEnabled", false);

        // 8. Div di dettaglio: non visibili dopo il push back
        model.addAttribute("showRowDetails", false);
        model.addAttribute("showFileDetails", false);

        return "details";
    }


}

