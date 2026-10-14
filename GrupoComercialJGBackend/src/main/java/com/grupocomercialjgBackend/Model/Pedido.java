
package com.grupocomercialjgBackend.Model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TOKEN PRIVADO DE SEGUIMIENTO
    // Solo guardamos el hash, nunca el token original.
    @Column(
        name = "token_seguimiento_hash",
        length = 64,
        unique = true
    )
    private String tokenSeguimientoHash;

    // DATOS DEL CLIENTE

    @Column(nullable = false)
    private String nombreCliente;

    @Column(nullable = false)
    private String telefonoCliente;

    private String correoCliente;

    // DATOS DE ENTREGA

    @Column(nullable = false)
    private String direccionEntrega;

    private String departamento;
    private String provincia;
    private String distrito;
    private String referencia;

    // SHALOM, EMTRAFESA, OLVA, RECOJO
    @Column(length = 20)
    private String metodoEntrega;

    // IMPORTES

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costoEnvio = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // ESTADOS

    // PENDIENTE, PREPARANDO, LISTO_RECOJO,
    // ENVIADO, ENTREGADO, CANCELADO
    @Column(nullable = false, length = 30)
    private String estadoPedido = "PENDIENTE";

    // PENDIENTE, EN_REVISION, APROBADO, RECHAZADO
    @Column(nullable = false, length = 30)
    private String estadoPago = "PENDIENTE";

    // YAPE, PLIN, BCP, BBVA, TARJETA
    @Column(length = 20)
    private String metodoPago;

    // DATOS DE PAGO

    // Campo anterior conservado por compatibilidad
    @Column(unique = true)
    private String referenciaPago;

    // Numero declarado por el cliente
    @Column(length = 100)
    private String numeroOperacionPago;

    private LocalDateTime fechaReportePago;

    private LocalDateTime fechaVerificacionPago;

    @Column(length = 500)
    private String observacionPago;

    // FECHAS

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaPedido;

    private LocalDateTime fechaPago;

    // DETALLES DEL PEDIDO

    @OneToMany(
        mappedBy = "pedido",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<DetallePedido> detalles = new ArrayList<>();

    public Pedido() {
    }

    @PrePersist
    public void prePersist() {
        if (fechaPedido == null) {
            fechaPedido = LocalDateTime.now();
        }
    }

    public void agregarDetalle(DetallePedido detalle) {
        detalles.add(detalle);
        detalle.setPedido(this);
    }

    public void eliminarDetalle(DetallePedido detalle) {
        detalles.remove(detalle);
        detalle.setPedido(null);
    }

    // GETTERS Y SETTERS

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenSeguimientoHash() {
        return tokenSeguimientoHash;
    }

    public void setTokenSeguimientoHash(String tokenSeguimientoHash) {
        this.tokenSeguimientoHash = tokenSeguimientoHash;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getTelefonoCliente() {
        return telefonoCliente;
    }

    public void setTelefonoCliente(String telefonoCliente) {
        this.telefonoCliente = telefonoCliente;
    }

    public String getCorreoCliente() {
        return correoCliente;
    }

    public void setCorreoCliente(String correoCliente) {
        this.correoCliente = correoCliente;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getMetodoEntrega() {
        return metodoEntrega;
    }

    public void setMetodoEntrega(String metodoEntrega) {
        this.metodoEntrega = metodoEntrega;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getCostoEnvio() {
        return costoEnvio;
    }

    public void setCostoEnvio(BigDecimal costoEnvio) {
        this.costoEnvio = costoEnvio;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getEstadoPedido() {
        return estadoPedido;
    }

    public void setEstadoPedido(String estadoPedido) {
        this.estadoPedido = estadoPedido;
    }

    public String getEstadoPago() {
        return estadoPago;
    }

    public void setEstadoPago(String estadoPago) {
        this.estadoPago = estadoPago;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getReferenciaPago() {
        return referenciaPago;
    }

    public void setReferenciaPago(String referenciaPago) {
        this.referenciaPago = referenciaPago;
    }

    public String getNumeroOperacionPago() {
        return numeroOperacionPago;
    }

    public void setNumeroOperacionPago(String numeroOperacionPago) {
        this.numeroOperacionPago = numeroOperacionPago;
    }

    public LocalDateTime getFechaReportePago() {
        return fechaReportePago;
    }

    public void setFechaReportePago(LocalDateTime fechaReportePago) {
        this.fechaReportePago = fechaReportePago;
    }

    public LocalDateTime getFechaVerificacionPago() {
        return fechaVerificacionPago;
    }

    public void setFechaVerificacionPago(
            LocalDateTime fechaVerificacionPago
    ) {
        this.fechaVerificacionPago = fechaVerificacionPago;
    }

    public String getObservacionPago() {
        return observacionPago;
    }

    public void setObservacionPago(String observacionPago) {
        this.observacionPago = observacionPago;
    }

    public LocalDateTime getFechaPedido() {
        return fechaPedido;
    }

    public void setFechaPedido(LocalDateTime fechaPedido) {
        this.fechaPedido = fechaPedido;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDateTime fechaPago) {
        this.fechaPago = fechaPago;
    }

    public List<DetallePedido> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePedido> detalles) {
        this.detalles = detalles;
    }
}
