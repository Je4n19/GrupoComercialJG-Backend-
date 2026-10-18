
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
    // ENCRIPTACION DE CONTRASEÑAS
    // ========================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ========================================
    // ADMINISTRADOR DE AUTENTICACION
    // ========================================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }

    // ========================================
    // CONFIGURACION DE SPRING SECURITY
    // ========================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

            // Habilitar CORS
            .cors(Customizer.withDefaults())

            // ========================================
            // PROTECCION CSRF
            // ========================================

            .csrf(csrf -> csrf

                // Guardar el token CSRF en la sesion
                .csrfTokenRepository(
                    new HttpSessionCsrfTokenRepository()
                )

                // Procesar el token enviado por React
                // mediante X-CSRF-TOKEN
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
                // PERMITIR PROCESAMIENTO DE ERRORES
                // ====================================

                .requestMatchers(
                    "/error"
                ).permitAll()

                // ====================================
                // OBTENER TOKEN CSRF
                // ====================================

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auth/csrf"
                ).permitAll()

                // ====================================
                // CATALOGO PUBLICO
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
                // LOGIN PUBLICO
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
                // COMPATIBILIDAD
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
                // ADMINISTRACION DE PAGOS
                // SOLO ADMINISTRADORES
                // ====================================

                .requestMatchers(
                    "/api/admin/pagos",
                    "/api/admin/pagos/**"
                ).hasRole("ADMIN")

                // ====================================
                // SESION ADMINISTRATIVA
                // ====================================

                .requestMatchers(
                    "/api/auth/me",
                    "/api/auth/logout"
                ).hasRole("ADMIN")

                // ====================================
                // GESTION DE PEDIDOS
                // POST /api/pedidos YA ES PUBLICO
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

                // ====================================
                // DENEGAR OTRAS RUTAS
                // ====================================

                .anyRequest()
                .denyAll()
            )

            // ========================================
            // DESACTIVAR LOGIN HTML DE SPRING
            // ========================================

            .formLogin(form -> form.disable())

            // ========================================
            // DESACTIVAR HTTP BASIC
            // ========================================

            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    // ========================================
    // CONFIGURACION CORS
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
        // METODOS HTTP PERMITIDOS
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
        // ENCABEZADOS EXPUESTOS A REACT
        // ====================================

        config.setExposedHeaders(List.of(
            "X-CSRF-TOKEN",
            "X-XSRF-TOKEN"
        ));

        // ====================================
        // PERMITIR COOKIES DE SESION
        // ====================================

        config.setAllowCredentials(true);

        // Cache del preflight: 1 hora
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
