
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

    // ========================================
    // TOKEN PRIVADO DE SEGUIMIENTO
    // ========================================

    @Column(
        name = "token_seguimiento_hash",
        length = 64,
        unique = true
    )
    private String tokenSeguimientoHash;

    // ========================================
    // DATOS DEL CLIENTE
    // ========================================

    @Column(nullable = false)
    private String nombreCliente;

    @Column(nullable = false)
    private String telefonoCliente;

    private String correoCliente;

    // ========================================
    // DATOS DE ENTREGA
    // ========================================

    @Column(nullable = false)
    private String direccionEntrega;

    private String departamento;
    private String provincia;
    private String distrito;
    private String referencia;

    // SHALOM, EMTRAFESA, OLVA, RECOJO
    @Column(length = 20)
    private String metodoEntrega;

    // ========================================
    // IMPORTES DE PRODUCTOS
    // ========================================

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    // Se conserva por compatibilidad con
    // el backend y los pedidos existentes.
    //
    // Para envios contra entrega representa
    // el flete informado por la agencia,
    // NO un cobro a favor de J&G.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal costoEnvio = BigDecimal.ZERO;

    // Total que el cliente paga a J&G.
    // NO incluye flete contra entrega.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // ========================================
    // NUEVA GESTION DEL FLETE
    // ========================================

    // CONTRA_ENTREGA:
    // El cliente paga el flete directamente
    // a la agencia cuando recoge su pedido.
    //
    // NO_APLICA:
    // Recojo en tienda.
    @Column(name = "modalidad_pago_flete", length = 30)
    private String modalidadPagoFlete;

    // PENDIENTE:
    // Aun no se conoce el importe.
    //
    // ESTIMADO:
    // J&G ingreso un importe aproximado.
    //
    // CONFIRMADO:
    // La agencia confirmo el importe.
    //
    // NO_APLICA:
    // Recojo en tienda.
    @Column(name = "estado_cotizacion_flete", length = 20)
    private String estadoCotizacionFlete;

    // Fecha de la ultima cotizacion.
    @Column(name = "fecha_cotizacion_flete")
    private LocalDateTime fechaCotizacionFlete;

    // Informacion adicional de la cotizacion.
    @Column(name = "observacion_flete", length = 500)
    private String observacionFlete;

    // ========================================
    // ESTADOS DEL PEDIDO
    // ========================================

    // PENDIENTE, PREPARANDO, LISTO_RECOJO,
    // ENVIADO, ENTREGADO, CANCELADO
    @Column(nullable = false, length = 30)
    private String estadoPedido = "PENDIENTE";

    // PENDIENTE, EN_REVISION,
    // APROBADO, RECHAZADO
    @Column(nullable = false, length = 30)
    private String estadoPago = "PENDIENTE";

    // YAPE, PLIN, BCP, BBVA, TARJETA
    @Column(length = 20)
    private String metodoPago;

    // ========================================
    // DATOS DEL PAGO
    // ========================================

    // Campo conservado por compatibilidad.
    @Column(unique = true)
    private String referenciaPago;

    @Column(length = 100)
    private String numeroOperacionPago;

    private LocalDateTime fechaReportePago;

    private LocalDateTime fechaVerificacionPago;

    @Column(length = 500)
    private String observacionPago;

    // ========================================
    // FECHAS
    // ========================================

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaPedido;

    private LocalDateTime fechaPago;

    // ========================================
    // DETALLES DEL PEDIDO
    // ========================================

    @OneToMany(
        mappedBy = "pedido",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<DetallePedido> detalles = new ArrayList<>();

    // ========================================
    // CONSTRUCTOR
    // ========================================

    public Pedido() {
    }

    // ========================================
    // VALORES INICIALES
    // ========================================

    @PrePersist
    public void prePersist() {

        if (fechaPedido == null) {
            fechaPedido = LocalDateTime.now();
        }

        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }

        if (costoEnvio == null) {
            costoEnvio = BigDecimal.ZERO;
        }

        if (total == null) {
            total = BigDecimal.ZERO;
        }

        if (estadoPedido == null) {
            estadoPedido = "PENDIENTE";
        }

        if (estadoPago == null) {
            estadoPago = "PENDIENTE";
        }

        if ("RECOJO".equalsIgnoreCase(metodoEntrega)) {

            modalidadPagoFlete = "NO_APLICA";
            estadoCotizacionFlete = "NO_APLICA";
            costoEnvio = BigDecimal.ZERO;

        } else {

            if (modalidadPagoFlete == null) {
                modalidadPagoFlete = "CONTRA_ENTREGA";
            }

            if (estadoCotizacionFlete == null) {
                estadoCotizacionFlete = "PENDIENTE";
            }
        }
    }

    // ========================================
    // GESTION DE DETALLES
    // ========================================

    public void agregarDetalle(DetallePedido detalle) {

        if (detalle == null) {
            throw new IllegalArgumentException(
                "El detalle no puede ser nulo"
            );
        }

        detalles.add(detalle);
        detalle.setPedido(this);
    }

    public void eliminarDetalle(DetallePedido detalle) {

        if (detalle == null) {
            return;
        }

        detalles.remove(detalle);
        detalle.setPedido(null);
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

    public String getTokenSeguimientoHash() {
        return tokenSeguimientoHash;
    }

    public void setTokenSeguimientoHash(
        String tokenSeguimientoHash
    ) {
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

    // ========================================
    // NUEVOS GETTERS Y SETTERS DEL FLETE
    // ========================================

    public String getModalidadPagoFlete() {
        return modalidadPagoFlete;
    }

    public void setModalidadPagoFlete(
        String modalidadPagoFlete
    ) {
        this.modalidadPagoFlete = modalidadPagoFlete;
    }

    public String getEstadoCotizacionFlete() {
        return estadoCotizacionFlete;
    }

    public void setEstadoCotizacionFlete(
        String estadoCotizacionFlete
    ) {
        this.estadoCotizacionFlete = estadoCotizacionFlete;
    }

    public LocalDateTime getFechaCotizacionFlete() {
        return fechaCotizacionFlete;
    }

    public void setFechaCotizacionFlete(
        LocalDateTime fechaCotizacionFlete
    ) {
        this.fechaCotizacionFlete = fechaCotizacionFlete;
    }

    public String getObservacionFlete() {
        return observacionFlete;
    }

    public void setObservacionFlete(
        String observacionFlete
    ) {
        this.observacionFlete = observacionFlete;
    }

    // ========================================
    // ESTADOS
    // ========================================

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

    // ========================================
    // DATOS DEL PAGO
    // ========================================

    public String getReferenciaPago() {
        return referenciaPago;
    }

    public void setReferenciaPago(String referenciaPago) {
        this.referenciaPago = referenciaPago;
    }

    public String getNumeroOperacionPago() {
        return numeroOperacionPago;
    }

    public void setNumeroOperacionPago(
        String numeroOperacionPago
    ) {
        this.numeroOperacionPago = numeroOperacionPago;
    }

    public LocalDateTime getFechaReportePago() {
        return fechaReportePago;
    }

    public void setFechaReportePago(
        LocalDateTime fechaReportePago
    ) {
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

    public void setObservacionPago(
        String observacionPago
    ) {
        this.observacionPago = observacionPago;
    }

    // ========================================
    // FECHAS
    // ========================================

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

    // ========================================
    // DETALLES
    // ========================================

    public List<DetallePedido> getDetalles() {
        return detalles;
    }

    public void setDetalles(
        List<DetallePedido> detalles
    ) {
        this.detalles = detalles;
    }
}
