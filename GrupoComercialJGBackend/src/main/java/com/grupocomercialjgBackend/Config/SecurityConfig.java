
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
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    // ========================================
    // ENCRIPTACIÓN DE CONTRASEÑAS
    // ========================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ========================================
    // ADMINISTRADOR DE AUTENTICACIÓN
    // ========================================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }

    // ========================================
    // CONFIGURACIÓN DE SPRING SECURITY
    // ========================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

            // Habilitar CORS
            .cors(Customizer.withDefaults())

            // ========================================
            // PROTECCIÓN CSRF
            // ========================================

            .csrf(csrf -> csrf

                // Guardar el token CSRF en la sesión
                .csrfTokenRepository(
                    new HttpSessionCsrfTokenRepository()
                )

                // Procesar el token enviado por React
                // en el encabezado X-CSRF-TOKEN
                .csrfTokenRequestHandler(
                    new CsrfTokenRequestAttributeHandler()
                )
            )

            // ========================================
            // PERMISOS DE ENDPOINTS
            // ========================================

            .authorizeHttpRequests(auth -> auth

                // ====================================
                // PETICIONES CORS PREFLIGHT
                // ====================================

                .requestMatchers(
                    HttpMethod.OPTIONS,
                    "/api/**"
                ).permitAll()

                // ====================================
                // OBTENER TOKEN CSRF
                // ====================================

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auth/csrf"
                ).permitAll()

                // ====================================
                // CATÁLOGO PÚBLICO
                // ====================================

                .requestMatchers(
                    HttpMethod.GET,

                    "/api/productos",
                    "/api/productos/**",

                    "/api/categorias",
                    "/api/categorias/**",

                    "/api/repuestos",
                    "/api/repuestos/**",

                    "/api/categorias-repuesto",
                    "/api/categorias-repuesto/**"

                ).permitAll()

                // ====================================
                // LOGIN PÚBLICO
                // ====================================

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/login"
                ).permitAll()

                // ====================================
                // REGISTRAR PEDIDOS
                // ====================================

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/pedidos"
                ).permitAll()

                // ====================================
                // REPORTAR PAGO SIN COMPROBANTE
                // COMPATIBILIDAD CON VERSIÓN ANTERIOR
                // ====================================

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/pagos/reportar"
                ).permitAll()

                // ====================================
                // REPORTAR PAGO CON COMPROBANTE
                // CLOUDINARY
                // ====================================

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/pagos/reportar-comprobante"
                ).permitAll()

                // ====================================
                // ADMINISTRACIÓN DE PAGOS
                // SOLO ADMINISTRADORES
                // ====================================

                .requestMatchers(
                    "/api/admin/pagos",
                    "/api/admin/pagos/**"
                ).hasRole("ADMIN")

                // ====================================
                // SESIÓN ADMINISTRATIVA
                // ====================================

                .requestMatchers(
                    "/api/auth/me",
                    "/api/auth/logout"
                ).hasRole("ADMIN")

                // ====================================
                // GESTIÓN DE PEDIDOS
                // POST /api/pedidos YA ES PÚBLICO
                // ====================================

                .requestMatchers(
                    "/api/pedidos",
                    "/api/pedidos/**"
                ).hasRole("ADMIN")

                // ====================================
                // RESTO DE API PROTEGIDA
                // ====================================

                .requestMatchers(
                    "/api/**"
                ).hasRole("ADMIN")

                // DENEGAR RUTAS NO AUTORIZADAS
                .anyRequest()
                .denyAll()
            )

            // ========================================
            // DESACTIVAR LOGIN HTML DE SPRING
            // ========================================

            .formLogin(form -> form.disable())

            // DESACTIVAR HTTP BASIC
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    // ========================================
    // CONFIGURACIÓN CORS
    // ========================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // ====================================
        // DOMINIOS PERMITIDOS
        // ====================================

        config.setAllowedOrigins(List.of(

            // Desarrollo local React + Vite
            "http://localhost:5173",

            // Frontend principal en Vercel
            "https://grupocomercialjg.vercel.app",

            // Frontend alternativo
            "https://grupo-comercial-jg-frontend-two.vercel.app"

        ));

        // ====================================
        // MÉTODOS HTTP PERMITIDOS
        // ====================================

        config.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));

        // ====================================
        // ENCABEZADOS PERMITIDOS
        // ====================================

        config.setAllowedHeaders(List.of(
            "Content-Type",
            "X-CSRF-TOKEN",
            "X-XSRF-TOKEN",
            "Authorization",
            "Accept"
        ));

        // ====================================
        // ENCABEZADOS VISIBLES EN REACT
        // ====================================

        config.setExposedHeaders(List.of(
            "X-CSRF-TOKEN",
            "X-XSRF-TOKEN"
        ));

        // ====================================
        // PERMITIR COOKIES DE SESIÓN
        // ====================================

        config.setAllowCredentials(true);

        // Caché del preflight: 1 hora
        config.setMaxAge(3600L);

        // ====================================
        // APLICAR CORS A LA API
        // ====================================

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/api/**",
            config
        );

        return source;
    }
}
