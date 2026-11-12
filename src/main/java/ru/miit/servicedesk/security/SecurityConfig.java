package ru.miit.servicedesk.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/error", "/css/**", "/js/**", "/requests/**")
                        .permitAll()
                        .requestMatchers("/users/**", "/departments/*/head")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll())
                .logout(logout -> logout.permitAll());
        return http.build();
    }
}
