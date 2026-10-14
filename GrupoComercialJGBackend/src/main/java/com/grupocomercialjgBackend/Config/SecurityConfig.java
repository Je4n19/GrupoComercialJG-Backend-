
package com.grupocomercialjgBackend.Config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    // ========================================
    // CIFRADO DE CONTRASEÑAS
    // ========================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ========================================
    // AUTENTICACION
    // ========================================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // ========================================
    // SEGURIDAD DE ENDPOINTS
    // ========================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
            .cors(Customizer.withDefaults())

            // Mantener proteccion CSRF activa
            .csrf(csrf -> csrf
                .csrfTokenRepository(
                    new HttpSessionCsrfTokenRepository()
                )
            )

            .authorizeHttpRequests(auth -> auth

                // Token CSRF disponible sin login
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auth/csrf"
                ).permitAll()

                // Productos visibles para clientes
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/productos",
                    "/api/productos/**"
                ).permitAll()

                // Categorias visibles para clientes
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/categorias",
                    "/api/categorias/**"
                ).permitAll()

                // Repuestos visibles para clientes
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/repuestos",
                    "/api/repuestos/**"
                ).permitAll()

                // Categorias de repuestos publicas
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/categorias-repuesto",
                    "/api/categorias-repuesto/**"
                ).permitAll()

                // Login publico, con CSRF obligatorio
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/login"
                ).permitAll()

                // Consultar y cerrar sesion
                .requestMatchers(
                    "/api/auth/me",
                    "/api/auth/logout"
                ).hasRole("ADMIN")

                // Todas las demas operaciones API
                // requieren ser administrador
                .requestMatchers("/api/**")
                .hasRole("ADMIN")

                // Bloquear rutas no configuradas
                .anyRequest()
                .denyAll()
            )

            // No usar formulario predeterminado
            .formLogin(form -> form.disable())

            // No usar autenticacion HTTP Basic
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    // ========================================
    // CORS PARA REACT - LOCAL Y VERCEL
    // ========================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config =
                new CorsConfiguration();

        // Origenes autorizados
        config.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "https://grupo-comercial-jg-frontend-two.vercel.app"
        ));

        // Metodos permitidos por CORS
        config.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));

        // Cabeceras autorizadas
        config.setAllowedHeaders(List.of(
            "Content-Type",
            "X-CSRF-TOKEN"
        ));

        // Permitir envio de cookies de sesion
        config.setAllowCredentials(true);

        // Cache de solicitudes preflight
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/api/**",
            config
        );

        return source;
    }
}
