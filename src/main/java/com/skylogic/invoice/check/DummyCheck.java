package com.skylogic.invoice.check;

import org.springframework.stereotype.Component;

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
    	setField(FieldEnum.Field1);
    }

    @Override
    public InvoiceCheckResultDTO check(InvoiceStDTO row) {
    	log.info("Check - START - name: {}, order: {}, field: {}", getName(), getOrder(), getField().getValue());
    	
    	/////////////////////////////////////////////////////
    	/// Qui va implementata la logica del check
    	/////////////////////////////////////////////////////
    	String fieldValue = row.getField1();
    	    			    		
    	// setta il risultato
    	setPassed(false); // o true in base al risultato del check
    	log.info("Check - fieldName: " + getField().getValue() + " - fieldValue: " + fieldValue + " - passed: " + isPassed());
    	
    	log.info("Check - END - paassed: {}", isPassed());
    	// Crea il FieldDTO con il risultato del check
		return createCheckResult(getField(), fieldValue);
    }
	
}
