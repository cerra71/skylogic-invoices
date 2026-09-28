package com.skylogic.invoice.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileCheckSummaryDTO {

    private String loadingId;

    private Long totalRows;

    private Long rowsChecked;

    private Long rowsPassed;

    private Long rowsFailed;

    private Long rowsAlreadyMoved;

    private boolean executed;

    private long durationMs;

    /**
     * Per ogni riga (rowNumber) che ha fallito almeno un check, contiene la lista
     * dei controlId (es. "RA-QA-01", "RA-QA-93") falliti ordinati per ordine di
     * esecuzione.
     */
    private Map<Integer, List<String>> rowFailedChecks = new LinkedHashMap<>();

    /**
     * Risultati aggregati a LIVELLO FILE per ogni field (stessa struttura della
     * tabella del check della singola riga).
     * <p>
     * Regola: <strong>basta anche un solo record fallito per rendere "Failed"
     * l'intera cella del campo</strong> (in pratica: AND tra tutte le righe).
     * La list mantiene lo stesso ordine dei campi della tabella del singolo
     * check (dedup per fieldName).
     * <p>
     * La voce {@link InvoiceCheckResultDTO#getFieldValue()} in questo caso
     * contiene un messaggio riassuntivo tipo "0/1500 fallite" vs "12/1500 fallite".
     */
    private List<InvoiceCheckResultDTO> fileLevelResults = new ArrayList<>();

    public FileCheckSummaryDTO(String loadingId) {
        this.loadingId = loadingId;
        this.totalRows = 0L;
        this.rowsChecked = 0L;
        this.rowsPassed = 0L;
        this.rowsFailed = 0L;
        this.rowsAlreadyMoved = 0L;
        this.executed = false;
        this.durationMs = 0L;
        this.rowFailedChecks = new LinkedHashMap<>();
        this.fileLevelResults = new ArrayList<>();
    }

    public void addFailedChecks(Integer rowNumber, List<String> failedControlIds) {
        if (failedControlIds == null || failedControlIds.isEmpty()) {
            return;
        }
        if (!rowFailedChecks.containsKey(rowNumber)) {
            rowFailedChecks.put(rowNumber, new ArrayList<>());
        }
        for (String id : failedControlIds) {
            if (!rowFailedChecks.get(rowNumber).contains(id)) {
                rowFailedChecks.get(rowNumber).add(id);
            }
        }
    }
}
