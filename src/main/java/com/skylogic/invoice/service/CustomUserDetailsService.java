package com.skylogic.invoice.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

// Servizio personalizzato per caricare i dettagli dell'utente da database nell'auth form
@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {
	
	@Value("${invoices.login.username}")
    private String username;

    @Value("${invoices.login.password}")
    private String password;

    /**
     * Carica i dettagli dell'utente richiesti per l'autenticazione.
     * In caso di utente non trovato, lancia una {@link UsernameNotFoundException}.
     *
     * @param username il nome dell'utente in fase di login
     * @return un'astrazione {@link UserDetails} per l'uso interno alla struttura di sicurezza
     * @throws UsernameNotFoundException se le credenziali ricercate non restituiscono alcun record
     */
    @Override
    public UserDetails loadUserByUsername(String user) throws UsernameNotFoundException {
        log.info("loadUserByUsername - START: Username: {}", user);
        
        log.info("loadUserByUsername - START: usr salvato: {}", username);
        log.info("loadUserByUsername - START: pwd salvata: {}", password);
        
        if(!user.equalsIgnoreCase(username))
        	throw new UsernameNotFoundException("Utente non trovato: " + user);
        
        log.info("loadUserByUsername - Utente trovato: {}", user);
        log.info("loadUserByUsername - END");

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        return new org.springframework.security.core.userdetails.User(
        		username,
        		password,
                true,
                true,
                true,
                true,
                authorities
        );
    }
}
