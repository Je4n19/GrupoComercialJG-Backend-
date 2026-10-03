package com.grupocomercialjgBackend.Controller;
import com.grupocomercialjgBackend.Model.Configuracion;
import com.grupocomercialjgBackend.Service.ConfiguracionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/configuracion")
@CrossOrigin(origins = "*")
public class ConfiguracionController {
     private final ConfiguracionService service;

    public ConfiguracionController(ConfiguracionService service) {
        this.service = service;
    }

    @GetMapping
    public Configuracion obtener() {
        return service.obtener();
    }

    @PostMapping
    public Configuracion guardar(@RequestBody Configuracion configuracion) {
        return service.guardar(configuracion);
    }
}
