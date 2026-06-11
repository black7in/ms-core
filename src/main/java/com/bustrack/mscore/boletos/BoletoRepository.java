package com.bustrack.mscore.boletos;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BoletoRepository extends JpaRepository<Boleto, String> {
    Optional<Boleto> findByQrCode(String qrCode);
}
