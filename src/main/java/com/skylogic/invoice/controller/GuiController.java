package com.skylogic.invoice.controller;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.skylogic.invoice.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
    public String home(@AuthenticationPrincipal UserDetails userDetails, Model model) {

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
    public String details(@AuthenticationPrincipal UserDetails userDetails, Model model,
                          @RequestParam(name = "loadingId", required = false) String loadingId) {

        log.info("details - START");

        // Valori standard della pagina al caricamento
        model.addAttribute("loadingId", loadingId);
        model.addAttribute("rowNumber", "1");
        model.addAttribute("checkEnabled", false);

        return "details"; // templates/home.html
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
                          @RequestParam("loadingId") String loadingId,
                          @RequestParam("rowNumber") Integer rowNumber) {

        log.info("loadRow - START: loadingId: {}, rowNumber: {}", loadingId, rowNumber);

        // Rimandiamo alla pagina i parametri della ricerca per renderli visibili nei campi del form
        model.addAttribute("loadingId", loadingId);
        model.addAttribute("rowNumber", rowNumber);

        /*
         * Questa sarà la lista comune utilizzata da details.html.
         *
         * Il record può arrivare da invoice_st o da invoice: entrambi devono diventare una lista
         * di InvoiceCheckResultDTO.
         */
        List<InvoiceCheckResultDTO> fields;

        // Prima ricerca: cerchiamo il record nella tabella invoice_st.
        InvoiceStDTO invoiceStRow = guiService.loadInvoiceStRow(loadingId, rowNumber);

        // Se il record viene trovato in invoice_st
        if (invoiceStRow != null) {

            // Il risultato è passato a toResults()
            fields = toResults(invoiceStRow);

            /*
             * Il record non è ancora stato controllato.
             * Il pulsante Run Check deve essere abilitato.
             */
            model.addAttribute("checkEnabled", true);

        } else {

            /*
             * Il record non è stato trovato in invoice_st.
             * Seconda ricerca: cerchiamo il record nella tabella invoice.
             */
            InvoiceDTO invoiceRow = guiService.loadInvoiceRow(loadingId, rowNumber);


            // Il record non esiste in nessuna delle due tabelle.
            if (invoiceRow == null) {

                model.addAttribute("fields", Collections.emptyList());
                model.addAttribute("checkEnabled", false);
                model.addAttribute("errorMessage", "Nessun record trovato per Loading ID " + loadingId + " e Row Number " + rowNumber);

                log.warn("Record not found: loadingId: {}, rowNumber: {}", loadingId, rowNumber);

                return "details"; // templates/details.html

            // Il record viene trovato nella tabella invoice (= ha già superato i check)
            } else {

                // Il risultato è passato a toResults()
                fields = toResults(invoiceRow);

                // Il record è già stato controllato, quindi il pulsante Run Check è disabilitato.
                model.addAttribute("checkEnabled", false);
            }

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

        }

        return "details";   // templates/details.html
    }
}