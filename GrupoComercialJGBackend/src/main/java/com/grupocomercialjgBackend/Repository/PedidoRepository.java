
package com.grupocomercialjgBackend.Repository;

import com.grupocomercialjgBackend.Model.Pedido;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Obtener pedidos con sus detalles
    @Override
    @EntityGraph(attributePaths = {"detalles"})
    List<Pedido> findAll();

    // Obtener un pedido con sus detalles
    @Override
    @EntityGraph(attributePaths = {"detalles"})
    Optional<Pedido> findById(Long id);

    // Obtener los pedidos recientes con sus detalles
    @EntityGraph(attributePaths = {"detalles"})
    List<Pedido> findAllByOrderByFechaPedidoDesc();

    // Filtrar por estado del pedido
    @EntityGraph(attributePaths = {"detalles"})
    List<Pedido> findByEstadoPedidoOrderByFechaPedidoDesc(
            String estadoPedido
    );

    // Filtrar por estado del pago
    @EntityGraph(attributePaths = {"detalles"})
    List<Pedido> findByEstadoPagoOrderByFechaPedidoDesc(
            String estadoPago
    );
}
