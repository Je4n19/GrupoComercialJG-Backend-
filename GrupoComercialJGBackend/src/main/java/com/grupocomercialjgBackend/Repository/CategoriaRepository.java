package com.grupocomercialjgBackend.Repository;
import com.grupocomercialjgBackend.Model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}