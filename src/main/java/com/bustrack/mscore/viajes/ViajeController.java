package com.bustrack.mscore.viajes;

import com.bustrack.mscore.common.enums.EstadoViaje;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import java.util.*;

@Controller
public class ViajeController {

    private final ViajeService service;
    private final ViajeRepository viajeRepo;

    public ViajeController(ViajeService service, ViajeRepository viajeRepo) {
        this.service = service; this.viajeRepo = viajeRepo;
    }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','VENDEDOR','CHOFER')")
    public Viaje viaje(@Argument String id) { return service.findById(id); }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','VENDEDOR')")
    public Map<String, Object> viajes(@Argument String rutaId, @Argument String fecha,
            @Argument EstadoViaje estado, @Argument Integer page, @Argument Integer limit) {
        var all = viajeRepo.findAll().stream();
        if (rutaId != null) all = all.filter(v -> v.getHorario().getRuta().getId().equals(rutaId));
        if (fecha != null) all = all.filter(v -> v.getFecha().equals(fecha));
        if (estado != null) all = all.filter(v -> v.getEstado() == estado);
        int p = page != null ? page : 1, l = limit != null ? limit : 20;
        var items = all.skip((p - 1) * l).limit(l).toList();
        return Map.of("items", items, "total", (int) viajeRepo.count());
    }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','VENDEDOR')")
    public List<Viaje> viajesDisponibles(@Argument String rutaId, @Argument String fecha) {
        return viajeRepo.findByFechaAndEstado(fecha, EstadoViaje.PROGRAMADO).stream()
                .filter(v -> v.getHorario().getRuta().getId().equals(rutaId)).toList();
    }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','CHOFER')")
    public List<Viaje> viajesPorChofer(@Argument String choferId) {
        return viajeRepo.findAll().stream()
                .filter(v -> (v.getChoferTitular() != null && v.getChoferTitular().getId().equals(choferId))
                        || (v.getChoferAuxiliar() != null && v.getChoferAuxiliar().getId().equals(choferId)))
                .toList();
    }

    @MutationMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public Viaje crearViaje(@Argument CrearViajeInput input) {
        return service.crearViaje(input.horarioId(), input.fecha(), input.busId(),
                input.choferTitularId(), input.choferAuxiliarId(), input.carrilAsignado());
    }

    @MutationMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public Viaje actualizarViaje(@Argument String id, @Argument ActualizarViajeInput input) {
        return service.actualizarViaje(id, input.busId(), input.choferTitularId(),
                input.choferAuxiliarId(), input.carrilAsignado());
    }

    @MutationMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public Viaje cancelarViaje(@Argument String id) { return service.cancelarViaje(id); }

    @MutationMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','CHOFER')")
    public Viaje iniciarViaje(@Argument String id) { return service.iniciarViaje(id); }

    @MutationMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','CHOFER')")
    public Viaje finalizarViaje(@Argument String id) { return service.finalizarViaje(id); }

    @MutationMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<Viaje> generarViajesDelDia(@Argument String fecha) {
        return service.generarViajesDelDia(fecha);
    }

    public record CrearViajeInput(String horarioId, String fecha, String busId,
                                  String choferTitularId, String choferAuxiliarId, String carrilAsignado) {}

    public record ActualizarViajeInput(String horarioId, String fecha, String busId,
                                       String choferTitularId, String choferAuxiliarId, String carrilAsignado) {}
}
