package com.bustrack.mscore.viajes;

import com.bustrack.mscore.choferes.ChoferRepository;
import com.bustrack.mscore.common.enums.EstadoBoleto;
import com.bustrack.mscore.usuarios.Usuario;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/viajes")
public class ViajeRestController {

    private final ViajeService viajeService;
    private final ViajeRepository viajeRepo;
    private final ChoferRepository choferRepo;
    private final AsientoViajeRepository asientoRepo;

    public ViajeRestController(ViajeService viajeService, ViajeRepository viajeRepo,
                                ChoferRepository choferRepo, AsientoViajeRepository asientoRepo) {
        this.viajeService = viajeService;
        this.viajeRepo = viajeRepo;
        this.choferRepo = choferRepo;
        this.asientoRepo = asientoRepo;
    }

    @GetMapping("/chofer/activo")
    @PreAuthorize("hasRole('CHOFER')")
    public ResponseEntity<?> viajeActivo(@AuthenticationPrincipal Usuario user) {
        var chofer = choferRepo.findByUsuarioId(user.getId());
        if (chofer.isEmpty()) return ResponseEntity.ok(Map.of("viaje", null));
        var hoy = LocalDate.now().toString();
        var viajes = viajeRepo.findAll().stream()
                .filter(v -> v.getFecha().equals(hoy)
                        && v.getChoferTitular() != null
                        && v.getChoferTitular().getId().equals(chofer.get().getId())
                        && (v.getEstado() == com.bustrack.mscore.common.enums.EstadoViaje.PROGRAMADO
                            || v.getEstado() == com.bustrack.mscore.common.enums.EstadoViaje.EN_RUTA))
                .findFirst();
        return viajes.isPresent()
                ? ResponseEntity.ok(Map.of("viaje", viajes.get()))
                : ResponseEntity.ok(Map.of("viaje", null));
    }

    @GetMapping("/hoy")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public Map<String, Object> viajesHoy(@AuthenticationPrincipal Usuario user) {
        var hoy = LocalDate.now().toString();
        var viajes = viajeRepo.findAll().stream().filter(v -> v.getFecha().equals(hoy));
        // Terminal filter for SUPERVISOR
        if (!user.getRol().name().equals("ADMIN") && user.getTerminal() != null) {
            var tid = user.getTerminal().getId();
            viajes = viajes.filter(v -> v.getHorario().getRuta().getOrigen().getId().equals(tid));
        }
        return Map.of("viajes", viajes.map(v -> {
            var vendidos = v.getAsientos() != null ? v.getAsientos().stream()
                    .filter(a -> a.getEstado() != com.bustrack.mscore.common.enums.EstadoAsiento.LIBRE).count() : 0;
            var libres = v.getAsientos() != null ? v.getAsientos().stream()
                    .filter(a -> a.getEstado() == com.bustrack.mscore.common.enums.EstadoAsiento.LIBRE).count() : 0;
            return Map.of("id", v.getId(), "fecha", v.getFecha(), "estado", v.getEstado().name(),
                    "carrilAsignado", v.getCarrilAsignado(),
                    "horario", Map.of("horaSalida", v.getHorario().getHoraSalida()),
                    "bus", v.getBus() != null ? Map.of("placa", v.getBus().getPlaca()) : null,
                    "choferTitular", v.getChoferTitular() != null ? Map.of("nombre", v.getChoferTitular().getNombre()) : null,
                    "totalVendidos", vendidos, "totalLibres", libres);
        }).toList());
    }

    @GetMapping("/proximos")
    public Map<String, Object> proximos(@RequestParam(defaultValue = "2") int horas) {
        var hoy = LocalDate.now().toString();
        var ahora = java.time.LocalTime.now();
        var limite = ahora.plusHours(horas);
        var viajes = viajeRepo.findAll().stream()
                .filter(v -> v.getFecha().equals(hoy) && v.getEstado() == com.bustrack.mscore.common.enums.EstadoViaje.PROGRAMADO
                        && v.getHorario().getHoraSalida().compareTo(ahora.toString()) >= 0
                        && v.getHorario().getHoraSalida().compareTo(limite.toString()) <= 0);
        return Map.of("viajes", viajes.map(v -> Map.of("id", v.getId(),
                "horaSalida", v.getHorario().getHoraSalida(),
                "carrilAsignado", v.getCarrilAsignado(),
                "ruta", Map.of("origen", v.getHorario().getRuta().getOrigen().getCiudad(),
                        "destino", v.getHorario().getRuta().getDestino().getCiudad()),
                "bus", v.getBus() != null ? Map.of("placa", v.getBus().getPlaca()) : null,
                "pasajeros", List.of())).toList());
    }

    @GetMapping("/{id}/asientos")
    @PreAuthorize("hasAnyRole('CHOFER','ADMIN','SUPERVISOR')")
    public List<AsientoViaje> asientos(@PathVariable String id) {
        return asientoRepo.findByViajeIdOrderByNumeroAsiento(id);
    }

    @PatchMapping("/{id}/iniciar")
    @PreAuthorize("hasAnyRole('CHOFER','ADMIN')")
    public Viaje iniciar(@PathVariable String id) { return viajeService.iniciarViaje(id); }

    @PatchMapping("/{id}/finalizar")
    @PreAuthorize("hasAnyRole('CHOFER','ADMIN')")
    public Viaje finalizar(@PathVariable String id) { return viajeService.finalizarViaje(id); }
}
