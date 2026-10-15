
package com.grupocomercialjgBackend.Service;

import com.grupocomercialjgBackend.Model.Pedido;
import com.grupocomercialjgBackend.Repository.PedidoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class PagoClienteService {

    private static final Set<String> METODOS_PERMITIDOS =
            Set.of("YAPE", "PLIN", "BCP", "BBVA");

    private final PedidoRepository pedidoRepository;
    private final TokenPedidoService tokenPedidoService;

    public PagoClienteService(
            PedidoRepository pedidoRepository,
            TokenPedidoService tokenPedidoService
    ) {
        this.pedidoRepository = pedidoRepository;
        this.tokenPedidoService = tokenPedidoService;
    }

    @Transactional
    public Pedido reportarPago(
            Long pedidoId,
            String tokenSeguimiento,
            String numeroOperacion
    ) {

        if (pedidoId == null || pedidoId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Pedido inválido"
            );
        }

        if (tokenSeguimiento == null ||
                tokenSeguimiento.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El token de seguimiento es obligatorio"
            );
        }

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

        String hash = tokenPedidoService.generarHash(
                tokenSeguimiento
        );

        // Buscar por hash, no solamente por ID.
        Pedido pedido = pedidoRepository
                .findByTokenSeguimientoHash(hash)
                .filter(p -> p.getId().equals(pedidoId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pedido no encontrado o acceso inválido"
                ));

        if (!METODOS_PERMITIDOS.contains(
                pedido.getMetodoPago()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El método de pago no admite reporte manual"
            );
        }

        // No permitir cobros antes de cotizar el envío.
        if (!"RECOJO".equals(pedido.getMetodoEntrega())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El costo de envío debe confirmarse antes de reportar el pago"
            );
        }

        if (!Set.of("PENDIENTE", "RECHAZADO")
                .contains(pedido.getEstadoPago())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este pedido ya tiene un pago reportado o verificado"
            );
        }

        if ("CANCELADO".equals(pedido.getEstadoPedido())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede reportar un pago de un pedido cancelado"
            );
        }

        pedido.setNumeroOperacionPago(operacion);
        pedido.setReferenciaPago(operacion);
        pedido.setEstadoPago("EN_REVISION");
        pedido.setFechaReportePago(LocalDateTime.now());
        pedido.setFechaVerificacionPago(null);
        pedido.setFechaPago(null);
        pedido.setObservacionPago(null);

        return pedidoRepository.save(pedido);
    }
}
