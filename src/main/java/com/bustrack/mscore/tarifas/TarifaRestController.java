package com.bustrack.mscore.tarifas;

import com.bustrack.mscore.common.enums.TipoDia;
import com.bustrack.mscore.rutas.RutaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/tarifas")
public class TarifaRestController {

    private final TarifaRepository tarifaRepo;
    private final RutaRepository rutaRepo;

    public TarifaRestController(TarifaRepository tarifaRepo, RutaRepository rutaRepo) {
        this.tarifaRepo = tarifaRepo;
        this.rutaRepo = rutaRepo;
    }

    @PostMapping("/aprobar-sugerencia")
    @PreAuthorize("hasRole('ADMIN')")
    public Tarifa aprobarSugerencia(@RequestBody Map<String, Object> body) {
        var rutaId = (String) body.get("rutaId");
        var fecha = (String) body.get("fecha");
        var precio = ((Number) body.get("precioAprobado")).doubleValue();
        var ruta = rutaRepo.findById(rutaId).orElseThrow(() -> new RuntimeException("Ruta no encontrada"));
        return tarifaRepo.save(Tarifa.builder()
                .ruta(ruta).tipoDia(TipoDia.TEMPORADA_ALTA)
                .precioBase(BigDecimal.valueOf(precio))
                .vigenteDesde(fecha).vigenteHasta(fecha)
                .build());
    }
}
