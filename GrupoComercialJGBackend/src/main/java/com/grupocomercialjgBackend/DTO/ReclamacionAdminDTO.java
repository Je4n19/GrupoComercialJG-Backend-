
package com.grupocomercialjgBackend.DTO;

import com.grupocomercialjgBackend.Model.Reclamacion;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReclamacionAdminDTO(
        Long id,
        String numeroHoja,

        String nombres,
        String apellidos,
        String tipoDocumento,
        String numeroDocumento,
        String domicilio,
        String telefono,
        String correo,

        boolean menorEdad,
        String representante,
        String domicilioRepresentante,
        String telefonoRepresentante,
        String correoRepresentante,

        String tipoBien,
        String descripcionBien,
        BigDecimal monto,
        String numeroPedido,

        String tipo,
        String detalle,
        String pedidoConsumidor,

        String estado,
        String respuestaProveedor,
        LocalDateTime fechaRegistro,
        LocalDateTime fechaRespuesta
) {

    public static ReclamacionAdminDTO desdeEntidad(
            Reclamacion reclamacion
    ) {
        return new ReclamacionAdminDTO(
                reclamacion.getId(),
                reclamacion.getNumeroHoja(),

                reclamacion.getNombres(),
                reclamacion.getApellidos(),
                reclamacion.getTipoDocumento(),
                reclamacion.getNumeroDocumento(),
                reclamacion.getDomicilio(),
                reclamacion.getTelefono(),
                reclamacion.getCorreo(),

                reclamacion.isMenorEdad(),
                reclamacion.getRepresentante(),
                reclamacion.getDomicilioRepresentante(),
                reclamacion.getTelefonoRepresentante(),
                reclamacion.getCorreoRepresentante(),

                reclamacion.getTipoBien(),
                reclamacion.getDescripcionBien(),
                reclamacion.getMonto(),
                reclamacion.getNumeroPedido(),

                reclamacion.getTipo(),
                reclamacion.getDetalle(),
                reclamacion.getPedidoConsumidor(),

                reclamacion.getEstado(),
                reclamacion.getRespuestaProveedor(),
                reclamacion.getFechaRegistro(),
                reclamacion.getFechaRespuesta()
        );
    }
}
