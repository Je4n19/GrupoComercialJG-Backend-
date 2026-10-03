package com.grupocomercialjgBackend.Controller;
import com.grupocomercialjgBackend.Model.CategoriaRepuesto;
import com.grupocomercialjgBackend.Service.CategoriaRepuestoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias-repuesto")
@CrossOrigin(origins = "*")
public class CategoriaRepuestoController {
    private final CategoriaRepuestoService service;

    public CategoriaRepuestoController(
            CategoriaRepuestoService service) {

        this.service = service;
    }

    @GetMapping
    public List<CategoriaRepuesto> listar() {
        return service.listar();
    }

    @PostMapping
    public CategoriaRepuesto guardar(
            @RequestBody CategoriaRepuesto categoria) {

        return service.guardar(categoria);
    }

    @GetMapping("/{id}")
    public CategoriaRepuesto obtenerPorId(
            @PathVariable Long id) {

        return service.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public CategoriaRepuesto actualizar(
            @PathVariable Long id,
            @RequestBody CategoriaRepuesto categoria) {

        categoria.setId(id);

        return service.guardar(categoria);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
