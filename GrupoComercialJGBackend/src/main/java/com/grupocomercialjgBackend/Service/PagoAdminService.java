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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class PagoAdminService {

    private static final Set<String> METODOS_HABILITADOS =
            Set.of("YAPE", "PLIN", "BCP", "BBVA");

    private static final Set<String> ENTREGAS_HABILITADAS =
            Set.of("RECOJO", "SHALOM", "EMTRAFESA", "OLVA");

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final RepuestoRepository repuestoRepository;

    public PagoAdminService(
            PedidoRepository pedidoRepository,
            ProductoRepository productoRepository,
            RepuestoRepository repuestoRepository
    ) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.repuestoRepository = repuestoRepository;
    }

    // ========================================
    // LISTAR PEDIDOS
    // ========================================

    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return pedidoRepository.findAllByOrderByFechaPedidoDesc();
    }

    // ========================================
    // REGISTRAR REPORTE DE PAGO
    // ========================================

    @Transactional
    public Pedido registrarReporte(
            Long pedidoId,
            String numeroOperacion
    ) {

        Pedido pedido = buscarConBloqueo(pedidoId);

        if ("CANCELADO".equals(pedido.getEstadoPedido())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede reportar un pago de un pedido cancelado"
            );
        }

        if (!METODOS_HABILITADOS.contains(pedido.getMetodoPago())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El metodo de pago no esta habilitado"
            );
        }

        if (!ENTREGAS_HABILITADAS.contains(pedido.getMetodoEntrega())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Metodo de entrega no habilitado"
            );
        }

        if (!"PENDIENTE".equals(pedido.getEstadoPago())
                && !"RECHAZADO".equals(pedido.getEstadoPago())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El pago no puede reportarse en su estado actual"
            );
        }

        if (numeroOperacion == null
                || !numeroOperacion.trim().matches("[A-Za-z0-9-]{4,50}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Numero de operacion invalido"
            );
        }

        pedido.setNumeroOperacionPago(numeroOperacion.trim());
        pedido.setFechaReportePago(LocalDateTime.now());
        pedido.setFechaVerificacionPago(null);
        pedido.setFechaPago(null);
        pedido.setObservacionPago(null);
        pedido.setEstadoPago("EN_REVISION");

        return pedidoRepository.save(pedido);
    }

    // ========================================
    // APROBAR PAGO Y DESCONTAR STOCK
    // ========================================

    @Transactional
    public Pedido aprobar(Long pedidoId) {

        // Bloquear el pedido para evitar doble aprobacion.
        Pedido pedido = buscarConBloqueo(pedidoId);

        if ("CANCELADO".equals(pedido.getEstadoPedido())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede aprobar un pago de un pedido cancelado"
            );
        }

        if (!"EN_REVISION".equals(pedido.getEstadoPago())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden aprobar pagos en revision"
            );
        }

        if (pedido.getNumeroOperacionPago() == null
                || pedido.getNumeroOperacionPago().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El pedido no tiene un numero de operacion reportado"
            );
        }

        if (pedido.getDetalles() == null
                || pedido.getDetalles().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El pedido no contiene articulos"
            );
        }

        // Agrupar articulos para no descontar dos veces
        // de forma incorrecta si aparecen repetidos.
        Map<String, Integer> cantidades = new TreeMap<>();

        for (DetallePedido detalle : pedido.getDetalles()) {

            if (detalle == null
                    || detalle.getTipoArticulo() == null
                    || detalle.getIdArticulo() == null
                    || detalle.getCantidad() == null
                    || detalle.getCantidad() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El pedido contiene articulos invalidos"
                );
            }

            String tipo = detalle.getTipoArticulo().trim().toUpperCase();

            if (!tipo.equals("PRODUCTO")
                    && !tipo.equals("REPUESTO")) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Tipo de articulo no valido"
                );
            }

            String clave = tipo + "-" + detalle.getIdArticulo();

            try {
                cantidades.merge(
                        clave,
                        detalle.getCantidad(),
                        Math::addExact
                );
            } catch (ArithmeticException ex) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cantidad de articulos invalida"
                );
            }
        }

        // ========================================
        // VERIFICAR STOCK CON BLOQUEO
        // ========================================

        Map<Long, Producto> productos = new HashMap<>();
        Map<Long, Repuesto> repuestos = new HashMap<>();

        for (Map.Entry<String, Integer> entrada
                : cantidades.entrySet()) {

            String[] partes = entrada.getKey().split("-", 2);

            String tipo = partes[0];
            Long idArticulo = Long.valueOf(partes[1]);
            int cantidad = entrada.getValue();

            if ("PRODUCTO".equals(tipo)) {

                Producto producto = productoRepository
                        .buscarPorIdConBloqueo(idArticulo)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "El producto ya no existe: " + idArticulo
                        ));

                if (producto.getStock() == null
                        || producto.getStock() < cantidad) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Stock insuficiente para: "
                                    + producto.getNombre()
                    );
                }

                productos.put(idArticulo, producto);

            } else {

                Repuesto repuesto = repuestoRepository
                        .buscarPorIdConBloqueo(idArticulo)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "El repuesto ya no existe: " + idArticulo
                        ));

                if (repuesto.getStock() == null
                        || repuesto.getStock() < cantidad) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Stock insuficiente para: "
                                    + repuesto.getNombre()
                    );
                }

                repuestos.put(idArticulo, repuesto);
            }
        }

        // ========================================
        // DESCONTAR STOCK
        // ========================================

        for (Map.Entry<String, Integer> entrada
                : cantidades.entrySet()) {

            String[] partes = entrada.getKey().split("-", 2);

            String tipo = partes[0];
            Long idArticulo = Long.valueOf(partes[1]);
            int cantidad = entrada.getValue();

            if ("PRODUCTO".equals(tipo)) {

                Producto producto = productos.get(idArticulo);

                producto.setStock(
                        producto.getStock() - cantidad
                );

                productoRepository.save(producto);

            } else {

                Repuesto repuesto = repuestos.get(idArticulo);

                repuesto.setStock(
                        repuesto.getStock() - cantidad
                );

                repuestoRepository.save(repuesto);
            }
        }

        // ========================================
        // CONFIRMAR APROBACION
        // ========================================

        LocalDateTime ahora = LocalDateTime.now();

        pedido.setEstadoPago("APROBADO");
        pedido.setFechaPago(ahora);
        pedido.setFechaVerificacionPago(ahora);
        pedido.setObservacionPago(
                "Abono verificado por administrador"
        );

        return pedidoRepository.save(pedido);
    }

    // ========================================
    // RECHAZAR PAGO
    // ========================================

    @Transactional
    public Pedido rechazar(Long pedidoId, String motivo) {

        Pedido pedido = buscarConBloqueo(pedidoId);

        if (!"EN_REVISION".equals(pedido.getEstadoPago())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden rechazar pagos en revision"
            );
        }

        if (motivo == null
                || motivo.isBlank()
                || motivo.trim().length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar un motivo valido"
            );
        }

        pedido.setEstadoPago("RECHAZADO");
        pedido.setFechaVerificacionPago(LocalDateTime.now());
        pedido.setObservacionPago(motivo.trim());
        pedido.setFechaPago(null);

        return pedidoRepository.save(pedido);
    }

    // ========================================
    // BUSCAR PEDIDO CON BLOQUEO
    // ========================================

    private Pedido buscarConBloqueo(Long id) {
        return pedidoRepository.buscarPorIdConBloqueo(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pedido no encontrado"
                ));
    }
}