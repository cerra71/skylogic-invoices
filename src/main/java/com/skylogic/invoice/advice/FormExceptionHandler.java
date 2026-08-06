package com.skylogic.invoice.advice;

import com.skylogic.invoice.controller.GuiController;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;



// Classe che gestisce gli errori relativi ai parametri dei form del {@link GuiController}.
// Ha precedenza rispetto al GlobalExceptionHandler, che rimane invece responsabile degli errori generali e
// imprevisti dell'applicazione.

@ControllerAdvice(assignableTypes = GuiController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class FormExceptionHandler {

    // Check: parametro obbligatorio mancante (loading id, rowNumber)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public String handleMissingParameter(MissingServletRequestParameterException ex, Model model,
                                         HttpServletRequest request) {

        Set<String> messages = collectRequiredFieldErrors(request);

        if (messages.isEmpty()) {
            if ("loadingId".equals(ex.getParameterName())) {
                messages.add("Loading ID is required");
            } else if ("rowNumber".equals(ex.getParameterName())) {
                messages.add("Row Number is required");
            } else {
                messages.add("Required field is missing");
            }
        }

        String message = String.join(" and ", messages);

        log.warn( "handleMissingParameter - Missing request parameter: {}", ex.getParameterName());

        return showDetailsError(model, message, request);
    }

    // Check: abc, 1.5
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleTypeMismatch(MethodArgumentTypeMismatchException ex, Model model,
                                     HttpServletRequest request) {

        Set<String> messages = collectRequiredFieldErrors(request);

        if ("rowNumber".equals(ex.getName())) {
            messages.add("Row Number must be an integer");
        } else {
            messages.add("Invalid search value");
        }

        String message = String.join(" and ", messages);

        log.warn("handleTypeMismatch - Invalid value '{}' for parameter '{}'", ex.getValue(), ex.getName());

        return showDetailsError(model, message, request);

    }

    // Check: loadingId "" oppure spazi, rowNumber = 0 o negativo
    @ExceptionHandler(HandlerMethodValidationException.class)
    public String handleValidationException(HandlerMethodValidationException ex, Model model,
                                            HttpServletRequest request) {

        String message = ex.getAllErrors()
                .stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(" and "));

        if (message.isBlank()) {
            message = "Invalid search value";
        }

        log.warn("handleValidationException - Validation error: {}", message);

        return showDetailsError(model, message, request);
    }



    // METODI HELPER

    // Raccoglie esclusivamente gli errori relativi ai campi obbligatori.
    // Questo controllo è necessario negli handler che intercettano errori avvenuti prima della validazione
    // completa del metodo come un parametro mancante o un valore non convertibile.
    // Un LinkedHashSet evita messaggi duplicati e mantiene l'ordine: prima Loading ID, poi Row number.
    private Set<String> collectRequiredFieldErrors(HttpServletRequest request) {

        Set<String> messages = new LinkedHashSet<>();

        String loadingId = request.getParameter("loadingId");
        String rowNumber = request.getParameter("rowNumber");

        if (loadingId == null || loadingId.isBlank()) {
            messages.add("Loading ID is required");
        }

        if (rowNumber == null || rowNumber.isBlank()) {
            messages.add("Row Number is required");
        }

        return messages;
    }

    // Prepara il Model comune a tutti gli errori del form e restituisce nuovamente la pagina details.
    private String showDetailsError(Model model, String message, HttpServletRequest request) {

        model.addAttribute("errorMessage", message);
        model.addAttribute("checkEnabled", false);

        // Rimette nei campi i valori digitati dall'utente.
        model.addAttribute("loadingId", request.getParameter("loadingId"));
        model.addAttribute("rowNumber", request.getParameter("rowNumber"));

        return "details";
    }
}




