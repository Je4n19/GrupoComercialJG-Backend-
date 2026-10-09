
package com.grupocomercialjgBackend.Service;

import com.grupocomercialjgBackend.Model.Producto;
import com.grupocomercialjgBackend.Repository.ProductoRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    // LISTAR PRODUCTOS
    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    // OBTENER PRODUCTO POR ID
    public Producto obtenerPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Producto no encontrado con ID: " + id
                ));
    }

    // GUARDAR PRODUCTO
    // Guarda también imagen2 y datosTecnicos
    public Producto guardar(Producto producto) {
        producto.setId(null);
        return productoRepository.save(producto);
    }

    // ACTUALIZAR PRODUCTO EXISTENTE
    public Producto actualizar(Long id, Producto datos) {

        Producto producto = obtenerPorId(id);

        producto.setNombre(datos.getNombre());
        producto.setMarca(datos.getMarca());
        producto.setCategoria(datos.getCategoria());
        producto.setPrecio(datos.getPrecio());
        producto.setStock(datos.getStock());

        // DESCRIPCIÓN LARGA
        producto.setDescripcion(datos.getDescripcion());

        // IMÁGENES
        producto.setImagen(datos.getImagen());
        producto.setImagen2(datos.getImagen2());

        // MODELO
        producto.setModelo(datos.getModelo());

        // DATOS TÉCNICOS
        producto.setDatosTecnicos(datos.getDatosTecnicos());

        return productoRepository.save(producto);
    }

    // ELIMINAR PRODUCTO
    public void eliminar(Long id) {

        Producto producto = obtenerPorId(id);

        productoRepository.delete(producto);
    }
}
