package com.nhom12.doangiaohang.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Autowired private CustomLoginSuccessHandler loginSuccessHandler;
    @Autowired private CustomLoginFailureHandler loginFailureHandler; 
    @Autowired private CustomLogoutHandler logoutHandler;
    
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() { return new HttpSessionEventPublisher(); }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((requests) -> requests
                .requestMatchers("/css/**", "/js/**", "/register", "/register-admin", "/forgot-password", "/reset-password", "/login", "/tra-cuu/**", "/setup", "/setup-success").permitAll()
                .requestMatchers("/quan-ly/**").hasRole("QUANLY")
                .requestMatchers("/shipper/**").hasRole("SHIPPER")
                .requestMatchers("/khach-hang/**").hasRole("KHACHHANG")
                .anyRequest().authenticated() 
            )
            .formLogin((form) -> form
                .loginPage("/login")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler) 
                .permitAll()
            )
            .logout((logout) -> logout
                .addLogoutHandler(logoutHandler)
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(10) 
                .sessionRegistry(sessionRegistry()) 
            )
            .exceptionHandling((exceptions) -> exceptions.accessDeniedPage("/access-denied"));

        return http.build();
    }
}