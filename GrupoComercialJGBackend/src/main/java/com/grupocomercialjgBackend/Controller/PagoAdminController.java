
package com.grupocomercialjgBackend.Controller;

import com.grupocomercialjgBackend.Model.Pedido;
import com.grupocomercialjgBackend.Service.PagoAdminService;
import com.grupocomercialjgBackend.Service.ComprobantePagoService;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/pagos")
public class PagoAdminController {

    private final PagoAdminService pagoAdminService;
    private final ComprobantePagoService comprobantePagoService;

    public PagoAdminController(
            PagoAdminService pagoAdminService,
            ComprobantePagoService comprobantePagoService
    ) {
        this.pagoAdminService = pagoAdminService;
        this.comprobantePagoService = comprobantePagoService;
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
            String observacion,
            boolean comprobanteDisponible
    ) {}

    public record ReporteRequest(
            String numeroOperacion
    ) {}

    public record RechazoRequest(
            String motivo
    ) {}

    // ========================================
    // LISTAR PAGOS
    // ========================================

    @GetMapping
    public ResponseEntity<List<PagoDTO>> listar() {

        return ResponseEntity.ok(
            pagoAdminService.listar()
                .stream()
                .map(this::convertir)
                .toList()
        );
    }

    // ========================================
    // REGISTRAR REPORTE DESDE ADMIN
    // ========================================

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

    // ========================================
    // APROBAR PAGO Y DESCONTAR STOCK
    // ========================================

    @PatchMapping("/{id}/aprobar")
    public ResponseEntity<PagoDTO> aprobar(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
            convertir(
                pagoAdminService.aprobar(id)
            )
        );
    }

    // ========================================
    // RECHAZAR PAGO
    // ========================================

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

    // ========================================
    // VER COMPROBANTE - SOLO ADMIN
    // ========================================

    @GetMapping("/{id}/comprobante")
    public ResponseEntity<byte[]> verComprobante(
            @PathVariable Long id
    ) {

        ComprobantePagoService.ImagenComprobante imagen =
            comprobantePagoService.obtenerImagen(id);

        return ResponseEntity.ok()
            .contentType(
                MediaType.parseMediaType(
                    imagen.tipoContenido()
                )
            )
            .cacheControl(CacheControl.noStore())
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"comprobante-pago\""
            )
            .header(
                "X-Content-Type-Options",
                "nosniff"
            )
            .body(imagen.contenido());
    }

    // ========================================
    // CONVERTIR A DTO
    // ========================================

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
            pedido.getObservacionPago(),
            comprobantePagoService.existe(
                pedido.getId()
            )
        );
    }
}
