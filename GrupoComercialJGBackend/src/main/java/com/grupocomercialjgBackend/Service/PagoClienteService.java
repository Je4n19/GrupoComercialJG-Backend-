
package com.grupocomercialjgBackend.Service;

import com.grupocomercialjgBackend.Model.Pedido;
import com.grupocomercialjgBackend.Repository.PedidoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
public class PagoClienteService {

    private static final Set<String> METODOS_PERMITIDOS =
            Set.of("YAPE", "PLIN", "BCP", "BBVA");

    private static final Set<String> METODOS_ENTREGA =
            Set.of("RECOJO", "SHALOM", "EMTRAFESA", "OLVA");

    private final PedidoRepository pedidoRepository;
    private final TokenPedidoService tokenPedidoService;
    private final ComprobantePagoService comprobantePagoService;

    public PagoClienteService(
            PedidoRepository pedidoRepository,
            TokenPedidoService tokenPedidoService,
            ComprobantePagoService comprobantePagoService
    ) {
        this.pedidoRepository = pedidoRepository;
        this.tokenPedidoService = tokenPedidoService;
        this.comprobantePagoService = comprobantePagoService;
    }

    // ========================================
    // REPORTE ORIGINAL: COMPATIBILIDAD JSON
    // ========================================

    @Transactional
    public Pedido reportarPago(
            Long pedidoId,
            String tokenSeguimiento,
            String numeroOperacion
    ) {
        return registrarPago(
            pedidoId,
            tokenSeguimiento,
            numeroOperacion,
            null
        );
    }

    // ========================================
    // NUEVO REPORTE CON COMPROBANTE
    // ========================================

    @Transactional
    public Pedido reportarPagoConComprobante(
            Long pedidoId,
            String tokenSeguimiento,
            String numeroOperacion,
            MultipartFile comprobante
    ) {

        if (comprobante == null || comprobante.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Debes adjuntar el comprobante de pago"
            );
        }

        return registrarPago(
            pedidoId,
            tokenSeguimiento,
            numeroOperacion,
            comprobante
        );
    }

    // ========================================
    // VALIDAR Y REGISTRAR PAGO
    // ========================================

    private Pedido registrarPago(
            Long pedidoId,
            String tokenSeguimiento,
            String numeroOperacion,
            MultipartFile comprobante
    ) {

        // IDENTIFICADOR
        if (pedidoId == null || pedidoId <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Pedido inválido"
            );
        }

        // TOKEN PRIVADO
        if (tokenSeguimiento == null
                || tokenSeguimiento.isBlank()) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El token de seguimiento es obligatorio"
            );
        }

        // NUMERO DE OPERACION
        if (numeroOperacion == null
                || numeroOperacion.trim().isEmpty()) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Ingresa el número de operación"
            );
        }

        String operacion = numeroOperacion.trim();

        if (!operacion.matches("[A-Za-z0-9-]{4,50}")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El número de operación debe tener entre "
                + "4 y 50 caracteres, usando letras, "
                + "números o guiones"
            );
        }

        // VERIFICAR ACCESO AL PEDIDO
        String hash = tokenPedidoService.generarHash(
            tokenSeguimiento
        );

        Pedido pedido = pedidoRepository
            .findByTokenSeguimientoHash(hash)
            .filter(p -> p.getId().equals(pedidoId))
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Pedido no encontrado o acceso inválido"
            ));

        // VALIDAR METODO DE PAGO
        String metodoPago = normalizar(
            pedido.getMetodoPago()
        );

        if (!METODOS_PERMITIDOS.contains(metodoPago)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El método de pago no admite reporte manual"
            );
        }

        // VALIDAR METODO DE ENTREGA
        String metodoEntrega = normalizar(
            pedido.getMetodoEntrega()
        );

        if (!METODOS_ENTREGA.contains(metodoEntrega)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El método de entrega del pedido no es válido"
            );
        }

        // VALIDAR PEDIDO CANCELADO
        if ("CANCELADO".equals(
                normalizar(pedido.getEstadoPedido())
        )) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "No se puede reportar un pago "
                + "de un pedido cancelado"
            );
        }

        // VALIDAR ESTADO DEL PAGO
        String estadoPago = normalizar(
            pedido.getEstadoPago()
        );

        if (!Set.of("PENDIENTE", "RECHAZADO")
                .contains(estadoPago)) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Este pedido ya tiene un pago "
                + "reportado o verificado"
            );
        }

        // FLETE CONTRA ENTREGA
        if (!"RECOJO".equals(metodoEntrega)) {

            String modalidadFlete = normalizar(
                pedido.getModalidadPagoFlete()
            );

            if (!modalidadFlete.isEmpty()
                    && !"CONTRA_ENTREGA".equals(
                        modalidadFlete
                    )) {

                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La modalidad de pago del flete "
                    + "no es válida"
                );
            }
        }

        // ========================================
        // GUARDAR COMPROBANTE, SI FUE ADJUNTADO
        // ========================================

        if (comprobante != null) {
            comprobantePagoService.guardar(
                pedido.getId(),
                comprobante
            );
        }

        // ========================================
        // REGISTRAR REPORTE DE PAGO
        // ========================================

        pedido.setNumeroOperacionPago(operacion);
        pedido.setReferenciaPago(operacion);

        // Reportar NO aprueba el pago.
        pedido.setEstadoPago("EN_REVISION");

        pedido.setFechaReportePago(
            LocalDateTime.now()
        );

        pedido.setFechaVerificacionPago(null);
        pedido.setFechaPago(null);
        pedido.setObservacionPago(null);

        // No modificar subtotal, envío ni total.
        // El stock se descuenta únicamente
        // cuando el administrador aprueba.

        return pedidoRepository.save(pedido);
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }

        return valor.trim().toUpperCase(Locale.ROOT);
    }
}
