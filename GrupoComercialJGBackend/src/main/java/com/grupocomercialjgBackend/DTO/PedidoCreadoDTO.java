package com.grupocomercialjgBackend.DTO;

public record PedidoCreadoDTO(
        PedidoResponseDTO pedido,
        String tokenSeguimiento
) {}
