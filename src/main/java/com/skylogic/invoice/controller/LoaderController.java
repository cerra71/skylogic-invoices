package com.skylogic.invoice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.skylogic.invoice.service.LoaderService;

import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

@Slf4j
@Controller
public class LoaderController extends GenericController {

	@Autowired
	private LoaderService loaderService;

    /**
     * Riceve il file caricato da form multipart e lo passa al {@link LoaderService}.
     * Riconosce automaticamente l'estensione:
     * <ul>
     *   <li>{@code .csv} → {@link LoaderService#loadCsv(MultipartFile)}</li>
     *   <li>{@code .zip} → {@link LoaderService#loadZip(MultipartFile)}</li>
     * </ul>
     *
     * @return redirect verso la pagina {@code home}
     */
    @PostMapping("/loadCsv")
    public String loadCsv(@RequestParam("file") MultipartFile file) {
        log.info("loadCsv - START: file: {}", file.getOriginalFilename());

        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            loaderService.loadZip(file);
        } else {
            loaderService.loadCsv(file);
        }

        log.info("loadCsv - END");
        return "redirect:/home";
    }

}
