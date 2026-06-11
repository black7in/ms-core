package com.bustrack.mscore.viajes;

import com.bustrack.mscore.common.enums.EstadoViaje;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ViajeRepository extends JpaRepository<Viaje, String> {
    List<Viaje> findByHorarioIdAndFecha(String horarioId, String fecha);
    List<Viaje> findByFechaAndEstado(String fecha, EstadoViaje estado);
}
