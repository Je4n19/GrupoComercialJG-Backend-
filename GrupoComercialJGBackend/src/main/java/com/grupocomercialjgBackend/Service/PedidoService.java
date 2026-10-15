
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PedidoService {

    // ========================================
    // METODOS DE ENTREGA
    // ========================================

    private static final Set<String> METODOS_ENTREGA =
            Set.of("SHALOM", "EMTRAFESA", "OLVA", "RECOJO");

    // Se conservan los metodos existentes.
    // La disponibilidad de cada pago se
    // controla tambien desde el frontend.
    private static final Set<String> METODOS_PAGO =
            Set.of("TARJETA", "YAPE", "PLIN", "BCP", "BBVA");

    // ========================================
    // DEPENDENCIAS
    // ========================================

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

        if (estado == null || estado.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El estado es obligatorio"
            );
        }

        return pedidoRepository
                .findByEstadoPedidoOrderByFechaPedidoDesc(
                        estado.trim().toUpperCase(Locale.ROOT)
                );
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

        // ========================================
        // VALIDAR DATOS DE ENTREGA
        // ========================================

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

        // ========================================
        // VALIDAR ARTICULOS
        // ========================================

        if (datos.getDetalles() == null
                || datos.getDetalles().isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El pedido debe contener al menos un articulo"
            );
        }

        // Agrupar articulos repetidos.
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

        // ========================================
        // CREAR PEDIDO SEGURO
        // ========================================

        // No confiar en precios, importes,
        // estados ni cotizaciones enviadas
        // por el navegador.
        Pedido pedido = new Pedido();

        pedido.setNombreCliente(
                datos.getNombreCliente().trim()
        );

        pedido.setTelefonoCliente(
                datos.getTelefonoCliente().trim()
        );

        pedido.setCorreoCliente(
                datos.getCorreoCliente()
        );

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

        // ========================================
        // ESTADOS INICIALES
        // ========================================

        pedido.setEstadoPedido("PENDIENTE");
        pedido.setEstadoPago("PENDIENTE");

        pedido.setReferenciaPago(null);
        pedido.setNumeroOperacionPago(null);
        pedido.setFechaReportePago(null);
        pedido.setFechaVerificacionPago(null);
        pedido.setObservacionPago(null);
        pedido.setFechaPago(null);

        // ========================================
        // CONFIGURAR FLETE CONTRA ENTREGA
        // ========================================

        if (metodoEntrega.equals("RECOJO")) {

            // El cliente recoge en J&G.
            // No existe flete de agencia.
            pedido.setModalidadPagoFlete("NO_APLICA");
            pedido.setEstadoCotizacionFlete("NO_APLICA");
            pedido.setCostoEnvio(BigDecimal.ZERO);

        } else {

            // El cliente paga el flete
            // directamente a la agencia.
            pedido.setModalidadPagoFlete("CONTRA_ENTREGA");
            pedido.setEstadoCotizacionFlete("PENDIENTE");

            // El flete aun no esta cotizado.
            // Cero NO significa envio gratuito.
            pedido.setCostoEnvio(BigDecimal.ZERO);
        }

        pedido.setFechaCotizacionFlete(null);
        pedido.setObservacionFlete(null);

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
        // CALCULAR IMPORTES
        // ========================================

        pedido.setSubtotal(subtotalGeneral);

        // El importe a pagar a J&G
        // es solamente el de los productos.
        //
        // El costoEnvio se informara
        // por separado cuando el administrador
        // registre la cotizacion.
        pedido.setTotal(subtotalGeneral);

        // El stock se valida, pero todavia
        // no se reserva ni se descuenta.
        return pedidoRepository.save(pedido);
    }

    // ========================================
    // REGISTRAR COTIZACION DEL FLETE
    // SOLO DEBE INVOCARSE DESDE ADMIN
    // ========================================

    @Transactional
    public Pedido actualizarCotizacionFlete(
            Long pedidoId,
            BigDecimal costoFlete,
            String estadoCotizacion,
            String observacion
    ) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pedido no encontrado con ID: " + pedidoId
                ));

        // Solo pedidos enviados por agencia.
        if ("RECOJO".equalsIgnoreCase(
                pedido.getMetodoEntrega()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los pedidos con recojo en tienda no tienen flete"
            );
        }

        if (!METODOS_ENTREGA.contains(
                String.valueOf(pedido.getMetodoEntrega())
                        .toUpperCase(Locale.ROOT)
        )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Metodo de entrega no valido"
            );
        }

        if ("CANCELADO".equalsIgnoreCase(
                pedido.getEstadoPedido()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede cotizar un pedido cancelado"
            );
        }

        if (costoFlete == null
                || costoFlete.signum() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El costo del flete debe ser mayor que cero"
            );
        }

        if (costoFlete.scale() > 2
                || costoFlete.compareTo(
                        new BigDecimal("9999999999.99")
                ) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El importe del flete no es valido"
            );
        }

        String estado = validarOpcion(
                estadoCotizacion,
                Set.of("ESTIMADO", "CONFIRMADO"),
                "Estado de cotizacion"
        );

        if (observacion != null
                && observacion.length() > 500) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La observacion no puede superar 500 caracteres"
            );
        }

        pedido.setCostoEnvio(
                costoFlete.setScale(2, RoundingMode.UNNECESSARY)
        );

        pedido.setModalidadPagoFlete("CONTRA_ENTREGA");
        pedido.setEstadoCotizacionFlete(estado);
        pedido.setFechaCotizacionFlete(LocalDateTime.now());

        pedido.setObservacionFlete(
                observacion == null
                        ? null
                        : observacion.trim()
        );

        // MUY IMPORTANTE:
        // El flete no forma parte del
        // dinero que recibe J&G.
        //
        // Aunque el flete cambie,
        // el total de productos no cambia.
        pedido.setTotal(pedido.getSubtotal());

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
