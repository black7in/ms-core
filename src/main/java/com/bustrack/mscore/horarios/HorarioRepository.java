package com.bustrack.mscore.horarios;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HorarioRepository extends JpaRepository<Horario, String> {
    List<Horario> findByActivoTrue();
    List<Horario> findByRutaId(String rutaId);
    List<Horario> findByRutaIdAndActivo(String rutaId, boolean activo);
}
