package com.bustrack.mscore.boletos;

import com.bustrack.mscore.common.enums.EstadoBoleto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface BoletoRepository extends JpaRepository<Boleto, String> {
    Optional<Boleto> findByQrCode(String qrCode);

    @Query("SELECT b FROM Boleto b WHERE " +
           "(:viajeId IS NULL OR b.viaje.id = :viajeId) AND " +
           "(:clienteId IS NULL OR b.cliente.id = :clienteId) AND " +
           "(:estado IS NULL OR b.estado = :estado) " +
           "ORDER BY b.createdAt DESC")
    Page<Boleto> findFiltered(@Param("viajeId") String viajeId,
                              @Param("clienteId") String clienteId,
                              @Param("estado") EstadoBoleto estado,
                              Pageable pageable);

    @Query("SELECT COUNT(b) FROM Boleto b WHERE " +
           "(:viajeId IS NULL OR b.viaje.id = :viajeId) AND " +
           "(:clienteId IS NULL OR b.cliente.id = :clienteId) AND " +
           "(:estado IS NULL OR b.estado = :estado)")
    long countFiltered(@Param("viajeId") String viajeId,
                       @Param("clienteId") String clienteId,
                       @Param("estado") EstadoBoleto estado);
}
