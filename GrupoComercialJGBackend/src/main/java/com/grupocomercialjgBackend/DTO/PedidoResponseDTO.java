
package com.grupocomercialjgBackend.DTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponseDTO(

        // ========================================
        // IDENTIFICACION DEL PEDIDO
        // ========================================

        Long id,

        // ========================================
        // DATOS DEL CLIENTE
        // ========================================

        String nombreCliente,
        String telefonoCliente,
        String correoCliente,

        // ========================================
        // DATOS DE ENTREGA
        // ========================================

        String direccionEntrega,
        String departamento,
        String provincia,
        String distrito,
        String referencia,

        // SHALOM, EMTRAFESA, OLVA, RECOJO
        String metodoEntrega,

        // ========================================
        // IMPORTES
        // ========================================

        // Importe de los productos
        BigDecimal subtotal,

        // Flete que paga el cliente directamente
        // a la agencia cuando recoge su pedido.
        // No se suma al total cobrado por J&G.
        BigDecimal costoEnvio,

        // Importe que el cliente paga a J&G.
        // Solo incluye productos.
        BigDecimal total,

        // ========================================
        // INFORMACION DEL FLETE
        // ========================================

        // CONTRA_ENTREGA o NO_APLICA
        String modalidadPagoFlete,

        // PENDIENTE, ESTIMADO,
        // CONFIRMADO o NO_APLICA
        String estadoCotizacionFlete,

        // Fecha de la cotizacion
        LocalDateTime fechaCotizacionFlete,

        // Observaciones sobre peso, volumen
        // o informacion de la agencia.
        String observacionFlete,

        // ========================================
        // ESTADOS
        // ========================================

        String estadoPedido,
        String estadoPago,

        // ========================================
        // METODO DE PAGO
        // ========================================

        String metodoPago,

        // ========================================
        // FECHAS
        // ========================================

        LocalDateTime fechaPedido,
        LocalDateTime fechaPago,

        // ========================================
        // ARTICULOS DEL PEDIDO
        // ========================================

        List<DetallePedidoDTO> detalles

) {}
