
package com.grupocomercialjgBackend.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import com.grupocomercialjgBackend.Model.ComprobantePago;
import com.grupocomercialjgBackend.Repository.ComprobantePagoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Service
public class ComprobantePagoService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final Cloudinary cloudinary;
    private final ComprobantePagoRepository repository;

    public ComprobantePagoService(
            Cloudinary cloudinary,
            ComprobantePagoRepository repository
    ) {
        this.cloudinary = cloudinary;
        this.repository = repository;
    }

    public boolean existe(Long pedidoId) {
        return repository.existsById(pedidoId);
    }

    public ComprobantePago guardar(
            Long pedidoId,
            MultipartFile archivo
    ) {
        if (pedidoId == null || pedidoId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Pedido inválido"
            );
        }

        if (repository.existsById(pedidoId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El pedido ya tiene un comprobante"
            );
        }

        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Adjunta una imagen del comprobante"
            );
        }

        if (archivo.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "El comprobante no puede superar 5 MB"
            );
        }

        byte[] contenido;

        try {
            contenido = archivo.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No se pudo leer el comprobante"
            );
        }

        String formato = detectarFormato(contenido);

        if (formato == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo se permiten imágenes JPG, PNG o WebP válidas"
            );
        }

        String publicId = "jg/comprobantes/"
                + UUID.randomUUID();

        boolean subido = false;

        try {
            Map<?, ?> resultado = cloudinary.uploader().upload(
                contenido,
                ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "image",
                    "type", "authenticated",
                    "overwrite", false
                )
            );

            Object idRespuesta = resultado.get("public_id");
            Object formatoRespuesta = resultado.get("format");

            if (idRespuesta == null || formatoRespuesta == null) {
                throw new IllegalStateException(
                    "Cloudinary no devolvió los datos del archivo"
                );
            }

            subido = true;

            ComprobantePago comprobante = new ComprobantePago();

            comprobante.setPedidoId(pedidoId);
            comprobante.setPublicId(idRespuesta.toString());
            comprobante.setFormato(formatoRespuesta.toString());
            comprobante.setNombreArchivo(
                limpiarNombre(archivo.getOriginalFilename())
            );
            comprobante.setFechaSubida(LocalDateTime.now());

            return repository.saveAndFlush(comprobante);

        } catch (Exception ex) {

            // Limpieza compensatoria si se subió la imagen
            // pero falló el registro de sus metadatos.
            if (subido) {
                eliminarDeCloudinary(publicId);
            }

            if (ex instanceof ResponseStatusException rse) {
                throw rse;
            }

            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "No se pudo almacenar el comprobante",
                ex
            );
        }
    }

    public record ImagenComprobante(
            byte[] contenido,
            String tipoContenido
    ) {}

    public ImagenComprobante obtenerImagen(Long pedidoId) {

        ComprobantePago comprobante = repository.findById(pedidoId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "El pedido no tiene comprobante"
            ));

        try {
            long vencimiento = java.time.Instant.now()
                    .plusSeconds(60)
                    .getEpochSecond();

            String url = cloudinary.privateDownload(
                comprobante.getPublicId(),
                comprobante.getFormato(),
                ObjectUtils.asMap(
                    "type", "authenticated",
                    "expires_at", vencimiento,
                    "attachment", false
                )
            );

            HttpClient cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

            HttpRequest solicitud = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

            HttpResponse<byte[]> respuesta = cliente.send(
                solicitud,
                HttpResponse.BodyHandlers.ofByteArray()
            );

            if (respuesta.statusCode() != 200) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloudinary no permitió recuperar la imagen"
                );
            }

            byte[] contenido = respuesta.body();

            if (contenido == null
                    || contenido.length == 0
                    || contenido.length > MAX_BYTES) {

                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El comprobante recibido no es válido"
                );
            }

            String formato = detectarFormato(contenido);

            if (formato == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloudinary no devolvió una imagen válida"
                );
            }

            String tipo = switch (formato) {
                case "jpg" -> "image/jpeg";
                case "png" -> "image/png";
                case "webp" -> "image/webp";
                default -> "application/octet-stream";
            };

            return new ImagenComprobante(contenido, tipo);

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "No se pudo recuperar el comprobante",
                ex
            );
        }
    }

    private void eliminarDeCloudinary(String publicId) {
        try {
            cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap(
                    "resource_type", "image",
                    "type", "authenticated",
                    "invalidate", true
                )
            );
        } catch (Exception ignored) {
            // Se debe revisar manualmente si queda un
            // archivo huérfano en Cloudinary.
        }
    }

    private String detectarFormato(byte[] datos) {

        if (datos == null) {
            return null;
        }

        // JPEG
        if (datos.length >= 4
                && (datos[0] & 0xFF) == 0xFF
                && (datos[1] & 0xFF) == 0xD8
                && (datos[2] & 0xFF) == 0xFF) {
            return "jpg";
        }

        // PNG
        byte[] firmaPng = new byte[] {
            (byte) 137, 80, 78, 71, 13, 10, 26, 10
        };

        if (datos.length >= 8
                && Arrays.equals(
                    Arrays.copyOf(datos, 8),
                    firmaPng
                )) {
            return "png";
        }

        // WebP
        if (datos.length >= 12
                && datos[0] == 'R'
                && datos[1] == 'I'
                && datos[2] == 'F'
                && datos[3] == 'F'
                && datos[8] == 'W'
                && datos[9] == 'E'
                && datos[10] == 'B'
                && datos[11] == 'P') {
            return "webp";
        }

        return null;
    }

    private String limpiarNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            return "comprobante";
        }

        String limpio = nombre.replace("\\", "/");

        limpio = limpio.substring(
            limpio.lastIndexOf('/') + 1
        );

        limpio = limpio.replaceAll(
            "[^A-Za-z0-9._-]", "_"
        );

        if (limpio.length() > 200) {
            limpio = limpio.substring(0, 200);
        }

        return limpio;
    }
}
