package com.grupocomercialjgBackend.Repository;
import com.grupocomercialjgBackend.Model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}