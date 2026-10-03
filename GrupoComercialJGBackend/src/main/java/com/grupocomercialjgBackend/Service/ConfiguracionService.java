package com.grupocomercialjgBackend.Service;

import com.grupocomercialjgBackend.Model.Configuracion;
import com.grupocomercialjgBackend.Repository.ConfiguracionRepository;
import org.springframework.stereotype.Service;

@Service
public class ConfiguracionService {
    private final ConfiguracionRepository repository;

    public ConfiguracionService(ConfiguracionRepository repository) {
        this.repository = repository;
    }

    public Configuracion obtener() {

        return repository.findAll()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Configuracion guardar(Configuracion configuracion) {

        Configuracion existente = obtener();

        if (existente != null) {
            configuracion.setId(existente.getId());
        }

        return repository.save(configuracion);
    }
}
