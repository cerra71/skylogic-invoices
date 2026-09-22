package com.skylogic.invoice.service;

import com.skylogic.invoice.advice.DuplicateControlIdException;
import com.skylogic.invoice.entity.KpiDocumentation;
import com.skylogic.invoice.repository.KpiDocumentationRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class KpiDocumentationService {

    @Autowired
    private KpiDocumentationRepository kpiDocumentationRepository;

    /**
     * Ricerca nella tabella kpi_documentation in base a kpiId (LIKE case-insensitive),
     * controlId (LIKE case-insensitive) e field (match esatto).
     */

    // READ
    public List<KpiDocumentation> searchKpiDocumentation(String kpiId, String controlId, String field) {
        log.info("searchKpiDocumentation - START - kpiId: {}, controlId: {}, field: {}", kpiId, controlId, field);
        List<KpiDocumentation> result = kpiDocumentationRepository.search(kpiId, controlId, field);
        log.info("searchKpiDocumentation - END - found {} record", result.size());
        return result;
    }

    // READ - Recupero di un singolo record tramite ID
    public Optional<KpiDocumentation> findById(Long id) {
        return kpiDocumentationRepository.findById(id);
    }

    /**
     * Salva un nuovo record nella tabella kpi_documentation.
     */
    // CREATE
    public KpiDocumentation saveKpiDocumentation(KpiDocumentation documentation) {

        log.info("saveKpiDocumentation - START - kpiId: {}, controlId: {}", documentation.getKpiId(), documentation.getControlId());

        if (kpiDocumentationRepository.existsByControlId(documentation.getControlId())) {
            throw new DuplicateControlIdException(documentation.getControlId());
        }

        if (kpiDocumentationRepository.existsByControlId(documentation.getControlId())) {
            throw new DuplicateControlIdException(documentation.getControlId());
        }


        KpiDocumentation result = kpiDocumentationRepository.save(documentation);

        log.info("saveKpiDocumentation - END - id: {}", result.getId());

        return result;
    }

    // DELETE
    public void deleteById(Long id) {
        kpiDocumentationRepository.deleteById(id);
    }
}