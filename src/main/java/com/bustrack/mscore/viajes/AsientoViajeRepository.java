package com.bustrack.mscore.viajes;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AsientoViajeRepository extends JpaRepository<AsientoViaje, String> {
    List<AsientoViaje> findByViajeIdOrderByNumeroAsiento(String viajeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AsientoViaje a WHERE a.id = :id AND a.viaje.id = :viajeId")
    Optional<AsientoViaje> findByIdAndViajeIdForUpdate(@Param("id") String id, @Param("viajeId") String viajeId);
}
