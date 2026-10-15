
package com.grupocomercialjgBackend.Controller;

import com.grupocomercialjgBackend.DTO.DetallePedidoDTO;
import com.grupocomercialjgBackend.DTO.PedidoCreadoDTO;
import com.grupocomercialjgBackend.DTO.PedidoRequestDTO;
import com.grupocomercialjgBackend.DTO.PedidoResponseDTO;

import com.grupocomercialjgBackend.Model.DetallePedido;
import com.grupocomercialjgBackend.Model.Pedido;

import com.grupocomercialjgBackend.Service.PedidoService;
import com.grupocomercialjgBackend.Service.TokenPedidoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final TokenPedidoService tokenPedidoService;

    public PedidoController(
            PedidoService pedidoService,
            TokenPedidoService tokenPedidoService
    ) {
        this.pedidoService = pedidoService;
        this.tokenPedidoService = tokenPedidoService;
    }

    // ========================================
    // DTO PARA COTIZAR FLETE
    // ========================================

    public record CotizacionFleteRequest(
            BigDecimal costoFlete,
            String estadoCotizacion,
            String observacion
    ) {}

    // ========================================
    // REGISTRAR NUEVO PEDIDO - PUBLICO
    // ========================================

    @PostMapping
    public ResponseEntity<PedidoCreadoDTO> registrar(
            @RequestBody PedidoRequestDTO request
    ) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los datos del pedido son obligatorios"
            );
        }

        Pedido datos = new Pedido();

        // DATOS DEL CLIENTE
        datos.setNombreCliente(request.nombreCliente());
        datos.setTelefonoCliente(request.telefonoCliente());
        datos.setCorreoCliente(request.correoCliente());

        // DATOS DE ENTREGA
        datos.setDireccionEntrega(request.direccionEntrega());
        datos.setDepartamento(request.departamento());
        datos.setProvincia(request.provincia());
        datos.setDistrito(request.distrito());
        datos.setReferencia(request.referencia());

        // METODOS DE ENTREGA Y PAGO
        datos.setMetodoEntrega(request.metodoEntrega());
        datos.setMetodoPago(request.metodoPago());

        // ARTICULOS DEL PEDIDO
        if (request.detalles() != null) {

            for (PedidoRequestDTO.ItemPedidoRequestDTO item
                    : request.detalles()) {

                if (item == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "El pedido contiene un articulo invalido"
                    );
                }

                DetallePedido detalle = new DetallePedido();

                detalle.setTipoArticulo(item.tipoArticulo());
                detalle.setIdArticulo(item.idArticulo());
                detalle.setCantidad(item.cantidad());

                datos.agregarDetalle(detalle);
            }
        }

        // GENERAR TOKEN PRIVADO
        String tokenSeguimiento =
                tokenPedidoService.generarToken();

        // REGISTRAR PEDIDO
        Pedido nuevoPedido = pedidoService.registrarConToken(
                datos,
                tokenSeguimiento
        );

        // DEVOLVER TOKEN SOLO AL CREAR
        PedidoCreadoDTO respuesta = new PedidoCreadoDTO(
                convertirPedido(nuevoPedido),
                tokenSeguimiento
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    // ========================================
    // LISTAR TODOS LOS PEDIDOS - ADMIN
    // ========================================

    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listar() {

        List<PedidoResponseDTO> pedidos = pedidoService
                .listar()
                .stream()
                .map(this::convertirPedido)
                .toList();

        return ResponseEntity.ok(pedidos);
    }

    // ========================================
    // FILTRAR PEDIDOS POR ESTADO - ADMIN
    // ========================================

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<PedidoResponseDTO>> listarPorEstado(
            @PathVariable String estado
    ) {

        if (estado == null || estado.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El estado es obligatorio"
            );
        }

        List<PedidoResponseDTO> pedidos = pedidoService
                .listarPorEstado(
                        estado.toUpperCase(Locale.ROOT)
                )
                .stream()
                .map(this::convertirPedido)
                .toList();

        return ResponseEntity.ok(pedidos);
    }

    // ========================================
    // OBTENER PEDIDO POR ID - ADMIN
    // ========================================

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> obtenerPorId(
            @PathVariable Long id
    ) {

        Pedido pedido = pedidoService.obtenerPorId(id);

        return ResponseEntity.ok(convertirPedido(pedido));
    }

    // ========================================
    // ACTUALIZAR COTIZACION DE FLETE - ADMIN
    // ========================================

    @PatchMapping("/{id}/flete")
    public ResponseEntity<PedidoResponseDTO> actualizarFlete(
            @PathVariable Long id,
            @RequestBody CotizacionFleteRequest request
    ) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los datos del flete son obligatorios"
            );
        }

        Pedido pedidoActualizado =
                pedidoService.actualizarCotizacionFlete(
                        id,
                        request.costoFlete(),
                        request.estadoCotizacion(),
                        request.observacion()
                );

        return ResponseEntity.ok(
                convertirPedido(pedidoActualizado)
        );
    }

    // ========================================
    // CONVERTIR PEDIDO A DTO
    // ========================================

    private PedidoResponseDTO convertirPedido(Pedido pedido) {

        List<DetallePedidoDTO> detalles = pedido.getDetalles()
                .stream()
                .map(this::convertirDetalle)
                .toList();

        return new PedidoResponseDTO(

                // IDENTIFICACION
                pedido.getId(),

                // DATOS DEL CLIENTE
                pedido.getNombreCliente(),
                pedido.getTelefonoCliente(),
                pedido.getCorreoCliente(),

                // DATOS DE ENTREGA
                pedido.getDireccionEntrega(),
                pedido.getDepartamento(),
                pedido.getProvincia(),
                pedido.getDistrito(),
                pedido.getReferencia(),
                pedido.getMetodoEntrega(),

                // IMPORTES
                pedido.getSubtotal(),
                pedido.getCostoEnvio(),
                pedido.getTotal(),

                // INFORMACION DEL FLETE
                pedido.getModalidadPagoFlete(),
                pedido.getEstadoCotizacionFlete(),
                pedido.getFechaCotizacionFlete(),
                pedido.getObservacionFlete(),

                // ESTADOS
                pedido.getEstadoPedido(),
                pedido.getEstadoPago(),

                // METODO DE PAGO
                pedido.getMetodoPago(),

                // FECHAS
                pedido.getFechaPedido(),
                pedido.getFechaPago(),

                // DETALLES
                detalles
        );
    }

    // ========================================
    // CONVERTIR DETALLE A DTO
    // ========================================

    private DetallePedidoDTO convertirDetalle(
            DetallePedido detalle
    ) {

        return new DetallePedidoDTO(
                detalle.getId(),
                detalle.getTipoArticulo(),
                detalle.getIdArticulo(),
                detalle.getNombreArticulo(),
                detalle.getMarcaArticulo(),
                detalle.getCantidad(),
                detalle.getPrecioUnitario(),
                detalle.getSubtotal()
        );
    }
}
