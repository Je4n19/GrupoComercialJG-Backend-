
package com.grupocomercialjgBackend.Controller;

import com.grupocomercialjgBackend.DTO.DetallePedidoDTO;
import com.grupocomercialjgBackend.DTO.PedidoRequestDTO;
import com.grupocomercialjgBackend.DTO.PedidoResponseDTO;

import com.grupocomercialjgBackend.Model.DetallePedido;
import com.grupocomercialjgBackend.Model.Pedido;
import com.grupocomercialjgBackend.Service.PedidoService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    // ========================================
    // REGISTRAR PEDIDO
    // ========================================

    @PostMapping
    public ResponseEntity<PedidoResponseDTO> registrar(
            @RequestBody PedidoRequestDTO request
    ) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        Pedido datos = new Pedido();

        datos.setNombreCliente(request.nombreCliente());
        datos.setTelefonoCliente(request.telefonoCliente());
        datos.setCorreoCliente(request.correoCliente());
        datos.setDireccionEntrega(request.direccionEntrega());
        datos.setDepartamento(request.departamento());
        datos.setProvincia(request.provincia());
        datos.setDistrito(request.distrito());
        datos.setReferencia(request.referencia());

        if (request.detalles() != null) {
            for (PedidoRequestDTO.ItemPedidoRequestDTO item
                    : request.detalles()) {

                if (item == null) {
                    throw new org.springframework.web.server.ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "El pedido contiene un artículo inválido"
                    );
                }

                DetallePedido detalle = new DetallePedido();

                detalle.setTipoArticulo(item.tipoArticulo());
                detalle.setIdArticulo(item.idArticulo());
                detalle.setCantidad(item.cantidad());

                datos.agregarDetalle(detalle);
            }
        }

        Pedido nuevoPedido = pedidoService.registrar(datos);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(convertirPedido(nuevoPedido));
    }

    // ========================================
    // LISTAR TODOS LOS PEDIDOS
    // ========================================

    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listar() {

        List<PedidoResponseDTO> pedidos = pedidoService.listar()
                .stream()
                .map(this::convertirPedido)
                .toList();

        return ResponseEntity.ok(pedidos);
    }

    // ========================================
    // OBTENER PEDIDO POR ID
    // ========================================

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> obtenerPorId(
            @PathVariable Long id
    ) {
        Pedido pedido = pedidoService.obtenerPorId(id);

        return ResponseEntity.ok(convertirPedido(pedido));
    }

    // ========================================
    // FILTRAR POR ESTADO
    // ========================================

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<PedidoResponseDTO>> listarPorEstado(
            @PathVariable String estado
    ) {

        List<PedidoResponseDTO> pedidos = pedidoService
                .listarPorEstado(estado.toUpperCase())
                .stream()
                .map(this::convertirPedido)
                .toList();

        return ResponseEntity.ok(pedidos);
    }

    // ========================================
    // CONVERTIR ENTIDAD A DTO
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
