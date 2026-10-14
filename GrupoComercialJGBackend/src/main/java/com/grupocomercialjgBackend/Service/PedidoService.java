
package com.grupocomercialjgBackend.Service;

import com.grupocomercialjgBackend.Model.DetallePedido;
import com.grupocomercialjgBackend.Model.Pedido;
import com.grupocomercialjgBackend.Model.Producto;
import com.grupocomercialjgBackend.Model.Repuesto;

import com.grupocomercialjgBackend.Repository.PedidoRepository;
import com.grupocomercialjgBackend.Repository.ProductoRepository;
import com.grupocomercialjgBackend.Repository.RepuestoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final RepuestoRepository repuestoRepository;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ProductoRepository productoRepository,
            RepuestoRepository repuestoRepository
    ) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.repuestoRepository = repuestoRepository;
    }

    // LISTAR PEDIDOS
    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return pedidoRepository.findAllByOrderByFechaPedidoDesc();
    }

    // OBTENER PEDIDO POR ID
    @Transactional(readOnly = true)
    public Pedido obtenerPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pedido no encontrado con ID: " + id
                ));
    }

    // LISTAR PEDIDOS POR ESTADO
    @Transactional(readOnly = true)
    public List<Pedido> listarPorEstado(String estado) {
        return pedidoRepository
                .findByEstadoPedidoOrderByFechaPedidoDesc(estado);
    }

    // REGISTRAR PEDIDO
    @Transactional
    public Pedido registrar(Pedido datos) {

        if (datos == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los datos del pedido son obligatorios"
            );
        }

        validarTexto(datos.getNombreCliente(), "Nombre del cliente");
        validarTexto(datos.getTelefonoCliente(), "Teléfono del cliente");
        validarTexto(datos.getDireccionEntrega(), "Dirección de entrega");

        if (datos.getDetalles() == null
                || datos.getDetalles().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El pedido debe contener al menos un artículo"
            );
        }

        // Agrupar cantidades por artículo para validar stock.
        Map<String, Integer> cantidades = new HashMap<>();

        for (DetallePedido item : datos.getDetalles()) {

            if (item == null
                    || item.getIdArticulo() == null
                    || item.getIdArticulo() <= 0
                    || item.getTipoArticulo() == null
                    || item.getCantidad() == null
                    || item.getCantidad() <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Hay artículos con datos inválidos"
                );
            }

            String tipo = item.getTipoArticulo()
                    .trim()
                    .toUpperCase();

            if (!tipo.equals("PRODUCTO")
                    && !tipo.equals("REPUESTO")) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tipo de artículo no válido: " + tipo
                );
            }

            String clave = tipo + "-" + item.getIdArticulo();

            try {
                cantidades.merge(
                        clave,
                        item.getCantidad(),
                        Math::addExact
                );
            } catch (ArithmeticException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cantidad de artículos demasiado grande"
                );
            }
        }

        Pedido pedido = new Pedido();

        pedido.setNombreCliente(datos.getNombreCliente().trim());
        pedido.setTelefonoCliente(datos.getTelefonoCliente().trim());
        pedido.setCorreoCliente(datos.getCorreoCliente());
        pedido.setDireccionEntrega(datos.getDireccionEntrega().trim());
        pedido.setDepartamento(datos.getDepartamento());
        pedido.setProvincia(datos.getProvincia());
        pedido.setDistrito(datos.getDistrito());
        pedido.setReferencia(datos.getReferencia());

        // Estados definidos por el servidor.
        pedido.setEstadoPedido("PENDIENTE");
        pedido.setEstadoPago("PENDIENTE");

        // El método de pago se configurará en el checkout.
        pedido.setMetodoPago(null);
        pedido.setReferenciaPago(null);

        BigDecimal subtotalGeneral = BigDecimal.ZERO;
        List<DetallePedido> detallesValidados = new ArrayList<>();

        for (Map.Entry<String, Integer> entrada
                : cantidades.entrySet()) {

            String clave = entrada.getKey();
            Integer cantidad = entrada.getValue();

            String[] partes = clave.split("-", 2);
            String tipo = partes[0];
            Long idArticulo = Long.valueOf(partes[1]);

            String nombre;
            String marca;
            Double precio;
            Integer stock;

            if (tipo.equals("PRODUCTO")) {

                Producto producto = productoRepository
                        .findById(idArticulo)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Producto no encontrado: " + idArticulo
                        ));

                nombre = producto.getNombre();
                marca = producto.getMarca();
                precio = producto.getPrecio();
                stock = producto.getStock();

            } else {

                Repuesto repuesto = repuestoRepository
                        .findById(idArticulo)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Repuesto no encontrado: " + idArticulo
                        ));

                nombre = repuesto.getNombre();
                marca = repuesto.getMarca();
                precio = repuesto.getPrecio();
                stock = repuesto.getStock();
            }

            if (stock == null || stock < cantidad) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock insuficiente para: " + nombre
                );
            }

            if (precio == null
                    || !Double.isFinite(precio)
                    || precio < 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Precio inválido para: " + nombre
                );
            }

            BigDecimal precioUnitario = BigDecimal
                    .valueOf(precio)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal subtotalItem = precioUnitario.multiply(
                    BigDecimal.valueOf(cantidad)
            );

            DetallePedido detalle = new DetallePedido();

            detalle.setTipoArticulo(tipo);
            detalle.setIdArticulo(idArticulo);
            detalle.setNombreArticulo(nombre);
            detalle.setMarcaArticulo(marca);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSubtotal(subtotalItem);

            detallesValidados.add(detalle);
            subtotalGeneral = subtotalGeneral.add(subtotalItem);
        }

        for (DetallePedido detalle : detallesValidados) {
            pedido.agregarDetalle(detalle);
        }

        // Por ahora no cobramos envío automáticamente.
        BigDecimal costoEnvio = BigDecimal.ZERO;

        pedido.setSubtotal(subtotalGeneral);
        pedido.setCostoEnvio(costoEnvio);
        pedido.setTotal(subtotalGeneral.add(costoEnvio));

        return pedidoRepository.save(pedido);
    }

    private void validarTexto(String valor, String campo) {

        if (valor == null || valor.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    campo + " es obligatorio"
            );
        }
    }
}
