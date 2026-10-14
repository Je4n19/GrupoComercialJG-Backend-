package com.grupocomercialjgBackend.DTO;

import java.util.List;

public record PedidoRequestDTO(
        String nombreCliente,
        String telefonoCliente,
        String correoCliente,
        String direccionEntrega,
        String departamento,
        String provincia,
        String distrito,
        String referencia,
        List<ItemPedidoRequestDTO> detalles
) {
    public record ItemPedidoRequestDTO(
            String tipoArticulo,
            Long idArticulo,
            Integer cantidad
    ) {}
}
