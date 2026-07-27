package com.skylogic.invoice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // LOGIN FORM-BASED CUSTOM


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**").permitAll()
                .anyRequest().authenticated()
                )
                .formLogin(form -> form
                .loginPage("/login") // pagina custom con le credenziali (GET)
                .loginProcessingUrl("/login") // URL che riceve le credenziali in  POST, intercettata da Spring Security
                .defaultSuccessUrl("/home", true) // redirect dopo login successo
                .failureUrl("/login?error=true") // redirect dopo login fallito
                .permitAll()
                )
                .logout(logout -> logout
                .logoutSuccessUrl("/login?logout=true") // redirect dopo logout 
                .permitAll()
                )
                .csrf(csrf -> csrf.disable());
                //Disabilita la protezione CSRF (Cross-Site Request Forgery).
                //Spring Security, di default, protegge le richieste POST/PUT/DELETE da attacchi CSRF.

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
