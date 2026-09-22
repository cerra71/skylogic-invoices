package com.skylogic.invoice.advice;

public class DuplicateKpiIdException extends RuntimeException {

  public DuplicateKpiIdException(String kpiId) {
    super("KPI ID already exists: " + kpiId);
  }
}
