
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
            .cors(Customizer.withDefaults())

            // Mantener CSRF activo
            .csrf(csrf -> csrf
                .csrfTokenRepository(
                    new HttpSessionCsrfTokenRepository()
                )
            )

            .authorizeHttpRequests(auth -> auth

                // CORS preflight
                .requestMatchers(
                    HttpMethod.OPTIONS,
                    "/api/**"
                ).permitAll()

                // Token CSRF
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auth/csrf"
                ).permitAll()

                // Catálogo público
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

                // Login público
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/login"
                ).permitAll()

                // Registrar pedido
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/pedidos"
                ).permitAll()

                // Reportar pago JSON (compatibilidad)
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/pagos/reportar"
                ).permitAll()

                // Nuevo: subir comprobante de pago
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/pagos/reportar-comprobante"
                ).permitAll()

                // Gestión administrativa de pagos,
                // incluida visualización de comprobantes
                .requestMatchers(
                    "/api/admin/pagos",
                    "/api/admin/pagos/**"
                ).hasRole("ADMIN")

                // Sesión administrativa
                .requestMatchers(
                    "/api/auth/me",
                    "/api/auth/logout"
                ).hasRole("ADMIN")

                // Pedidos: POST público ya permitido
                .requestMatchers(
                    "/api/pedidos",
                    "/api/pedidos/**"
                ).hasRole("ADMIN")

                // Resto de API protegida
                .requestMatchers("/api/**")
                .hasRole("ADMIN")

                .anyRequest()
                .denyAll()
            )

            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "https://grupocomercialjg.vercel.app",
            "https://grupo-comercial-jg-frontend-two.vercel.app"
        ));

        config.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
            "Content-Type",
            "X-CSRF-TOKEN",
            "X-XSRF-TOKEN",
            "Authorization",
            "Accept"
        ));

        config.setExposedHeaders(List.of(
            "X-CSRF-TOKEN",
            "X-XSRF-TOKEN"
        ));

        config.setAllowCredentials(true);
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
