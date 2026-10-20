
package com.grupocomercialjgBackend.DTO;

import java.time.LocalDateTime;

public record ReclamacionResponseDTO(

        // ========================================
        // CONSTANCIA DE REGISTRO
        // ========================================

        String numeroHoja,

        String codigoSeguimiento,

        LocalDateTime fechaRegistro,

        // ========================================
        // INFORMACION DE LA RECLAMACION
        // ========================================

        String tipo,

        String estado,

        String mensaje

) {}
