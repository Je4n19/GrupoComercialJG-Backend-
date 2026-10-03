package com.grupocomercialjgBackend.Controller;
import com.grupocomercialjgBackend.Model.Repuesto;
import com.grupocomercialjgBackend.Service.RepuestoService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repuestos")
@CrossOrigin(origins = "*")
public class RepuestoController {
     private final RepuestoService repuestoService;

    public RepuestoController(RepuestoService repuestoService) {
        this.repuestoService = repuestoService;
    }

    @GetMapping
    public List<Repuesto> listar() {
        return repuestoService.listar();
    }

    @PostMapping
    public Repuesto guardar(@RequestBody Repuesto repuesto) {
        return repuestoService.guardar(repuesto);
    }

    @GetMapping("/{id}")
    public Repuesto obtenerPorId(@PathVariable Long id) {
        return repuestoService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public Repuesto actualizar(
            @PathVariable Long id,
            @RequestBody Repuesto repuesto) {

        repuesto.setId(id);
        return repuestoService.guardar(repuesto);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        repuestoService.eliminar(id);
    }
}
