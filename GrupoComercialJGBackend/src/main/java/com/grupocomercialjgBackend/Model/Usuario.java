
package com.grupocomercialjgBackend.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre de usuario único para cada administrador
    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String username;

    // Aquí se guardará el HASH BCrypt, no la contraseña original
    @NotBlank
    @Column(nullable = false, length = 255)
    private String password;

    // Único rol permitido: ADMIN
    @Column(nullable = false, length = 20, updatable = false)
    private String rol = "ADMIN";

    // Nombre del administrador
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String nombre;

    // Permite desactivar una cuenta sin eliminarla
    @Column(nullable = false)
    private boolean activo = true;

    public Usuario() {
    }

    public Usuario(String username, String password, String nombre) {
        this.username = username;
        this.password = password;
        this.nombre = nombre;
        this.rol = "ADMIN";
        this.activo = true;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        return rol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
