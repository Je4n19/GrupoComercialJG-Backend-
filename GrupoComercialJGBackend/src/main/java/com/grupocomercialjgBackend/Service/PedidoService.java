
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PedidoService {

    private static final Set<String> METODOS_ENTREGA =
            Set.of("SHALOM", "EMTRAFESA", "OLVA", "RECOJO");

    // Conservamos todos los metodos.
    // BCP, BBVA y TARJETA todavia no procesan pagos.
    private static final Set<String> METODOS_PAGO =
            Set.of("TARJETA", "YAPE", "PLIN", "BCP", "BBVA");

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final RepuestoRepository repuestoRepository;
    private final TokenPedidoService tokenPedidoService;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ProductoRepository productoRepository,
            RepuestoRepository repuestoRepository,
            TokenPedidoService tokenPedidoService
    ) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.repuestoRepository = repuestoRepository;
        this.tokenPedidoService = tokenPedidoService;
    }

    // ========================================
    // CONSULTAS ADMINISTRATIVAS
    // ========================================

    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return pedidoRepository.findAllByOrderByFechaPedidoDesc();
    }

    @Transactional(readOnly = true)
    public Pedido obtenerPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pedido no encontrado con ID: " + id
                ));
    }

    @Transactional(readOnly = true)
    public List<Pedido> listarPorEstado(String estado) {
        return pedidoRepository
                .findByEstadoPedidoOrderByFechaPedidoDesc(estado);
    }

    // ========================================
    // REGISTRAR PEDIDO CON TOKEN PRIVADO
    // ========================================

    @Transactional
    public Pedido registrarConToken(
            Pedido datos,
            String tokenSeguimiento
    ) {
        String hash = tokenPedidoService.generarHash(
                tokenSeguimiento
        );

        Pedido pedido = registrar(datos);

        pedido.setTokenSeguimientoHash(hash);

        return pedidoRepository.save(pedido);
    }

    // ========================================
    // REGISTRAR PEDIDO
    // ========================================

    @Transactional
    public Pedido registrar(Pedido datos) {

        if (datos == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los datos del pedido son obligatorios"
            );
        }

        validarTexto(
                datos.getNombreCliente(),
                "Nombre del cliente"
        );

        validarTexto(
                datos.getTelefonoCliente(),
                "Telefono del cliente"
        );

        String metodoEntrega = validarOpcion(
                datos.getMetodoEntrega(),
                METODOS_ENTREGA,
                "Metodo de entrega"
        );

        String metodoPago = validarOpcion(
                datos.getMetodoPago(),
                METODOS_PAGO,
                "Metodo de pago"
        );

        if (!metodoEntrega.equals("RECOJO")) {
            validarTexto(
                    datos.getDepartamento(),
                    "Departamento"
            );
            validarTexto(
                    datos.getProvincia(),
                    "Provincia"
            );
            validarTexto(
                    datos.getDistrito(),
                    "Distrito"
            );
            validarTexto(
                    datos.getDireccionEntrega(),
                    "Direccion de entrega"
            );
        }

        if (datos.getDetalles() == null
                || datos.getDetalles().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El pedido debe contener al menos un articulo"
            );
        }

        // Agrupar cantidades de articulos repetidos
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
                        "Hay articulos con datos invalidos"
                );
            }

            String tipo = item.getTipoArticulo()
                    .trim()
                    .toUpperCase(Locale.ROOT);

            if (!tipo.equals("PRODUCTO")
                    && !tipo.equals("REPUESTO")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tipo de articulo no valido: " + tipo
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
                        "Cantidad de articulos demasiado grande"
                );
            }
        }

        // Crear una entidad nueva.
        // No confiar en precios ni estados enviados por React.
        Pedido pedido = new Pedido();

        pedido.setNombreCliente(
                datos.getNombreCliente().trim()
        );

        pedido.setTelefonoCliente(
                datos.getTelefonoCliente().trim()
        );

        pedido.setCorreoCliente(datos.getCorreoCliente());

        pedido.setMetodoEntrega(metodoEntrega);
        pedido.setMetodoPago(metodoPago);

        if (metodoEntrega.equals("RECOJO")) {

            pedido.setDireccionEntrega("RECOJO EN TIENDA");
            pedido.setDepartamento(null);
            pedido.setProvincia(null);
            pedido.setDistrito(null);

        } else {

            pedido.setDireccionEntrega(
                    datos.getDireccionEntrega().trim()
            );

            pedido.setDepartamento(
                    datos.getDepartamento().trim()
            );

            pedido.setProvincia(
                    datos.getProvincia().trim()
            );

            pedido.setDistrito(
                    datos.getDistrito().trim()
            );
        }

        pedido.setReferencia(datos.getReferencia());

        // Estados iniciales controlados por backend
        pedido.setEstadoPedido("PENDIENTE");
        pedido.setEstadoPago("PENDIENTE");

        pedido.setReferenciaPago(null);
        pedido.setNumeroOperacionPago(null);
        pedido.setFechaReportePago(null);
        pedido.setFechaVerificacionPago(null);
        pedido.setObservacionPago(null);
        pedido.setFechaPago(null);

        BigDecimal subtotalGeneral = BigDecimal.ZERO;

        // ========================================
        // VALIDAR PRODUCTOS Y REPUESTOS
        // ========================================

        for (Map.Entry<String, Integer> entrada
                : cantidades.entrySet()) {

            String[] partes = entrada.getKey().split("-", 2);

            String tipo = partes[0];
            Long idArticulo = Long.valueOf(partes[1]);
            Integer cantidad = entrada.getValue();

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
                        "Precio invalido para: " + nombre
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

            pedido.agregarDetalle(detalle);

            subtotalGeneral = subtotalGeneral.add(subtotalItem);
        }

        // ========================================
        // CALCULAR IMPORTE DEL PEDIDO
        // ========================================

        // RECOJO: no hay costo de envio.
        // AGENCIAS: el costo todavia esta pendiente
        // de cotizacion. Cero NO significa envio gratis.
        BigDecimal costoEnvio = BigDecimal.ZERO;

        pedido.setSubtotal(subtotalGeneral);
        pedido.setCostoEnvio(costoEnvio);
        pedido.setTotal(subtotalGeneral.add(costoEnvio));

        // El stock se valida, pero aun NO se reserva
        // ni se descuenta. Se implementara despues.
        return pedidoRepository.save(pedido);
    }

    // ========================================
    // VALIDACIONES
    // ========================================

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    campo + " es obligatorio"
            );
        }
    }

    private String validarOpcion(
            String valor,
            Set<String> permitidos,
            String campo
    ) {

        if (valor == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    campo + " es obligatorio"
            );
        }

        String normalizado = valor.trim()
                .toUpperCase(Locale.ROOT);

        if (!permitidos.contains(normalizado)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    campo + " no valido"
            );
        }

        return normalizado;
    }
}
