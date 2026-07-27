package com.skylogic.invoice.check;

import org.springframework.stereotype.Component;

import com.skylogic.invoice.dto.CheckCategoryEnum;
import com.skylogic.invoice.dto.FieldEnum;
import com.skylogic.invoice.dto.InvoiceCheckResultDTO;
import com.skylogic.invoice.dto.InvoiceStDTO;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
@Getter
@Setter
public class DummyCheck extends GenericCheck implements CheckI {

    @PostConstruct
    private void init() {
    	setName("Dummy");
    	setDescription("Dummy check");
    	setOrder(1);
    	setField(FieldEnum.invoiceNumber);
    	setCategory(CheckCategoryEnum.valueCheck);
    }

    @Override
    public InvoiceCheckResultDTO check(InvoiceStDTO row) {
    	
    	String fieldValue = row.getField1();
    	
    	try {
	    	log.debug("Check - START - name: {}, order: {}, field: {}", getName(), getOrder(), getField().getValue());
	    	
	    	/////////////////////////////////////////////////////
	    	/// Qui va implementata la logica del check
	    	/////////////////////////////////////////////////////
	    	
	    	/////////////////////////////////////////////////////
	    	/////////////////////////////////////////////////////
	    	    			    		
	    	// setta il risultato
	    	setPassed(false); // o true in base al risultato del check
	    	log.debug("Check - fieldName: " + getField().getValue() + " - fieldValue: " + fieldValue + " - passed: " + isPassed());   	
    	}
    	catch(Exception e) {
    		log.error("Check - ERROR - name: {}, order: {}, field: {}, error: {}", getName(), getOrder(), getField().getValue(), e.getMessage());
    		setPassed(false);     		
    	}
    	
    	log.info("Check - END - name: {} - passed: {}", getName(), isPassed());	
    	
    	// Crea il FieldDTO con il risultato del check
    	return createCheckResult(getField(), fieldValue);
    }
 
}
