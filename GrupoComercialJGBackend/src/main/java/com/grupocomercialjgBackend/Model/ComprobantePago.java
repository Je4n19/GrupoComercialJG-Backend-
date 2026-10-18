
package com.grupocomercialjgBackend.Model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comprobantes_pago")
public class ComprobantePago {

    @Id
    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "cloudinary_public_id",
            nullable = false, length = 255)
    private String publicId;

    @Column(nullable = false, length = 20)
    private String formato;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    public ComprobantePago() {
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getFormato() {
        return formato;
    }

    public void setFormato(String formato) {
        this.formato = formato;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(LocalDateTime fechaSubida) {
        this.fechaSubida = fechaSubida;
    }
}
