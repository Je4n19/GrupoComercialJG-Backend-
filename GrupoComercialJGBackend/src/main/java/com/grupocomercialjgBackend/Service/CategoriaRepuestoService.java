package com.grupocomercialjgBackend.Service;

import com.grupocomercialjgBackend.Model.CategoriaRepuesto;
import com.grupocomercialjgBackend.Repository.CategoriaRepuestoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaRepuestoService {
    private final CategoriaRepuestoRepository repository;

    public CategoriaRepuestoService(CategoriaRepuestoRepository repository) {
        this.repository = repository;
    }

    public List<CategoriaRepuesto> listar() {
        return repository.findAll();
    }

    public CategoriaRepuesto guardar(CategoriaRepuesto categoria) {
        return repository.save(categoria);
    }

    public CategoriaRepuesto obtenerPorId(Long id) {
        return repository.findById(id).orElse(null);
    }

    public void eliminar(Long id) {
        repository.deleteById(id);
    }
}
