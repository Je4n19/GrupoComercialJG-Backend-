
package com.grupocomercialjgBackend.DTO;

import java.math.BigDecimal;

public record ReclamacionRequestDTO(

        // ========================================
        // DATOS DEL CONSUMIDOR
        // ========================================

        String nombres,
        String apellidos,
        String tipoDocumento,
        String numeroDocumento,
        String domicilio,
        String telefono,
        String correo,

        // ========================================
        // CONSUMIDOR MENOR DE EDAD
        // ========================================

        boolean menorEdad,
        String representante,
        String domicilioRepresentante,
        String telefonoRepresentante,
        String correoRepresentante,

        // ========================================
        // PRODUCTO O SERVICIO
        // ========================================

        String tipoBien,
        String descripcionBien,
        BigDecimal monto,
        String numeroPedido,

        // ========================================
        // RECLAMO O QUEJA
        // ========================================

        String tipo,
        String detalle,
        String pedido

) {}
