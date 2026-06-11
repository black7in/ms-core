package com.bustrack.mscore.boletos;

import com.bustrack.mscore.common.enums.EstadoBoleto;
import com.bustrack.mscore.common.enums.EstadoAsiento;
import com.bustrack.mscore.viajes.AsientoViajeRepository;
import com.bustrack.mscore.storage.StorageService;
import com.bustrack.mscore.usuarios.Usuario;
import jakarta.persistence.EntityManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/boletos")
public class BoletoRestController {

    private final BoletoRepository boletoRepo;
    private final AsientoViajeRepository asientoRepo;
    private final EntityManager em;
    private final StorageService storage;

    public BoletoRestController(BoletoRepository boletoRepo, AsientoViajeRepository asientoRepo,
                                 EntityManager em, StorageService storage) {
        this.boletoRepo = boletoRepo;
        this.asientoRepo = asientoRepo;
        this.em = em;
        this.storage = storage;
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public ResponseEntity<?> descargarPdf(@PathVariable String id) {
        var b = boletoRepo.findById(id).orElseThrow(() -> new RuntimeException("Boleto no encontrado"));
        if (b.getPdfS3Key() == null) return ResponseEntity.notFound().build();
        var url = storage.getDownloadUrl(b.getPdfS3Key());
        return ResponseEntity.status(302).header("Location", url).build();
    }

    @GetMapping("/validar/{qrCode}")
    @PreAuthorize("hasAnyRole('CHOFER','ADMIN','SUPERVISOR')")
    public Map<String, Object> validarQr(@PathVariable String qrCode) {
        var b = boletoRepo.findByQrCode(qrCode)
                .orElseThrow(() -> new RuntimeException("Boleto no encontrado"));

        if (b.getEstado() == EstadoBoleto.ABORDADO)
            return Map.of("valido", false, "mensaje", "Boleto ya fue escaneado", "estado", "ABORDADO");
        if (b.getEstado() == EstadoBoleto.CANCELADO)
            return Map.of("valido", false, "mensaje", "Boleto cancelado", "estado", "CANCELADO");

        return Map.of("valido", true, "boleto", Map.of(
            "id", b.getId(), "estado", b.getEstado().name(),
            "numeroAsiento", b.getAsiento().getNumeroAsiento(),
            "viaje", Map.of("id", b.getViaje().getId(), "fecha", b.getViaje().getFecha(),
                    "horaSalida", b.getViaje().getHorario().getHoraSalida()),
            "cliente", Map.of("nombre", b.getCliente().getNombre(), "ci", b.getCliente().getCi())
        ), "mensaje", "Boleto válido");
    }

    @PatchMapping("/{id}/abordar")
    @PreAuthorize("hasAnyRole('CHOFER','ADMIN','SUPERVISOR')")
    public Map<String, Object> abordar(@PathVariable String id) {
        var b = boletoRepo.findById(id).orElseThrow();
        if (b.getEstado() != EstadoBoleto.VIGENTE) throw new RuntimeException("Boleto no vigente");
        b.setEstado(EstadoBoleto.ABORDADO);
        b.getAsiento().setEstado(EstadoAsiento.ABORDADO);
        boletoRepo.save(b);
        asientoRepo.save(b.getAsiento());
        return Map.of("id", b.getId(), "estado", b.getEstado().name(), "mensaje", "Boleto marcado como abordado");
    }
}
