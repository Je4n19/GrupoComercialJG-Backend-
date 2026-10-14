package com.grupocomercialjgBackend.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponseDTO(
        Long id,
        String nombreCliente,
        String telefonoCliente,
        String correoCliente,
        String direccionEntrega,
        String departamento,
        String provincia,
        String distrito,
        String referencia,
        BigDecimal subtotal,
        BigDecimal costoEnvio,
        BigDecimal total,
        String estadoPedido,
        String estadoPago,
        String metodoPago,
        LocalDateTime fechaPedido,
        LocalDateTime fechaPago,
        List<DetallePedidoDTO> detalles
) {}
