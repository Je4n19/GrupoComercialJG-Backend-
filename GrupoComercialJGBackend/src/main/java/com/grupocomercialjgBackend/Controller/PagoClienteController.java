
package com.grupocomercialjgBackend.Controller;

import com.grupocomercialjgBackend.Model.Pedido;
import com.grupocomercialjgBackend.Service.PagoClienteService;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    // ========================================
    // ENDPOINT ORIGINAL: JSON
    // ========================================

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
            convertirRespuesta(pedido)
        );
    }

    // ========================================
    // NUEVO: REPORTAR CON IMAGEN
    // ========================================

    @PostMapping(
        value = "/reportar-comprobante",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ReportePagoResponse>
            reportarConComprobante(

            @RequestParam("pedidoId")
            Long pedidoId,

            @RequestParam("tokenSeguimiento")
            String tokenSeguimiento,

            @RequestParam("numeroOperacion")
            String numeroOperacion,

            @RequestParam("comprobante")
            MultipartFile comprobante
    ) {

        Pedido pedido =
            pagoClienteService.reportarPagoConComprobante(
                pedidoId,
                tokenSeguimiento,
                numeroOperacion,
                comprobante
            );

        return ResponseEntity.ok(
            convertirRespuesta(pedido)
        );
    }

    private ReportePagoResponse convertirRespuesta(
            Pedido pedido
    ) {

        return new ReportePagoResponse(
            pedido.getId(),
            pedido.getMetodoPago(),
            pedido.getEstadoPago(),
            pedido.getNumeroOperacionPago(),
            pedido.getFechaReportePago(),
            "Pago reportado correctamente. "
                + "Está pendiente de verificación."
        );
    }
}
