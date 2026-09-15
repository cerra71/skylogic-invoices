package com.skylogic.invoice.repository;

import com.skylogic.invoice.entity.KpiDocumentation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KpiDocumentationRepository extends JpaRepository<KpiDocumentation, Long> {

    /**
     * Ricerca combinata (LIKE case-insensitive per kpiId e controlId,
     * match esatto per field se valorizzato).
     */
    @Query(value = """
        SELECT * FROM public.kpi_documentation k
        WHERE (:kpiId IS NULL OR UPPER(k.kpi_id) LIKE UPPER(CONCAT('%', :kpiId, '%')))
          AND (:controlId IS NULL OR UPPER(k.control_id) LIKE UPPER(CONCAT('%', :controlId, '%')))
          AND (:field IS NULL OR :field = '' OR k.field = :field)
        ORDER BY k.kpi_id, k.control_id
        """, nativeQuery = true)
    List<KpiDocumentation> search(@Param("kpiId") String kpiId,
                                  @Param("controlId") String controlId,
                                  @Param("field") String field);
}
