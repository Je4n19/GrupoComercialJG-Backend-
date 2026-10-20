
package com.grupocomercialjgBackend.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "reclamaciones",
    indexes = {
        @Index(
            name = "idx_reclamacion_estado",
            columnList = "estado"
        ),
        @Index(
            name = "idx_reclamacion_fecha",
            columnList = "fecha_registro"
        )
    }
)
public class Reclamacion {

    // ========================================
    // IDENTIFICACION
    // ========================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Numero correlativo asignado por el servidor
    @Column(
        name = "numero_hoja",
        unique = true,
        length = 40
    )
    private String numeroHoja;

    // Hash del codigo privado de seguimiento
    @Column(
        name = "codigo_seguimiento_hash",
        length = 64,
        unique = true
    )
    private String codigoSeguimientoHash;

    // ========================================
    // DATOS DEL CONSUMIDOR
    // ========================================

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(
        name = "tipo_documento",
        nullable = false,
        length = 20
    )
    private String tipoDocumento;

    @Column(
        name = "numero_documento",
        nullable = false,
        length = 20
    )
    private String numeroDocumento;

    @Column(nullable = false, length = 250)
    private String domicilio;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 150)
    private String correo;

    // ========================================
    // CONSUMIDOR MENOR DE EDAD
    // ========================================

    @Column(
        name = "menor_edad",
        nullable = false
    )
    private boolean menorEdad = false;

    @Column(length = 200)
    private String representante;

    @Column(
        name = "domicilio_representante",
        length = 250
    )
    private String domicilioRepresentante;

    @Column(
        name = "telefono_representante",
        length = 20
    )
    private String telefonoRepresentante;

    @Column(
        name = "correo_representante",
        length = 150
    )
    private String correoRepresentante;

    // ========================================
    // PRODUCTO O SERVICIO
    // ========================================

    // PRODUCTO o SERVICIO
    @Column(
        name = "tipo_bien",
        nullable = false,
        length = 20
    )
    private String tipoBien;

    @Column(
        name = "descripcion_bien",
        nullable = false,
        length = 500
    )
    private String descripcionBien;

    @Column(
        precision = 12,
        scale = 2
    )
    private BigDecimal monto;

    @Column(
        name = "numero_pedido",
        length = 60
    )
    private String numeroPedido;

    // ========================================
    // RECLAMO O QUEJA
    // ========================================

    // RECLAMO o QUEJA
    @Column(
        nullable = false,
        length = 20
    )
    private String tipo;

    @Column(
        nullable = false,
        length = 4000
    )
    private String detalle;

    @Column(
        name = "pedido_consumidor",
        nullable = false,
        length = 2000
    )
    private String pedidoConsumidor;

    // ========================================
    // GESTION ADMINISTRATIVA
    // ========================================

    // PENDIENTE, EN_REVISION, RESPONDIDO
    @Column(
        nullable = false,
        length = 30
    )
    private String estado = "PENDIENTE";

    @Column(
        name = "respuesta_proveedor",
        length = 4000
    )
    private String respuestaProveedor;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    // ========================================
    // FECHAS
    // ========================================

    @Column(
        name = "fecha_registro",
        nullable = false,
        updatable = false
    )
    private LocalDateTime fechaRegistro;

    // ========================================
    // CONSTRUCTOR
    // ========================================

    public Reclamacion() {
    }

    // ========================================
    // VALORES INICIALES
    // ========================================

    @PrePersist
    public void prePersist() {

        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }

        if (estado == null) {
            estado = "PENDIENTE";
        }
    }

    // ========================================
    // GETTERS Y SETTERS
    // ========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroHoja() {
        return numeroHoja;
    }

    public void setNumeroHoja(String numeroHoja) {
        this.numeroHoja = numeroHoja;
    }

    // ========================================
    // CODIGO PRIVADO DE SEGUIMIENTO
    // ========================================

    public String getCodigoSeguimientoHash() {
        return codigoSeguimientoHash;
    }

    public void setCodigoSeguimientoHash(
            String codigoSeguimientoHash
    ) {
        this.codigoSeguimientoHash = codigoSeguimientoHash;
    }

    // ========================================
    // DATOS DEL CONSUMIDOR
    // ========================================

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getDomicilio() {
        return domicilio;
    }

    public void setDomicilio(String domicilio) {
        this.domicilio = domicilio;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    // ========================================
    // CONSUMIDOR MENOR DE EDAD
    // ========================================

    public boolean isMenorEdad() {
        return menorEdad;
    }

    public void setMenorEdad(boolean menorEdad) {
        this.menorEdad = menorEdad;
    }

    public String getRepresentante() {
        return representante;
    }

    public void setRepresentante(String representante) {
        this.representante = representante;
    }

    public String getDomicilioRepresentante() {
        return domicilioRepresentante;
    }

    public void setDomicilioRepresentante(
            String domicilioRepresentante
    ) {
        this.domicilioRepresentante =
                domicilioRepresentante;
    }

    public String getTelefonoRepresentante() {
        return telefonoRepresentante;
    }

    public void setTelefonoRepresentante(
            String telefonoRepresentante
    ) {
        this.telefonoRepresentante =
                telefonoRepresentante;
    }

    public String getCorreoRepresentante() {
        return correoRepresentante;
    }

    public void setCorreoRepresentante(
            String correoRepresentante
    ) {
        this.correoRepresentante =
                correoRepresentante;
    }

    // ========================================
    // PRODUCTO O SERVICIO
    // ========================================

    public String getTipoBien() {
        return tipoBien;
    }

    public void setTipoBien(String tipoBien) {
        this.tipoBien = tipoBien;
    }

    public String getDescripcionBien() {
        return descripcionBien;
    }

    public void setDescripcionBien(
            String descripcionBien
    ) {
        this.descripcionBien = descripcionBien;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getNumeroPedido() {
        return numeroPedido;
    }

    public void setNumeroPedido(String numeroPedido) {
        this.numeroPedido = numeroPedido;
    }

    // ========================================
    // RECLAMO O QUEJA
    // ========================================

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getPedidoConsumidor() {
        return pedidoConsumidor;
    }

    public void setPedidoConsumidor(
            String pedidoConsumidor
    ) {
        this.pedidoConsumidor = pedidoConsumidor;
    }

    // ========================================
    // GESTION ADMINISTRATIVA
    // ========================================

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getRespuestaProveedor() {
        return respuestaProveedor;
    }

    public void setRespuestaProveedor(
            String respuestaProveedor
    ) {
        this.respuestaProveedor = respuestaProveedor;
    }

    public LocalDateTime getFechaRespuesta() {
        return fechaRespuesta;
    }

    public void setFechaRespuesta(
            LocalDateTime fechaRespuesta
    ) {
        this.fechaRespuesta = fechaRespuesta;
    }

    // ========================================
    // FECHAS
    // ========================================

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(
            LocalDateTime fechaRegistro
    ) {
        this.fechaRegistro = fechaRegistro;
    }
}
