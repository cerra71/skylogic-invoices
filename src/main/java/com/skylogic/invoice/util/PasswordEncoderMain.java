package com.skylogic.invoice.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderMain {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "sky01!";
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println("Password cifrata: " + encodedPassword);
    }
}
