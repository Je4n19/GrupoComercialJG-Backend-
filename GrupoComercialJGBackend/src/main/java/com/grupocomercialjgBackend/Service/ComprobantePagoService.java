
package com.grupocomercialjgBackend.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import com.grupocomercialjgBackend.Model.ComprobantePago;
import com.grupocomercialjgBackend.Repository.ComprobantePagoRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Service
public class ComprobantePagoService {

    private static final Logger log =
            LoggerFactory.getLogger(ComprobantePagoService.class);

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

    // ========================================
    // COMPROBAR EXISTENCIA
    // ========================================

    public boolean existe(Long pedidoId) {
        return repository.existsById(pedidoId);
    }

    // ========================================
    // GUARDAR COMPROBANTE EN CLOUDINARY
    // ========================================

    public ComprobantePago guardar(
            Long pedidoId,
            MultipartFile archivo
    ) {

        if (pedidoId == null || pedidoId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Pedido invalido"
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

            log.error(
                "Error leyendo comprobante del pedido {}",
                pedidoId,
                ex
            );

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "No se pudo leer el comprobante"
            );
        }

        String formato = detectarFormato(contenido);

        if (formato == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Solo se permiten imagenes JPG, PNG o WebP validas"
            );
        }

        String publicId = "jg/comprobantes/"
                + UUID.randomUUID();

        String publicIdSubido = null;

        // ========================================
        // SUBIR IMAGEN PRIVADA A CLOUDINARY
        // ========================================

        try {

            log.info(
                "Iniciando subida de comprobante. Pedido: {}, formato: {}, bytes: {}",
                pedidoId,
                formato,
                contenido.length
            );

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

            if (idRespuesta != null) {
                publicIdSubido = idRespuesta.toString();
            }

            Object formatoRespuesta = resultado.get("format");

            if (publicIdSubido == null
                    || publicIdSubido.isBlank()
                    || formatoRespuesta == null) {

                throw new IllegalStateException(
                    "Cloudinary no devolvio los metadatos esperados"
                );
            }

            log.info(
                "Cloudinary confirmo la subida del comprobante. Pedido: {}",
                pedidoId
            );

            // ========================================
            // GUARDAR METADATOS EN MYSQL
            // ========================================

            ComprobantePago comprobante = new ComprobantePago();

            comprobante.setPedidoId(pedidoId);
            comprobante.setPublicId(publicIdSubido);
            comprobante.setFormato(formatoRespuesta.toString());
            comprobante.setNombreArchivo(
                limpiarNombre(archivo.getOriginalFilename())
            );
            comprobante.setFechaSubida(LocalDateTime.now());

            ComprobantePago guardado =
                    repository.saveAndFlush(comprobante);

            log.info(
                "Comprobante registrado en MySQL. Pedido: {}",
                pedidoId
            );

            return guardado;

        } catch (Exception ex) {

            // No registrar credenciales, tokens ni URLs firmadas.
            log.error(
                "Fallo al almacenar comprobante. Pedido: {}, tipo: {}, mensaje: {}",
                pedidoId,
                ex.getClass().getSimpleName(),
                mensajeSeguro(ex)
            );

            // Limpieza compensatoria si Cloudinary confirmo
            // la subida, pero despues fallo MySQL.
            if (publicIdSubido != null) {
                eliminarDeCloudinary(publicIdSubido);
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

    // ========================================
    // RESPUESTA PARA VISUALIZAR COMPROBANTE
    // ========================================

    public record ImagenComprobante(
            byte[] contenido,
            String tipoContenido
    ) {}

    // ========================================
    // OBTENER COMPROBANTE PRIVADO
    // ========================================

    public ImagenComprobante obtenerImagen(Long pedidoId) {

        ComprobantePago comprobante = repository.findById(pedidoId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "El pedido no tiene comprobante"
            ));

        try {

            long vencimiento = Instant.now()
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

                log.error(
                    "Cloudinary devolvio HTTP {} al recuperar comprobante del pedido {}",
                    respuesta.statusCode(),
                    pedidoId
                );

                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloudinary no permitio recuperar la imagen"
                );
            }

            byte[] contenido = respuesta.body();

            if (contenido == null
                    || contenido.length == 0
                    || contenido.length > MAX_BYTES) {

                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "El comprobante recibido no es valido"
                );
            }

            String formato = detectarFormato(contenido);

            if (formato == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Cloudinary no devolvio una imagen valida"
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

            log.error(
                "Fallo al recuperar comprobante. Pedido: {}, tipo: {}, mensaje: {}",
                pedidoId,
                ex.getClass().getSimpleName(),
                mensajeSeguro(ex)
            );

            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "No se pudo recuperar el comprobante",
                ex
            );
        }
    }

    // ========================================
    // ELIMINAR IMAGEN DE CLOUDINARY
    // ========================================

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

            log.info(
                "Limpieza de comprobante solicitada a Cloudinary"
            );

        } catch (Exception ex) {

            log.error(
                "No se pudo limpiar el comprobante en Cloudinary. Tipo: {}",
                ex.getClass().getSimpleName()
            );
        }
    }

    // ========================================
    // IDENTIFICAR FORMATO REAL
    // ========================================

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

    // ========================================
    // LIMPIAR NOMBRE DEL ARCHIVO
    // ========================================

    private String limpiarNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            return "comprobante";
        }

        String limpio = nombre.replace("\\", "/");

        limpio = limpio.substring(
            limpio.lastIndexOf('/') + 1
        );

        limpio = limpio.replaceAll(
            "[^A-Za-z0-9._-]",
            "_"
        );

        if (limpio.length() > 200) {
            limpio = limpio.substring(0, 200);
        }

        return limpio;
    }

    // ========================================
    // MENSAJE DE DIAGNOSTICO SEGURO
    // ========================================

    private String mensajeSeguro(Exception ex) {

        if (ex instanceof ResponseStatusException rse) {
            return rse.getStatusCode().toString();
        }

        // No exponer mensajes de proveedores externos:
        // podrian contener URLs, tokens o credenciales.
        return "Consultar el tipo de excepcion";
    }
}
