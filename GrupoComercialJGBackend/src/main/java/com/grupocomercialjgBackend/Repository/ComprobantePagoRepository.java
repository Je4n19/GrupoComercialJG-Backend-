package com.grupocomercialjgBackend.Repository;

import com.grupocomercialjgBackend.Model.ComprobantePago;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComprobantePagoRepository
        extends JpaRepository<ComprobantePago, Long> {
}
