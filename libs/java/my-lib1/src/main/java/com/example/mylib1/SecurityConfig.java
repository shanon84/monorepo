package com.example.mylib1;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Für APIs oft deaktiviert, bei Formularen evtl. aktiv lassen
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**").permitAll() // öffentlich zugänglich
                        .requestMatchers("/admin/**").hasRole("ADMIN") // nur ADMIN
                        .anyRequest().authenticated() // Rest erfordert Login
                )
                .formLogin(form -> form   // Standard Login-Formular aktivieren
                        .loginPage("/login")  // eigene Login-Seite (optional)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
