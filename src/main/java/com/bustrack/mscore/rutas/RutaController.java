package com.bustrack.mscore.rutas;

import com.bustrack.mscore.terminales.Terminal;
import com.bustrack.mscore.terminales.TerminalRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import java.math.BigDecimal;

@Controller
public class RutaController {

    private final RutaService service;
    private final TerminalRepository terminalRepo;

    public RutaController(RutaService service, TerminalRepository terminalRepo) {
        this.service = service;
        this.terminalRepo = terminalRepo;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','VENDEDOR')")
    public java.util.List<Ruta> rutas(@Argument Boolean activa) {
        return service.findAll(activa, null);
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','VENDEDOR')")
    public Ruta ruta(@Argument String id) { return service.findById(id); }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Ruta crearRuta(@Argument CrearRutaInput input) {
        Terminal origen = terminalRepo.findById(input.terminalOrigenId())
                .orElseThrow(() -> new RuntimeException("Terminal origen no encontrada"));
        Terminal destino = terminalRepo.findById(input.terminalDestinoId())
                .orElseThrow(() -> new RuntimeException("Terminal destino no encontrada"));
        return service.create(Ruta.builder()
                .origen(origen).destino(destino)
                .distanciaKm(input.distanciaKm() != null ? BigDecimal.valueOf(input.distanciaKm()) : null)
                .duracionEstimadaMin(input.duracionEstimadaMin())
                .build());
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Ruta actualizarRuta(@Argument String id, @Argument ActualizarRutaInput input) {
        var current = service.findById(id);
        return service.update(id, Ruta.builder()
                .distanciaKm(input.distanciaKm() != null ? BigDecimal.valueOf(input.distanciaKm()) : null)
                .duracionEstimadaMin(input.duracionEstimadaMin())
                .activa(input.activa() != null ? input.activa() : current.isActiva())
                .build());
    }

    public record CrearRutaInput(String terminalOrigenId, String terminalDestinoId,
                                 Double distanciaKm, Integer duracionEstimadaMin) {}

    public record ActualizarRutaInput(String terminalOrigenId, String terminalDestinoId,
                                      Double distanciaKm, Integer duracionEstimadaMin, Boolean activa) {}
}
