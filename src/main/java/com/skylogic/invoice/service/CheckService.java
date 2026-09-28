package com.skylogic.invoice.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.skylogic.invoice.check.GenericCheck;
import com.skylogic.invoice.dto.FieldEnum;
import com.skylogic.invoice.dto.FileCheckSummaryDTO;
import com.skylogic.invoice.repository.InvoiceDiscardRepository;
import com.skylogic.invoice.repository.InvoiceRepository;
import com.skylogic.invoice.repository.InvoiceStRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
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

    @Autowired
    private InvoiceStRepository invoiceStRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceDiscardRepository invoiceDiscardRepository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

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

        checks.sort(Comparator.comparing(CheckI::getOrder));

        List<InvoiceCheckResultDTO> results = new java.util.ArrayList<>();
        for (CheckI check : checks) {
			log.info("checkRow - Eseguo check: " + check.getName() + " - order: " + check.getOrder());
			InvoiceCheckResultDTO result = check.check(row);
			results.add(result);
		}

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
     * Esegue tutti i controlli per ogni riga residua del file in invoice_st
     * e restituisce un summary contabile dell'operazione con:
     * <ul>
     *   <li>elenco check falliti per ogni riga ({@code rowFailedChecks});</li>
     *   <li>tabella aggregata a livello file ({@code fileLevelResults}):
     *       1 sola riga per field, globale su TUTTO il file (1 solo fallimento
     *       in 1 riga → tutto il campo considerato Failed).</li>
     * </ul>
     *
     * <p>OTTIMIZZAZIONE VELOCITÀ: i controlli che estendono GenericCheck
     * vengono eseguiti in BATCH (1 sola query per check che restituisce
     * row_num + result per TUTTE le righe) e poi riusi in memoria.
     */
    public FileCheckSummaryDTO checkFile(@NotBlank String loadingId, List<CheckI> checks) {
        log.info("checkFile - START (mode: batch for GenericChecks) - loadingId: {}", loadingId);
        long start = System.currentTimeMillis();

        FileCheckSummaryDTO summary = new FileCheckSummaryDTO(loadingId);

        List<CheckI> orderedChecks = new ArrayList<>(checks);
        orderedChecks.sort(Comparator.comparing(CheckI::getOrder));

        List<InvoiceStDTO> stagingRows = guiService.loadInvoiceStRows(loadingId);
        summary.setTotalRows((long) stagingRows.size());
        summary.setRowsChecked(0L);
        summary.setRowsPassed(0L);
        summary.setRowsFailed(0L);

        long alreadyMoved = invoiceRepository.countByLoadingId(loadingId);
        summary.setRowsAlreadyMoved(alreadyMoved);

        List<Integer> allRowNumbers = new ArrayList<>();
        for (InvoiceStDTO row : stagingRows) {
            if (row.getRowNum() != null) {
                allRowNumbers.add(row.getRowNum().intValue());
            }
        }

        Map<CheckI, Map<Integer, Boolean>> batchCache = new HashMap<>();
        for (CheckI check : orderedChecks) {
            if (!(check instanceof GenericCheck)) {
                continue;
            }
            GenericCheck gc = (GenericCheck) check;
            Map<Integer, Boolean> singleCheckResult = runBatchCheck(gc, loadingId, allRowNumbers);
            batchCache.put(check, singleCheckResult);
        }

        LinkedHashMap<String, AggregatedFieldResult> aggregated = new LinkedHashMap<>();
        long totalRowsForAggregate = (long) stagingRows.size();

        for (InvoiceStDTO row : stagingRows) {
            Integer rowNumber = row.getRowNum() != null ? row.getRowNum().intValue() : null;
            if (rowNumber == null) {
                continue;
            }

            List<InvoiceCheckResultDTO> rowResults = new ArrayList<>();
            for (CheckI check : orderedChecks) {
                Map<Integer, Boolean> perRow = batchCache.get(check);
                InvoiceCheckResultDTO dto;
                if (perRow != null) {
                    Boolean passed = perRow.get(rowNumber);
                    dto = createResultFromCache(check, row, passed != null && passed);
                } else {
                    dto = check.check(row);
                }
                rowResults.add(dto);
            }

            List<InvoiceCheckResultDTO> mergedRowResults = mergeByField(rowResults);

            for (InvoiceCheckResultDTO r : mergedRowResults) {
                String fieldName = r.getFieldName() != null
                        ? r.getFieldName()
                        : ("__no_field__" + System.identityHashCode(r));
                AggregatedFieldResult agg = aggregated.computeIfAbsent(fieldName,
                        k -> new AggregatedFieldResult(r, totalRowsForAggregate));
                agg.tickRow(r);
            }

            summary.setRowsChecked(summary.getRowsChecked() + 1);
            if (allPassed(mergedRowResults)) {
                summary.setRowsPassed(summary.getRowsPassed() + 1);
                log.debug("checkFile - Row {} passed → move to invoice", rowNumber);
                guiService.moveRowToInvoice(loadingId, rowNumber, row);
            } else {
                summary.setRowsFailed(summary.getRowsFailed() + 1);

                List<String> failedIds = new ArrayList<>();
                for (InvoiceCheckResultDTO checked : mergedRowResults) {
                    if (!Boolean.TRUE.equals(checked.getPassed())) {
                        String cid = (checked.getControlId() != null && !checked.getControlId().isBlank())
                                ? checked.getControlId()
                                : null;
                        if (cid == null && checked.getCheckFailed() != null && checked.getCheckFailed().contains("RA-QA-")) {
                            String raw = checked.getCheckFailed();
                            int idx = raw.indexOf("RA-QA-");
                            if (idx >= 0) {
                                int end = idx + 7;
                                while (end < raw.length() && Character.isLetterOrDigit(raw.charAt(end))) {
                                    end++;
                                }
                                cid = raw.substring(idx, Math.min(end, raw.length()));
                            }
                        }
                        if (cid != null && !failedIds.contains(cid)) {
                            failedIds.add(cid);
                        }
                    }
                }
                summary.addFailedChecks(rowNumber, failedIds);
            }
        }

        List<InvoiceCheckResultDTO> fileLevelList = new ArrayList<>();
        for (AggregatedFieldResult agg : aggregated.values()) {
            fileLevelList.add(agg.toDto());
        }
        summary.setFileLevelResults(fileLevelList);

        summary.setDurationMs(System.currentTimeMillis() - start);
        summary.setExecuted(true);
        log.info(
                "checkFile - END (batch mode) - loadingId: {}, checked: {}, passed: {}, failed: {}, alreadyMoved: {}, ms: {}",
                loadingId, summary.getRowsChecked(), summary.getRowsPassed(), summary.getRowsFailed(),
                summary.getRowsAlreadyMoved(), summary.getDurationMs()
        );
        return summary;
    }

    /**
     * Esegue UNA SOLA query SQL in batch per un GenericCheck.
     */
    private Map<Integer, Boolean> runBatchCheck(GenericCheck check,
                                                String loadingId,
                                                List<Integer> rowNumbers) {
        Map<Integer, Boolean> empty = new HashMap<>();
        if (rowNumbers == null || rowNumbers.isEmpty()) {
            return empty;
        }

        String baseSql;
        try {
            baseSql = check.getSql();
        } catch (Exception e) {
            log.warn("runBatchCheck - cannot get SQL for check {}, fallback single (getSql failed: {})",
                    check.getName(), e.getMessage());
            return empty;
        }
        if (baseSql == null || baseSql.isBlank()) {
            log.warn("runBatchCheck - empty SQL for check {} → fallback single-row mode", check.getName());
            return empty;
        }

        String batchSql = buildBatchSql(baseSql);
        if (batchSql == null) {
            log.warn("runBatchCheck - cannot build batch SQL for check {} → fallback single-row mode",
                    check.getName());
            return empty;
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("loadingId", loadingId)
                .addValue("rowNumbers", rowNumbers);

        try {
            Map<Integer, Boolean> result = new HashMap<>();
            jdbcTemplate.query(
                    batchSql,
                    params,
                    rs -> {
                        int rn = rs.getInt("rn");
                        String val = rs.getString("result");
                        result.put(rn, "Passed".equals(val));
                    }
            );
            log.debug("runBatchCheck - check {} returned {} rows", check.getName(), result.size());
            return result;
        } catch (Exception e) {
            log.error("runBatchCheck - ERROR on check {} - fallback single-row mode - {}",
                    check.getName(), e.getMessage());
            return empty;
        }
    }

    /**
     * Trasforma una SQL "singola riga" nella variante batch che restituisce
     * anche {@code row_num AS rn} e usa {@code IN (:rowNumbers)}.
     * Nessuna lambda, compatibile Java 8.
     */
    private String buildBatchSql(String baseSql) {
        try {
            String sql = baseSql;

            sql = sql.replaceAll("(?i)row_num\\s*=\\s*:rowNumber\\b", "row_num IN (:rowNumbers)");

            Pattern aliasRowNum = Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\.row_num\\s*=\\s*:rowNumber\\b", Pattern.CASE_INSENSITIVE);
            Matcher mAlias = aliasRowNum.matcher(sql);
            StringBuffer sbAlias = new StringBuffer();
            while (mAlias.find()) {
                String alias = mAlias.group(1);
                mAlias.appendReplacement(sbAlias, Matcher.quoteReplacement(alias + ".row_num IN (:rowNumbers)"));
            }
            mAlias.appendTail(sbAlias);
            sql = sbAlias.toString();

            sql = sql.replaceAll(":rowNumber\\b", ":rowNumbers");

            int selectIdx = indexOfCaseInsensitive(sql, "SELECT");
            if (selectIdx < 0) {
                return null;
            }

            int firstFrom = indexOfCaseInsensitive(sql, "FROM");
            if (firstFrom < 0 || firstFrom <= selectIdx) {
                return null;
            }

            String betweenSelectAndFrom = sql.substring(selectIdx + "SELECT".length(), firstFrom);
            if (betweenSelectAndFrom.toLowerCase().contains(" rn ") || betweenSelectAndFrom.toLowerCase().contains(" as rn")) {
                return sql;
            }

            int afterFromNonWs = findInvoiceStTableOpenParen(sql, firstFrom);
            String candidateAlias = findInvoiceStAlias(sql, firstFrom, afterFromNonWs);

            String rowProjection;
            if (candidateAlias != null && !candidateAlias.isBlank()) {
                rowProjection = candidateAlias.trim() + ".row_num AS rn, ";
            } else {
                rowProjection = "row_num AS rn, ";
            }

            return sql.substring(0, selectIdx + "SELECT".length()) +
                    " " + rowProjection +
                    sql.substring(selectIdx + "SELECT".length());
        } catch (Exception e) {
            log.error("buildBatchSql - transformation error - {}", e.getMessage());
            return null;
        }
    }

    private int indexOfCaseInsensitive(String s, String token) {
        if (s == null || token == null) return -1;
        return s.toLowerCase().indexOf(token.toLowerCase());
    }

    private int findInvoiceStTableOpenParen(String sql, int firstFromIdx) {
        String afterFrom = sql.substring(firstFromIdx + "FROM".length());
        int skippedWs = 0;
        while (skippedWs < afterFrom.length() && Character.isWhitespace(afterFrom.charAt(skippedWs))) {
            skippedWs++;
        }
        return firstFromIdx + "FROM".length() + skippedWs;
    }

    private String findInvoiceStAlias(String sql, int firstFromIdx, int afterFromNonWs) {
        String tail = sql.substring(afterFromNonWs);
        String tokenEndPunctuation = " \t\n\r\f,;.)";
        int end = 0;
        while (end < tail.length() && tokenEndPunctuation.indexOf(tail.charAt(end)) < 0) {
            end++;
        }
        int aliasStart = end;
        while (aliasStart < tail.length() && Character.isWhitespace(tail.charAt(aliasStart))) {
            aliasStart++;
        }
        if (aliasStart >= tail.length()) {
            return null;
        }
        String possibleAs = null;
        int t = aliasStart;
        while (t < tail.length() && tokenEndPunctuation.indexOf(tail.charAt(t)) < 0) {
            t++;
        }
        if (t > aliasStart) {
            possibleAs = tail.substring(aliasStart, t);
        }
        if (possibleAs == null) {
            return null;
        }
        if (possibleAs.equalsIgnoreCase("AS")) {
            int afterAs = t;
            while (afterAs < tail.length() && Character.isWhitespace(tail.charAt(afterAs))) {
                afterAs++;
            }
            int aliasEnd = afterAs;
            while (aliasEnd < tail.length() && tokenEndPunctuation.indexOf(tail.charAt(aliasEnd)) < 0) {
                aliasEnd++;
            }
            if (aliasEnd > afterAs) {
                return tail.substring(afterAs, aliasEnd);
            }
            return null;
        }
        return possibleAs;
    }

    private InvoiceCheckResultDTO createResultFromCache(CheckI check, InvoiceStDTO row, boolean passed) {
        String fieldValue = "";
        FieldEnum fe = check.getField();
        if (fe != null) {
            try {
                String getterName = "get" + Character.toUpperCase(fe.getValue().charAt(0)) +
                        (fe.getValue().length() > 1 ? fe.getValue().substring(1) : "");
                Method m = InvoiceStDTO.class.getMethod(getterName);
                Object val = m.invoke(row);
                if (val != null) {
                    fieldValue = val.toString();
                }
            } catch (Exception e) {
                log.debug("createResultFromCache - cannot read value of field {} from row: {}",
                        fe.getValue(), e.getMessage());
            }
        }

        InvoiceCheckResultDTO dto = new InvoiceCheckResultDTO();
        if (fe != null) {
            dto.setFieldName(fe.getValue());
        }
        dto.setFieldValue(fieldValue);
        dto.setPassed(passed);
        dto.setCheckFailed(check.getName());
        dto.setControlId(check.getControlId());
        return dto;
    }

    private List<InvoiceCheckResultDTO> mergeByField(List<InvoiceCheckResultDTO> results) {
        Map<String, InvoiceCheckResultDTO> mergedResults = new LinkedHashMap<>();
        for (InvoiceCheckResultDTO result : results) {
            String key = result.getFieldName() != null ? result.getFieldName() : "__no_field__" + System.identityHashCode(result);
            InvoiceCheckResultDTO existing = mergedResults.get(key);
            if (existing == null) {
                mergedResults.put(key, result);
            } else if (result.getCheckFailed() != null) {
                existing.setCheckFailed(existing.getCheckFailed() == null
                        ? result.getCheckFailed()
                        : existing.getCheckFailed() + " - " + result.getCheckFailed());
            }
        }
        return new ArrayList<>(mergedResults.values());
    }

    private boolean allPassed(List<InvoiceCheckResultDTO> results) {
        return results.stream().allMatch(r -> Boolean.TRUE.equals(r.getPassed()));
    }

    private static final class AggregatedFieldResult {
        private final String fieldName;
        private boolean globalPassed = true;
        private int failedRowsCount = 0;
        private final long totalRows;
        private String passedCheckName;
        private String firstFailedCheckName;
        private final List<String> failedCheckNames = new ArrayList<>();
        private String firstFailedControlId;
        private final List<String> failedControlIds = new ArrayList<>();

        AggregatedFieldResult(InvoiceCheckResultDTO firstSeen, long totalRows) {
            this.fieldName = firstSeen.getFieldName();
            this.totalRows = totalRows;
        }

        void tickRow(InvoiceCheckResultDTO rowResult) {
            boolean rowPassed = Boolean.TRUE.equals(rowResult.getPassed());
            if (!rowPassed) {
                globalPassed = false;
                failedRowsCount++;
            }
            if (rowPassed && passedCheckName == null) {
                passedCheckName = rowResult.getCheckFailed();
            }
            if (!rowPassed) {
                if (firstFailedCheckName == null && rowResult.getCheckFailed() != null) {
                    firstFailedCheckName = rowResult.getCheckFailed();
                }
                if (rowResult.getCheckFailed() != null && !failedCheckNames.contains(rowResult.getCheckFailed())) {
                    failedCheckNames.add(rowResult.getCheckFailed());
                }
                String cid = rowResult.getControlId();
                if (firstFailedControlId == null && cid != null && !cid.isBlank()) {
                    firstFailedControlId = cid;
                }
                if (cid != null && !cid.isBlank() && !failedControlIds.contains(cid)) {
                    failedControlIds.add(cid);
                }
            }
        }

        InvoiceCheckResultDTO toDto() {
            InvoiceCheckResultDTO dto = new InvoiceCheckResultDTO();
            dto.setFieldName(fieldName);
            dto.setPassed(globalPassed);
            dto.setFieldValue(String.format("%d/%d righe fallite", failedRowsCount, totalRows));
            if (globalPassed) {
                dto.setCheckFailed(passedCheckName != null ? passedCheckName : "All rows passed");
            } else {
                String merged;
                if (failedCheckNames.size() <= 1) {
                    merged = firstFailedCheckName != null ? firstFailedCheckName : "Check failed";
                } else {
                    merged = String.join(" - ", failedCheckNames);
                }
                dto.setCheckFailed(merged);
                dto.setControlId(failedControlIds.size() == 1
                        ? firstFailedControlId
                        : (failedControlIds.isEmpty() ? null : String.join(" - ", failedControlIds)));
            }
            return dto;
        }
    }
}
