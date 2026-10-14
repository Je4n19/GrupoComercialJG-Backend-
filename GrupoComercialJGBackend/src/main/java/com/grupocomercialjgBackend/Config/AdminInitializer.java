package com.grupocomercialjgBackend.Config;
import com.grupocomercialjgBackend.Model.Usuario;
import com.grupocomercialjgBackend.Repository.UsuarioRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    @Bean
    public CommandLineRunner crearAdministrador(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {

            String username = System.getenv("ADMIN_USERNAME");
            String password = System.getenv("ADMIN_PASSWORD");

            if (username == null || username.isBlank()
                    || password == null || password.isBlank()) {

                System.out.println(
                    "ADMIN: faltan las variables ADMIN_USERNAME y ADMIN_PASSWORD"
                );
                return;
            }

            if (password.length() < 12) {
                throw new IllegalStateException(
                    "La contraseña ADMIN debe tener al menos 12 caracteres"
                );
            }

            if (usuarioRepository.existsByUsername(username)) {
                System.out.println(
                    "ADMIN: la cuenta ya existe. No se modificó."
                );
                return;
            }

            // Evitar crear otra cuenta si ya existe un administrador
            if (usuarioRepository.count() > 0) {
                System.out.println(
                    "ADMIN: ya existen usuarios. No se creó otra cuenta."
                );
                return;
            }

            Usuario admin = new Usuario();

            admin.setUsername(username);
            admin.setNombre("Administrador J&G");
            admin.setPassword(passwordEncoder.encode(password));

            usuarioRepository.save(admin);

            System.out.println(
                "ADMIN: cuenta creada correctamente con BCrypt."
            );
        };
    }
}
