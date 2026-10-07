package com.skylogic.invoice.repository;

import com.skylogic.invoice.entity.Usage;
import com.skylogic.invoice.entity.UsageId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageRepository extends JpaRepository<Usage, UsageId> {

}