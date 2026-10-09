
package com.grupocomercialjgBackend.Model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "productos")
@Data
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String marca;

    private String categoria;

    private Double precio;

    private Integer stock;

    // Descripción larga
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // Primera imagen del producto
    @Column(name = "imagen", columnDefinition = "TEXT")
    private String imagen;

    // Segunda imagen del producto
    @Column(name = "imagen2", columnDefinition = "TEXT")
    private String imagen2;

    // Modelo del producto
    private String modelo;

    // Especificaciones técnicas en formato de texto
    @Column(name = "datos_tecnicos", columnDefinition = "TEXT")
    private String datosTecnicos;

    public Producto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public String getImagen2() {
        return imagen2;
    }

    public void setImagen2(String imagen2) {
        this.imagen2 = imagen2;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getDatosTecnicos() {
        return datosTecnicos;
    }

    public void setDatosTecnicos(String datosTecnicos) {
        this.datosTecnicos = datosTecnicos;
    }
}
