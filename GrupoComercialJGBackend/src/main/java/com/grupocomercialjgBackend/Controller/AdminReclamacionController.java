
package com.grupocomercialjgBackend.Controller;

import com.grupocomercialjgBackend.Model.Reclamacion;
import com.grupocomercialjgBackend.Service.ReclamacionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reclamaciones")
public class AdminReclamacionController {

    private final ReclamacionService reclamacionService;

    // ========================================
    // CONSTRUCTOR
    // ========================================

    public AdminReclamacionController(
            ReclamacionService reclamacionService
    ) {
        this.reclamacionService = reclamacionService;
    }

    // ========================================
    // DTO PARA RESPONDER RECLAMACIONES
    // ========================================

    public record RespuestaReclamacionRequest(
            String respuesta
    ) {}

    // ========================================
    // LISTAR TODAS LAS RECLAMACIONES - ADMIN
    // ========================================

    @GetMapping
    public ResponseEntity<List<Reclamacion>> listar() {

        List<Reclamacion> reclamaciones =
                reclamacionService.listar();

        return ResponseEntity.ok(reclamaciones);
    }

    // ========================================
    // FILTRAR RECLAMACIONES POR ESTADO - ADMIN
    // ========================================

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Reclamacion>> listarPorEstado(
            @PathVariable String estado
    ) {

        List<Reclamacion> reclamaciones =
                reclamacionService.listarPorEstado(estado);

        return ResponseEntity.ok(reclamaciones);
    }

    // ========================================
    // OBTENER RECLAMACION POR ID - ADMIN
    // ========================================

    @GetMapping("/{id}")
    public ResponseEntity<Reclamacion> obtenerPorId(
            @PathVariable Long id
    ) {

        Reclamacion reclamacion =
                reclamacionService.obtenerPorId(id);

        return ResponseEntity.ok(reclamacion);
    }

    // ========================================
    // REGISTRAR RESPUESTA - ADMIN
    // ========================================

    @PatchMapping("/{id}/responder")
    public ResponseEntity<Reclamacion> responder(
            @PathVariable Long id,
            @RequestBody RespuestaReclamacionRequest request
    ) {

        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        Reclamacion actualizada =
                reclamacionService.responder(
                        id,
                        request.respuesta()
                );

        return ResponseEntity.ok(actualizada);
    }
}
