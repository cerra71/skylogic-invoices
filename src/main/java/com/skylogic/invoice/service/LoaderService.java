package com.skylogic.invoice.service;

import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Validated
public class LoaderService {

    // Da implementare: carica il file CSV nel DB in staging
    public void loadCsv(MultipartFile file) {
        log.info("loadCsv - START: file: {}", file.getOriginalFilename());

        log.info("loadCsv - END");
    }

}
