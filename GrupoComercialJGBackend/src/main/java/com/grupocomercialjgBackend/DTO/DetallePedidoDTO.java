
package com.grupocomercialjgBackend.DTO;

import java.math.BigDecimal;

public record DetallePedidoDTO(
        Long id,
        String tipoArticulo,
        Long idArticulo,
        String nombreArticulo,
        String marcaArticulo,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
) {}
