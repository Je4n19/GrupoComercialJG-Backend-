
package com.grupocomercialjgBackend.Service;

import com.grupocomercialjgBackend.Model.Pedido;
import com.grupocomercialjgBackend.Repository.PedidoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

@Service
public class PagoClienteService {

    // ========================================
    // METODOS DE PAGO PERMITIDOS
    // ========================================

    private static final Set<String> METODOS_PERMITIDOS =
            Set.of("YAPE", "PLIN", "BCP", "BBVA");

    // ========================================
    // METODOS DE ENTREGA PERMITIDOS
    // ========================================

    private static final Set<String> METODOS_ENTREGA =
            Set.of("RECOJO", "SHALOM", "EMTRAFESA", "OLVA");

    private final PedidoRepository pedidoRepository;
    private final TokenPedidoService tokenPedidoService;

    public PagoClienteService(
            PedidoRepository pedidoRepository,
            TokenPedidoService tokenPedidoService
    ) {
        this.pedidoRepository = pedidoRepository;
        this.tokenPedidoService = tokenPedidoService;
    }

    // ========================================
    // REPORTAR PAGO DEL CLIENTE
    // ========================================

    @Transactional
    public Pedido reportarPago(
            Long pedidoId,
            String tokenSeguimiento,
            String numeroOperacion
    ) {

        // ========================================
        // VALIDAR IDENTIFICADOR
        // ========================================

        if (pedidoId == null || pedidoId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Pedido inválido"
            );
        }

        // ========================================
        // VALIDAR TOKEN DE SEGUIMIENTO
        // ========================================

        if (tokenSeguimiento == null ||
                tokenSeguimiento.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El token de seguimiento es obligatorio"
            );
        }

        // ========================================
        // VALIDAR NUMERO DE OPERACION
        // ========================================

        if (numeroOperacion == null ||
                numeroOperacion.trim().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ingresa el número de operación"
            );
        }

        String operacion = numeroOperacion.trim();

        if (!operacion.matches("[A-Za-z0-9-]{4,50}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El número de operación debe tener entre 4 y 50 caracteres, usando letras, números o guiones"
            );
        }

        // ========================================
        // VERIFICAR ACCESO AL PEDIDO
        // ========================================

        String hash = tokenPedidoService.generarHash(
                tokenSeguimiento
        );

        // El token privado debe corresponder
        // exactamente al ID del pedido.
        Pedido pedido = pedidoRepository
                .findByTokenSeguimientoHash(hash)
                .filter(p -> p.getId().equals(pedidoId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pedido no encontrado o acceso inválido"
                ));

        // ========================================
        // VALIDAR METODO DE PAGO
        // ========================================

        String metodoPago = normalizar(
                pedido.getMetodoPago()
        );

        if (!METODOS_PERMITIDOS.contains(metodoPago)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El método de pago no admite reporte manual"
            );
        }

        // ========================================
        // VALIDAR METODO DE ENTREGA
        // ========================================

        String metodoEntrega = normalizar(
                pedido.getMetodoEntrega()
        );

        if (!METODOS_ENTREGA.contains(metodoEntrega)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El método de entrega del pedido no es válido"
            );
        }

        // ========================================
        // VALIDAR ESTADO DEL PEDIDO
        // ========================================

        if ("CANCELADO".equals(
                normalizar(pedido.getEstadoPedido())
        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede reportar un pago de un pedido cancelado"
            );
        }

        // ========================================
        // VALIDAR ESTADO DEL PAGO
        // ========================================

        String estadoPago = normalizar(
                pedido.getEstadoPago()
        );

        if (!Set.of("PENDIENTE", "RECHAZADO")
                .contains(estadoPago)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este pedido ya tiene un pago reportado o verificado"
            );
        }

        // ========================================
        // LOGICA DEL FLETE CONTRA ENTREGA
        // ========================================

        if (!"RECOJO".equals(metodoEntrega)) {

            // SHALOM, EMTRAFESA Y OLVA:
            //
            // El cliente paga a J&G solamente
            // el importe de sus productos.
            //
            // El flete se paga directamente
            // a la agencia cuando recoge el pedido.
            //
            // No se necesita cotizar el flete
            // para reportar el pago de productos.

            String modalidadFlete = normalizar(
                    pedido.getModalidadPagoFlete()
            );

            // Compatibilidad con pedidos antiguos,
            // que pueden tener este campo nulo.
            if (!modalidadFlete.isEmpty()
                    && !"CONTRA_ENTREGA".equals(
                            modalidadFlete
                    )) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La modalidad de pago del flete no es válida"
                );
            }
        }

        // ========================================
        // REGISTRAR REPORTE DE PAGO
        // ========================================

        pedido.setNumeroOperacionPago(operacion);

        // Se conserva para compatibilidad
        // con la estructura actual.
        pedido.setReferenciaPago(operacion);

        // El pago queda pendiente de revisión
        // hasta que J&G verifique el depósito.
        pedido.setEstadoPago("EN_REVISION");

        pedido.setFechaReportePago(
                LocalDateTime.now()
        );

        pedido.setFechaVerificacionPago(null);
        pedido.setFechaPago(null);
        pedido.setObservacionPago(null);

        // No modificar subtotal, costoEnvio
        // ni total del pedido.
        //
        // Reportar un pago no significa
        // que el dinero ya fue recibido.

        return pedidoRepository.save(pedido);
    }

    // ========================================
    // NORMALIZAR TEXTOS
    // ========================================

    private String normalizar(String valor) {

        if (valor == null) {
            return "";
        }

        return valor.trim().toUpperCase(Locale.ROOT);
    }
}
