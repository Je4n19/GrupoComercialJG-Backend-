
package com.grupocomercialjgBackend.Controller;

import com.grupocomercialjgBackend.DTO.ReclamacionAdminDTO;
import com.grupocomercialjgBackend.Model.Reclamacion;
import com.grupocomercialjgBackend.Service.ReclamacionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reclamaciones")
public class AdminReclamacionController {

    private final ReclamacionService reclamacionService;

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
    // LISTAR TODAS LAS RECLAMACIONES
    // ========================================

    @GetMapping
    public ResponseEntity<List<ReclamacionAdminDTO>> listar() {

        List<ReclamacionAdminDTO> reclamaciones =
                reclamacionService.listar()
                        .stream()
                        .map(ReclamacionAdminDTO::desdeEntidad)
                        .toList();

        return ResponseEntity.ok(reclamaciones);
    }

    // ========================================
    // FILTRAR POR ESTADO
    // ========================================

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<ReclamacionAdminDTO>> listarPorEstado(
            @PathVariable String estado
    ) {

        List<ReclamacionAdminDTO> reclamaciones =
                reclamacionService.listarPorEstado(estado)
                        .stream()
                        .map(ReclamacionAdminDTO::desdeEntidad)
                        .toList();

        return ResponseEntity.ok(reclamaciones);
    }

    // ========================================
    // OBTENER RECLAMACION POR ID
    // ========================================

    @GetMapping("/{id}")
    public ResponseEntity<ReclamacionAdminDTO> obtenerPorId(
            @PathVariable Long id
    ) {

        Reclamacion reclamacion =
                reclamacionService.obtenerPorId(id);

        return ResponseEntity.ok(
                ReclamacionAdminDTO.desdeEntidad(reclamacion)
        );
    }

    // ========================================
    // REGISTRAR RESPUESTA DEL ADMINISTRADOR
    // ========================================

    @PatchMapping("/{id}/responder")
    public ResponseEntity<ReclamacionAdminDTO> responder(
            @PathVariable Long id,
            @RequestBody RespuestaReclamacionRequest request
    ) {

        if (request == null ||
                request.respuesta() == null ||
                request.respuesta().isBlank()) {

            return ResponseEntity.badRequest().build();
        }

        Reclamacion actualizada =
                reclamacionService.responder(
                        id,
                        request.respuesta().trim()
                );

        return ResponseEntity.ok(
                ReclamacionAdminDTO.desdeEntidad(actualizada)
        );
    }
}
