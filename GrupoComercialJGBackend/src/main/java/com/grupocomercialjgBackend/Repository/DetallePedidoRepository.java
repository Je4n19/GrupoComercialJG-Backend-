package com.grupocomercialjgBackend.Repository;

import com.grupocomercialjgBackend.Model.DetallePedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetallePedidoRepository
        extends JpaRepository<DetallePedido, Long> {

    // Obtener los artículos de un pedido
    List<DetallePedido> findByPedidoId(Long pedidoId);
}
