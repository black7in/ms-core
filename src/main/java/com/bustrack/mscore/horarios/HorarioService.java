package com.bustrack.mscore.horarios;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class HorarioService {

    private final HorarioRepository repo;

    public HorarioService(HorarioRepository repo) { this.repo = repo; }

    public List<Horario> findAll(String rutaId, Boolean activo) {
        if (rutaId != null && activo != null) return repo.findByRutaIdAndActivo(rutaId, activo);
        if (rutaId != null) return repo.findByRutaId(rutaId);
        if (Boolean.TRUE.equals(activo)) return repo.findByActivoTrue();
        return repo.findAll();
    }

    public Horario findById(String id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Horario no encontrado"));
    }

    public Horario create(Horario h) { return repo.save(h); }

    public Horario update(String id, Horario data) {
        var h = findById(id);
        if (data.getHoraSalida() != null) h.setHoraSalida(data.getHoraSalida());
        if (data.getDiasSemana() != null) h.setDiasSemana(data.getDiasSemana());
        if (data.isActivo() != h.isActivo()) h.setActivo(data.isActivo());
        return repo.save(h);
    }
}
