
package com.grupocomercialjgBackend.Controller;

import com.grupocomercialjgBackend.DTO.ReclamacionRequestDTO;
import com.grupocomercialjgBackend.DTO.ReclamacionResponseDTO;
import com.grupocomercialjgBackend.Service.ReclamacionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reclamaciones")
public class ReclamacionController {

    private final ReclamacionService reclamacionService;

    public ReclamacionController(
            ReclamacionService reclamacionService
    ) {
        this.reclamacionService = reclamacionService;
    }

    // REGISTRAR RECLAMACION - PUBLICO

    @PostMapping
    public ResponseEntity<ReclamacionResponseDTO> registrar(
            @RequestBody ReclamacionRequestDTO request
    ) {

        ReclamacionResponseDTO respuesta =
                reclamacionService.registrar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }
}
