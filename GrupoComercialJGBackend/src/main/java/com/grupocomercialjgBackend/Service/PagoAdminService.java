
package com.grupocomercialjgBackend.Service;

import com.grupocomercialjgBackend.Model.Pedido;
import com.grupocomercialjgBackend.Repository.PedidoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class PagoAdminService {

    private static final Set<String> METODOS_HABILITADOS =
            Set.of("YAPE", "PLIN");

    private final PedidoRepository pedidoRepository;

    public PagoAdminService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return pedidoRepository.findAllByOrderByFechaPedidoDesc();
    }

    @Transactional
    public Pedido registrarReporte(
            Long pedidoId,
            String numeroOperacion
    ) {
        Pedido pedido = buscar(pedidoId);

        if (!METODOS_HABILITADOS.contains(pedido.getMetodoPago())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El metodo de pago no esta habilitado"
            );
        }

        if (!"PENDIENTE".equals(pedido.getEstadoPago())
                && !"RECHAZADO".equals(pedido.getEstadoPago())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El pago no puede reportarse en su estado actual"
            );
        }

        if (numeroOperacion == null
                || numeroOperacion.isBlank()
                || numeroOperacion.trim().length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Numero de operacion invalido"
            );
        }

        if (!"RECOJO".equals(pedido.getMetodoEntrega())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Primero debe confirmarse el importe y las condiciones del envio"
            );
        }

        pedido.setNumeroOperacionPago(numeroOperacion.trim());
        pedido.setFechaReportePago(LocalDateTime.now());
        pedido.setFechaVerificacionPago(null);
        pedido.setObservacionPago(null);
        pedido.setEstadoPago("EN_REVISION");

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido aprobar(Long pedidoId) {
        Pedido pedido = buscar(pedidoId);

        if (!"EN_REVISION".equals(pedido.getEstadoPago())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden aprobar pagos en revision"
            );
        }

        pedido.setEstadoPago("APROBADO");
        pedido.setFechaPago(LocalDateTime.now());
        pedido.setFechaVerificacionPago(LocalDateTime.now());
        pedido.setObservacionPago("Abono verificado por administrador");

        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido rechazar(Long pedidoId, String motivo) {
        Pedido pedido = buscar(pedidoId);

        if (!"EN_REVISION".equals(pedido.getEstadoPago())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Solo se pueden rechazar pagos en revision"
            );
        }

        if (motivo == null
                || motivo.isBlank()
                || motivo.trim().length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar un motivo valido"
            );
        }

        pedido.setEstadoPago("RECHAZADO");
        pedido.setFechaVerificacionPago(LocalDateTime.now());
        pedido.setObservacionPago(motivo.trim());
        pedido.setFechaPago(null);

        return pedidoRepository.save(pedido);
    }

    private Pedido buscar(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pedido no encontrado"
                ));
    }
}
