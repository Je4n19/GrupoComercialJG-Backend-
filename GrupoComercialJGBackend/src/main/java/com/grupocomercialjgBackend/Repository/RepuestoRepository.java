package com.grupocomercialjgBackend.Repository;
import com.grupocomercialjgBackend.Model.Repuesto;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RepuestoRepository extends JpaRepository<Repuesto, Long> {
    
}
