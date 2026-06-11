package com.bustrack.mscore.buses;

import com.bustrack.mscore.common.enums.EstadoBus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BusRepository extends JpaRepository<Bus, String> {
    List<Bus> findByEstadoMecanico(EstadoBus estado);
}
