
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
    // REGISTRAR NUEVO PEDIDO
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

        // Datos del cliente
        datos.setNombreCliente(request.nombreCliente());
        datos.setTelefonoCliente(request.telefonoCliente());
        datos.setCorreoCliente(request.correoCliente());

        // Datos de entrega
        datos.setDireccionEntrega(request.direccionEntrega());
        datos.setDepartamento(request.departamento());
        datos.setProvincia(request.provincia());
        datos.setDistrito(request.distrito());
        datos.setReferencia(request.referencia());

        // Metodo de entrega y pago
        datos.setMetodoEntrega(request.metodoEntrega());
        datos.setMetodoPago(request.metodoPago());

        // Articulos del pedido
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

        // Generar token privado para este pedido
        String tokenSeguimiento =
                tokenPedidoService.generarToken();

        // Registrar el pedido con el hash del token
        Pedido nuevoPedido = pedidoService.registrarConToken(
                datos,
                tokenSeguimiento
        );

        // El token original solo se devuelve
        // en esta respuesta de creacion.
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
    // FILTRAR PEDIDOS POR ESTADO - ADMIN
    // ========================================

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<PedidoResponseDTO>> listarPorEstado(
            @PathVariable String estado
    ) {

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
    // CONVERTIR PEDIDO A DTO
    // ========================================

    private PedidoResponseDTO convertirPedido(Pedido pedido) {

        List<DetallePedidoDTO> detalles = pedido.getDetalles()
                .stream()
                .map(this::convertirDetalle)
                .toList();

        return new PedidoResponseDTO(
                pedido.getId(),
                pedido.getNombreCliente(),
                pedido.getTelefonoCliente(),
                pedido.getCorreoCliente(),
                pedido.getDireccionEntrega(),
                pedido.getDepartamento(),
                pedido.getProvincia(),
                pedido.getDistrito(),
                pedido.getReferencia(),
                pedido.getMetodoEntrega(),
                pedido.getSubtotal(),
                pedido.getCostoEnvio(),
                pedido.getTotal(),
                pedido.getEstadoPedido(),
                pedido.getEstadoPago(),
                pedido.getMetodoPago(),
                pedido.getFechaPedido(),
                pedido.getFechaPago(),
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
