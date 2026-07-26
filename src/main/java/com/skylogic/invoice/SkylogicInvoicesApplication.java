package com.skylogic.invoice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SkylogicInvoicesApplication {

	private static final Logger log = LoggerFactory.getLogger(SkylogicInvoicesApplication.class);
	
	public static void main(String[] args) {
		SpringApplication.run(SkylogicInvoicesApplication.class, args);
		log.info("SkylogicInvoicesApplication started");
	}

}
