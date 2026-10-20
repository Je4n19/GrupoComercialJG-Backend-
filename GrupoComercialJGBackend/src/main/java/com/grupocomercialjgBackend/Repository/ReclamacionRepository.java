
package com.grupocomercialjgBackend.Repository;

import com.grupocomercialjgBackend.Model.Reclamacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReclamacionRepository
        extends JpaRepository<Reclamacion, Long> {

    // ========================================
    // BUSCAR RECLAMACION POR NUMERO DE HOJA
    // ========================================

    Optional<Reclamacion> findByNumeroHoja(
            String numeroHoja
    );

    // ========================================
    // LISTAR RECLAMACIONES POR ESTADO
    // ========================================

    List<Reclamacion> findByEstadoOrderByFechaRegistroDesc(
            String estado
    );

    // ========================================
    // LISTAR TODAS LAS RECLAMACIONES
    // DE LA MAS RECIENTE A LA MAS ANTIGUA
    // ========================================

    List<Reclamacion> findAllByOrderByFechaRegistroDesc();

    // ========================================
    // CONTAR RECLAMACIONES POR ESTADO
    // ========================================

    long countByEstado(String estado);
}
