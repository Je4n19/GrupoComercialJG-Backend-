
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
    // ADMINISTRADOR DE AUTENTICACIÓN
    // ========================================

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // ========================================
    // CONFIGURACIÓN DE SEGURIDAD
    // ========================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
            // Habilitar CORS
            .cors(Customizer.withDefaults())

            // Mantener protección CSRF
            .csrf(csrf -> csrf
                .csrfTokenRepository(
                    new HttpSessionCsrfTokenRepository()
                )
            )

            // ========================================
            // PERMISOS DE ACCESO
            // ========================================

            .authorizeHttpRequests(auth -> auth

                // Obtener token CSRF públicamente
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auth/csrf"
                ).permitAll()

                // ========================================
                // CATÁLOGO PÚBLICO
                // ========================================

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

                // ========================================
                // LOGIN PÚBLICO
                // ========================================

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/login"
                ).permitAll()

                // ========================================
                // REGISTRO PÚBLICO DE PEDIDOS
                // ========================================

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/pedidos"
                ).permitAll()

                // ========================================
                // NUEVO: REPORTE DE PAGOS DEL CLIENTE
                // ========================================

                // Permite reportar una operación usando
                // el ID del pedido y su token privado.
                //
                // El controlador y servicio verifican
                // que el token corresponda al pedido.
                //
                // CSRF permanece obligatorio.

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/pagos/reportar"
                ).permitAll()

                // ========================================
                // GESTIÓN ADMINISTRATIVA DE PAGOS
                // ========================================

                // Solo ADMIN puede listar, aprobar
                // o rechazar los pagos.

                .requestMatchers(
                    "/api/admin/pagos",
                    "/api/admin/pagos/**"
                ).hasRole("ADMIN")

                // ========================================
                // SESIÓN ADMINISTRATIVA
                // ========================================

                .requestMatchers(
                    "/api/auth/me",
                    "/api/auth/logout"
                ).hasRole("ADMIN")

                // ========================================
                // CONSULTAS DE PEDIDOS - ADMIN
                // ========================================

                // La creación pública ya fue permitida
                // anteriormente con POST /api/pedidos.

                .requestMatchers(
                    "/api/pedidos",
                    "/api/pedidos/**"
                ).hasRole("ADMIN")

                // ========================================
                // RESTO DE LA API - ADMIN
                // ========================================

                .requestMatchers("/api/**")
                .hasRole("ADMIN")

                // Denegar cualquier otra ruta
                .anyRequest()
                .denyAll()
            )

            // No utilizar formulario de Spring Security
            .formLogin(form -> form.disable())

            // No utilizar autenticación HTTP Basic
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    // ========================================
    // CONFIGURACIÓN CORS
    // ========================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // Frontend local y frontend en Vercel
        config.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "https://grupo-comercial-jg-frontend-two.vercel.app"
        ));

        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));

        // Cabeceras permitidas
        config.setAllowedHeaders(List.of(
            "Content-Type",
            "X-CSRF-TOKEN"
        ));

        // Permitir cookies de sesión
        config.setAllowCredentials(true);

        // Tiempo de caché de preflight
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
