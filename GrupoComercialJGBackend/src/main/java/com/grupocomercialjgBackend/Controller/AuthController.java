
package com.grupocomercialjgBackend.Controller;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    // ========================================
    // DATOS RECIBIDOS DESDE REACT
    // ========================================

    public record LoginRequest(
            String username,
            String password
    ) {}

    // ========================================
    // INICIAR SESION
    // ========================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest datos,
            HttpServletRequest request
    ) {

        if (datos == null ||
                datos.username() == null ||
                datos.username().isBlank() ||
                datos.password() == null ||
                datos.password().isBlank()) {

            return ResponseEntity.badRequest().body(
                Map.of("mensaje", "Completa todos los campos")
            );
        }

        Authentication autenticacion =
                authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        datos.username().trim(),
                        datos.password()
                    )
                );

        // Crear una sesion si todavia no existe
        HttpSession session = request.getSession(true);

        // Cambiar el identificador para evitar
        // ataques de fijacion de sesion
        request.changeSessionId();

        // Crear el contexto de seguridad
        SecurityContext contexto =
                SecurityContextHolder.createEmptyContext();

        contexto.setAuthentication(autenticacion);

        SecurityContextHolder.setContext(contexto);

        // Guardar la autenticacion en la sesion
        session.setAttribute(
            HttpSessionSecurityContextRepository
                    .SPRING_SECURITY_CONTEXT_KEY,
            contexto
        );

        return ResponseEntity.ok(Map.of(
            "mensaje", "Inicio de sesion correcto",
            "usuario", autenticacion.getName(),
            "rol", "ADMIN"
        ));
    }

    // ========================================
    // VERIFICAR SESION
    // ========================================

    @GetMapping("/me")
    public ResponseEntity<?> verificar(
            Authentication autenticacion
    ) {

        if (autenticacion == null ||
                !autenticacion.isAuthenticated() ||
                autenticacion.getAuthorities().stream()
                    .noneMatch(autoridad ->
                        autoridad.getAuthority().equals("ROLE_ADMIN")
                    )) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                        "mensaje", "No autenticado"
                    ));
        }

        return ResponseEntity.ok(Map.of(
            "usuario", autenticacion.getName(),
            "rol", "ADMIN"
        ));
    }

    // ========================================
    // CERRAR SESION
    // ========================================

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletRequest request
    ) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(Map.of(
            "mensaje", "Sesion cerrada correctamente"
        ));
    }

    // ========================================
    // CREDENCIALES INCORRECTAS
    // ========================================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> credencialesIncorrectas() {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "mensaje", "Usuario o contraseña incorrectos"
                ));
    }
}
