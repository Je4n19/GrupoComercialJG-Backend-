
package com.grupocomercialjgBackend.Controller;

import com.grupocomercialjgBackend.Model.Pedido;
import com.grupocomercialjgBackend.Service.PagoClienteService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/pagos")
public class PagoClienteController {

    private final PagoClienteService pagoClienteService;

    public PagoClienteController(
            PagoClienteService pagoClienteService
    ) {
        this.pagoClienteService = pagoClienteService;
    }

    public record ReportePagoRequest(
            Long pedidoId,
            String tokenSeguimiento,
            String numeroOperacion
    ) {}

    public record ReportePagoResponse(
            Long pedidoId,
            String metodoPago,
            String estadoPago,
            String numeroOperacion,
            LocalDateTime fechaReporte,
            String mensaje
    ) {}

    @PostMapping("/reportar")
    public ResponseEntity<ReportePagoResponse> reportar(
            @RequestBody ReportePagoRequest request
    ) {

        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        Pedido pedido = pagoClienteService.reportarPago(
                request.pedidoId(),
                request.tokenSeguimiento(),
                request.numeroOperacion()
        );

        return ResponseEntity.ok(
                new ReportePagoResponse(
                        pedido.getId(),
                        pedido.getMetodoPago(),
                        pedido.getEstadoPago(),
                        pedido.getNumeroOperacionPago(),
                        pedido.getFechaReportePago(),
                        "Pago reportado correctamente. "
                        + "Está pendiente de verificación."
                )
        );
    }
}
