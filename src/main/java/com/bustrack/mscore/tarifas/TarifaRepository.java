package com.bustrack.mscore.tarifas;

import com.bustrack.mscore.common.enums.TipoDia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TarifaRepository extends JpaRepository<Tarifa, String> {
    List<Tarifa> findByRutaId(String rutaId);

    @Query("SELECT t FROM Tarifa t WHERE t.ruta.id = :rutaId AND t.tipoDia = :tipoDia " +
           "AND t.vigenteDesde <= :fecha AND (t.vigenteHasta IS NULL OR t.vigenteHasta >= :fecha) " +
           "ORDER BY t.vigenteDesde DESC")
    Optional<Tarifa> findTarifaActual(@Param("rutaId") String rutaId,
                                       @Param("tipoDia") TipoDia tipoDia,
                                       @Param("fecha") String fecha);
}
