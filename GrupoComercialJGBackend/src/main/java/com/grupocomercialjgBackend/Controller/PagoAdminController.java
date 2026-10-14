
package com.grupocomercialjgBackend.Controller;

import com.grupocomercialjgBackend.Model.Pedido;
import com.grupocomercialjgBackend.Service.PagoAdminService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/pagos")
public class PagoAdminController {

    private final PagoAdminService pagoAdminService;

    public PagoAdminController(PagoAdminService pagoAdminService) {
        this.pagoAdminService = pagoAdminService;
    }

    public record PagoDTO(
            Long pedidoId,
            String nombreCliente,
            String metodoPago,
            String metodoEntrega,
            String estadoPago,
            String numeroOperacion,
            String total,
            LocalDateTime fechaReporte,
            LocalDateTime fechaVerificacion,
            String observacion
    ) {}

    public record ReporteRequest(String numeroOperacion) {}

    public record RechazoRequest(String motivo) {}

    @GetMapping
    public ResponseEntity<List<PagoDTO>> listar() {
        return ResponseEntity.ok(
                pagoAdminService.listar()
                        .stream()
                        .map(this::convertir)
                        .toList()
        );
    }

    @PatchMapping("/{id}/reportar")
    public ResponseEntity<PagoDTO> registrarReporte(
            @PathVariable Long id,
            @RequestBody ReporteRequest request
    ) {
        return ResponseEntity.ok(
                convertir(
                        pagoAdminService.registrarReporte(
                                id,
                                request.numeroOperacion()
                        )
                )
        );
    }

    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<PagoDTO> aprobar(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                convertir(pagoAdminService.aprobar(id))
        );
    }

    @PatchMapping("/{id}/rechazar")
    public ResponseEntity<PagoDTO> rechazar(
            @PathVariable Long id,
            @RequestBody RechazoRequest request
    ) {
        return ResponseEntity.ok(
                convertir(
                        pagoAdminService.rechazar(
                                id,
                                request.motivo()
                        )
                )
        );
    }

    private PagoDTO convertir(Pedido pedido) {
        return new PagoDTO(
                pedido.getId(),
                pedido.getNombreCliente(),
                pedido.getMetodoPago(),
                pedido.getMetodoEntrega(),
                pedido.getEstadoPago(),
                pedido.getNumeroOperacionPago(),
                pedido.getTotal().toPlainString(),
                pedido.getFechaReportePago(),
                pedido.getFechaVerificacionPago(),
                pedido.getObservacionPago()
        );
    }
}
