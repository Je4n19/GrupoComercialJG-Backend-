
package com.grupocomercialjgBackend.Service;

import com.grupocomercialjgBackend.DTO.ReclamacionRequestDTO;
import com.grupocomercialjgBackend.DTO.ReclamacionResponseDTO;
import com.grupocomercialjgBackend.Model.Reclamacion;
import com.grupocomercialjgBackend.Repository.ReclamacionRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ReclamacionService {

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private static final Set<String> DOCUMENTOS =
            Set.of("DNI", "CE", "PASAPORTE", "OTRO");

    private static final Set<String> TIPOS_BIEN =
            Set.of("PRODUCTO", "SERVICIO");

    private static final Set<String> TIPOS_RECLAMACION =
            Set.of("RECLAMO", "QUEJA");

    private static final Set<String> ESTADOS =
            Set.of(
                    "PENDIENTE",
                    "EN_REVISION",
                    "RESPONDIDO"
            );

    private final ReclamacionRepository repository;

    public ReclamacionService(
            ReclamacionRepository repository
    ) {
        this.repository = repository;
    }

    // ========================================
    // REGISTRAR RECLAMACION
    // ========================================

    @Transactional
    public ReclamacionResponseDTO registrar(
            ReclamacionRequestDTO request
    ) {

        if (request == null) {
            throw error("Los datos son obligatorios");
        }

        // VALIDAR DATOS DEL CONSUMIDOR

        String nombres = obligatorio(
                request.nombres(), "Nombres", 100
        );

        String apellidos = obligatorio(
                request.apellidos(), "Apellidos", 100
        );

        String tipoDocumento = opcion(
                request.tipoDocumento(),
                DOCUMENTOS,
                "Tipo de documento"
        );

        String numeroDocumento = obligatorio(
                request.numeroDocumento(),
                "Numero de documento",
                20
        );

        if ("DNI".equals(tipoDocumento)
                && !numeroDocumento.matches("\\d{8}")) {
            throw error("El DNI debe tener 8 digitos");
        }

        String domicilio = obligatorio(
                request.domicilio(), "Domicilio", 250
        );

        String telefono = obligatorio(
                request.telefono(), "Telefono", 20
        );

        String correo = correoObligatorio(
                request.correo()
        );

        // VALIDAR REPRESENTANTE SI ES MENOR

        String representante = opcional(
                request.representante(), 200
        );

        String domicilioRepresentante = opcional(
                request.domicilioRepresentante(), 250
        );

        String telefonoRepresentante = opcional(
                request.telefonoRepresentante(), 20
        );

        String correoRepresentante = opcional(
                request.correoRepresentante(), 150
        );

        if (request.menorEdad()) {

            if (representante == null
                    || domicilioRepresentante == null
                    || telefonoRepresentante == null
                    || correoRepresentante == null) {

                throw error(
                        "Completa los datos del representante"
                );
            }

            validarCorreo(correoRepresentante);
        } else {
            representante = null;
            domicilioRepresentante = null;
            telefonoRepresentante = null;
            correoRepresentante = null;
        }

        // VALIDAR PRODUCTO O SERVICIO

        String tipoBien = opcion(
                request.tipoBien(),
                TIPOS_BIEN,
                "Tipo de bien"
        );

        String descripcionBien = obligatorio(
                request.descripcionBien(),
                "Descripcion del bien",
                500
        );

        BigDecimal monto = request.monto();

        if (monto != null) {

            if (monto.signum() < 0
                    || monto.scale() > 2
                    || monto.precision() > 12) {

                throw error("El monto no es valido");
            }
        }

        String numeroPedido = opcional(
                request.numeroPedido(), 60
        );

        // VALIDAR RECLAMO O QUEJA

        String tipo = opcion(
                request.tipo(),
                TIPOS_RECLAMACION,
                "Tipo de reclamacion"
        );

        String detalle = obligatorio(
                request.detalle(),
                "Detalle de la reclamacion",
                4000
        );

        if (detalle.length() < 10) {
            throw error(
                    "El detalle debe tener al menos 10 caracteres"
            );
        }

        String pedidoConsumidor = obligatorio(
                request.pedido(),
                "Pedido del consumidor",
                2000
        );

        if (pedidoConsumidor.length() < 5) {
            throw error(
                    "El pedido debe tener al menos 5 caracteres"
            );
        }

        // ========================================
        // CREAR ENTIDAD
        // ========================================

        Reclamacion reclamacion = new Reclamacion();

        reclamacion.setNombres(nombres);
        reclamacion.setApellidos(apellidos);
        reclamacion.setTipoDocumento(tipoDocumento);
        reclamacion.setNumeroDocumento(numeroDocumento);
        reclamacion.setDomicilio(domicilio);
        reclamacion.setTelefono(telefono);
        reclamacion.setCorreo(correo);

        reclamacion.setMenorEdad(request.menorEdad());
        reclamacion.setRepresentante(representante);
        reclamacion.setDomicilioRepresentante(
                domicilioRepresentante
        );
        reclamacion.setTelefonoRepresentante(
                telefonoRepresentante
        );
        reclamacion.setCorreoRepresentante(
                correoRepresentante
        );

        reclamacion.setTipoBien(tipoBien);
        reclamacion.setDescripcionBien(descripcionBien);
        reclamacion.setMonto(monto);
        reclamacion.setNumeroPedido(numeroPedido);

        reclamacion.setTipo(tipo);
        reclamacion.setDetalle(detalle);
        reclamacion.setPedidoConsumidor(
                pedidoConsumidor
        );

        reclamacion.setEstado("PENDIENTE");

        // ========================================
        // GENERAR CODIGO PRIVADO
        // ========================================

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);

        String codigoSeguimiento =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(bytes);

        reclamacion.setCodigoSeguimientoHash(
                generarHash(codigoSeguimiento)
        );

        // ========================================
        // GUARDAR EN MYSQL
        // ========================================

        Reclamacion guardada =
                repository.saveAndFlush(reclamacion);

        // El ID de MySQL es unico incluso
        // cuando existen registros simultaneos.

        String numeroHoja = String.format(
                Locale.ROOT,
                "LR-%010d",
                guardada.getId()
        );

        guardada.setNumeroHoja(numeroHoja);

        repository.saveAndFlush(guardada);

        // ========================================
        // RESPUESTA AL CONSUMIDOR
        // ========================================

        return new ReclamacionResponseDTO(
                guardada.getNumeroHoja(),
                codigoSeguimiento,
                guardada.getFechaRegistro(),
                guardada.getTipo(),
                guardada.getEstado(),
                "Tu reclamacion fue registrada correctamente."
        );
    }

    // ========================================
    // LISTAR RECLAMACIONES - ADMIN
    // ========================================

    @Transactional(readOnly = true)
    public List<Reclamacion> listar() {
        return repository
                .findAllByOrderByFechaRegistroDesc();
    }

    // ========================================
    // FILTRAR POR ESTADO - ADMIN
    // ========================================

    @Transactional(readOnly = true)
    public List<Reclamacion> listarPorEstado(
            String estado
    ) {

        String estadoValidado = opcion(
                estado,
                ESTADOS,
                "Estado"
        );

        return repository
                .findByEstadoOrderByFechaRegistroDesc(
                        estadoValidado
                );
    }

    // ========================================
    // BUSCAR POR ID - ADMIN
    // ========================================

    @Transactional(readOnly = true)
    public Reclamacion obtenerPorId(Long id) {

        return repository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Reclamacion no encontrada"
                        )
                );
    }

    // ========================================
    // REGISTRAR RESPUESTA - ADMIN
    // ========================================

    @Transactional
    public Reclamacion responder(
            Long id,
            String respuesta
    ) {

        Reclamacion reclamacion = obtenerPorId(id);

        String respuestaValidada = obligatorio(
                respuesta,
                "Respuesta del proveedor",
                4000
        );

        reclamacion.setRespuestaProveedor(
                respuestaValidada
        );

        reclamacion.setEstado("RESPONDIDO");

        reclamacion.setFechaRespuesta(
                LocalDateTime.now()
        );

        return repository.save(reclamacion);
    }

    // ========================================
    // VALIDACIONES
    // ========================================

    private String obligatorio(
            String valor,
            String campo,
            int maximo
    ) {

        if (valor == null || valor.isBlank()) {
            throw error(
                    "El campo " + campo + " es obligatorio"
            );
        }

        String limpio = valor.trim();

        if (limpio.length() > maximo) {
            throw error(
                    "El campo " + campo + " es demasiado largo"
            );
        }

        return limpio;
    }

    private String opcional(
            String valor,
            int maximo
    ) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        String limpio = valor.trim();

        if (limpio.length() > maximo) {
            throw error(
                    "Uno de los campos supera la longitud permitida"
            );
        }

        return limpio;
    }

    private String opcion(
            String valor,
            Set<String> permitidos,
            String campo
    ) {

        if (valor == null) {
            throw error("El campo " + campo + " es obligatorio");
        }

        String normalizado =
                valor.trim().toUpperCase(Locale.ROOT);

        if (!permitidos.contains(normalizado)) {
            throw error(
                    "El valor de " + campo + " no es valido"
            );
        }

        return normalizado;
    }

    private String correoObligatorio(
            String valor
    ) {

        String correo = obligatorio(
                valor,
                "Correo electronico",
                150
        );

        validarCorreo(correo);

        return correo;
    }

    private void validarCorreo(String correo) {

        if (!correo.matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
        )) {
            throw error(
                    "El correo electronico no es valido"
            );
        }
    }

    private String generarHash(String valor) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    valor.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "No se pudo generar el hash",
                    e
            );
        }
    }

    private ResponseStatusException error(
            String mensaje
    ) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                mensaje
        );
    }
}
