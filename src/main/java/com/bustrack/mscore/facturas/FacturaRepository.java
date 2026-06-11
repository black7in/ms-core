package com.bustrack.mscore.facturas;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, String> {
    Optional<Factura> findByBoletoId(String boletoId);
}
