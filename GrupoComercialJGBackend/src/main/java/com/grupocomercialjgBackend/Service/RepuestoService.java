package com.grupocomercialjgBackend.Service;
import com.grupocomercialjgBackend.Model.Repuesto;
import com.grupocomercialjgBackend.Repository.RepuestoRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RepuestoService {
    private final RepuestoRepository repuestoRepository;

    public RepuestoService(RepuestoRepository repuestoRepository) {
        this.repuestoRepository = repuestoRepository;
    }

    public List<Repuesto> listar() {
        return repuestoRepository.findAll();
    }

    public Repuesto guardar(Repuesto repuesto) {
        return repuestoRepository.save(repuesto);
    }

    public Repuesto obtenerPorId(Long id) {
        return repuestoRepository.findById(id).orElse(null);
    }

    public void eliminar(Long id) {
        repuestoRepository.deleteById(id);
    }
}
