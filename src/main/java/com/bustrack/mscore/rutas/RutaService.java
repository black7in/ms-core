package com.bustrack.mscore.rutas;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class RutaService {

    private final RutaRepository repo;

    public RutaService(RutaRepository repo) { this.repo = repo; }

    public List<Ruta> findAll(Boolean activa, String terminalId) {
        var all = repo.findAll().stream();
        if (activa != null) all = all.filter(r -> r.isActiva() == activa);
        if (terminalId != null) all = all.filter(r -> r.getOrigen().getId().equals(terminalId));
        return all.toList();
    }

    public Ruta findById(String id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Ruta no encontrada"));
    }

    public Ruta create(Ruta ruta) { return repo.save(ruta); }

    public Ruta update(String id, Ruta data) {
        var r = findById(id);
        if (data.getDistanciaKm() != null) r.setDistanciaKm(data.getDistanciaKm());
        if (data.getDuracionEstimadaMin() != null) r.setDuracionEstimadaMin(data.getDuracionEstimadaMin());
        if (data.isActiva() != r.isActiva()) r.setActiva(data.isActiva());
        return repo.save(r);
    }
}
