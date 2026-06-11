package com.bustrack.mscore.choferes;

import com.bustrack.mscore.common.enums.EstadoChofer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChoferRepository extends JpaRepository<Chofer, String> {
    List<Chofer> findByEstado(EstadoChofer estado);
    Optional<Chofer> findByUsuarioId(String usuarioId);
}
