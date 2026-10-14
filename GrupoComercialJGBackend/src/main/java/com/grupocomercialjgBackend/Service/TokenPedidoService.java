
package com.grupocomercialjgBackend.Service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class TokenPedidoService {

    private final SecureRandom secureRandom = new SecureRandom();

    // Generar token privado aleatorio
    public String generarToken() {
        byte[] bytes = new byte[32];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    // Guardar solamente el hash en la base de datos
    public String generarHash(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "El token es obligatorio"
            );
        }

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "No se pudo generar el hash",
                    e
            );
        }
    }
}
