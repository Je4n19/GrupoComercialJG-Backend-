
package com.grupocomercialjgBackend.Controller;

import com.grupocomercialjgBackend.Model.Producto;
import com.grupocomercialjgBackend.Service.ProductoService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // LISTAR TODOS LOS PRODUCTOS
    @GetMapping
    public List<Producto> listar() {
        return productoService.listar();
    }

    // OBTENER PRODUCTO POR ID
    @GetMapping("/{id}")
    public Producto obtenerPorId(@PathVariable Long id) {
        return productoService.obtenerPorId(id);
    }

    // REGISTRAR PRODUCTO
    // Incluye imagen, imagen2, descripcion y datosTecnicos
    @PostMapping
    public Producto guardar(@RequestBody Producto producto) {
        return productoService.guardar(producto);
    }

    // ACTUALIZAR PRODUCTO
    @PutMapping("/{id}")
    public Producto actualizar(
            @PathVariable Long id,
            @RequestBody Producto producto) {

        return productoService.actualizar(id, producto);
    }

    // ELIMINAR PRODUCTO
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
    }
}
