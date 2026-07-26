package com.skylogic.invoice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Controller
public class LoginController extends GenericController {
	
	@Autowired
	private GuiController guiController;

    /**
     * Gestisce la root dell'applicazione.
     * <ul>
     *   <li>Ritorna il template della pagina di login</li>
     * </ul>
     *
     * @return il nome del template {@code login}
     */
    @GetMapping("/")
    public String root() {
        return "login"; // templates/login.html
    }

    /**
     * Rimanda alla route di login.
     * <ul>
     *   <li>Indirizza al form di login.</li>
     * </ul>
     *
     * @return il nome del template {@code login}
     */
    @GetMapping("/login")
    public String login() {
        return "login"; // templates/login.html
    }

}
