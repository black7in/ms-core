package com.bustrack.mscore.horarios;

import com.bustrack.mscore.rutas.Ruta;
import com.bustrack.mscore.rutas.RutaRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import java.util.List;

@Controller
public class HorarioController {

    private final HorarioService service;
    private final RutaRepository rutaRepo;

    public HorarioController(HorarioService service, RutaRepository rutaRepo) {
        this.service = service;
        this.rutaRepo = rutaRepo;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','VENDEDOR')")
    public List<Horario> horarios(@Argument String rutaId, @Argument Boolean activo) {
        return service.findAll(rutaId, activo);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Horario crearHorario(@Argument CrearHorarioInput input) {
        Ruta ruta = rutaRepo.findById(input.rutaId())
                .orElseThrow(() -> new RuntimeException("Ruta no encontrada"));
        return service.create(Horario.builder()
                .ruta(ruta).horaSalida(input.horaSalida()).diasSemana(input.diasSemana()).build());
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Horario actualizarHorario(@Argument String id, @Argument ActualizarHorarioInput input) {
        var current = service.findById(id);
        return service.update(id, Horario.builder()
                .horaSalida(input.horaSalida()).diasSemana(input.diasSemana())
                .activo(input.activo() != null ? input.activo() : current.isActivo())
                .build());
    }

    public record CrearHorarioInput(String rutaId, String horaSalida, List<Integer> diasSemana) {}
    public record ActualizarHorarioInput(String rutaId, String horaSalida, List<Integer> diasSemana, Boolean activo) {}
}
