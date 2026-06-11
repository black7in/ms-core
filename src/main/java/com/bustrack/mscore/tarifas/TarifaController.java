package com.bustrack.mscore.tarifas;

import com.bustrack.mscore.common.enums.TipoDia;
import com.bustrack.mscore.rutas.Ruta;
import com.bustrack.mscore.rutas.RutaRepository;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Controller
public class TarifaController {

    private final TarifaRepository repo;
    private final RutaRepository rutaRepo;

    public TarifaController(TarifaRepository repo, RutaRepository rutaRepo) {
        this.repo = repo;
        this.rutaRepo = rutaRepo;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<Tarifa> tarifas(@Argument String rutaId) { return repo.findByRutaId(rutaId); }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','VENDEDOR')")
    public Tarifa tarifaActual(@Argument String rutaId, @Argument String fecha) {
        return repo.findTarifaActual(rutaId, determinarTipoDia(fecha), fecha)
                .orElseThrow(() -> new RuntimeException("No hay tarifa vigente"));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Tarifa crearTarifa(@Argument CrearTarifaInput input) {
        Ruta ruta = rutaRepo.findById(input.rutaId())
                .orElseThrow(() -> new RuntimeException("Ruta no encontrada"));
        return repo.save(Tarifa.builder().ruta(ruta).tipoDia(input.tipoDia())
                .precioBase(BigDecimal.valueOf(input.precioBase()))
                .vigenteDesde(input.vigenteDesde()).vigenteHasta(input.vigenteHasta()).build());
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Tarifa actualizarTarifa(@Argument String id, @Argument ActualizarTarifaInput input) {
        var t = repo.findById(id).orElseThrow(() -> new RuntimeException("Tarifa no encontrada"));
        if (input.precioBase() != null) t.setPrecioBase(BigDecimal.valueOf(input.precioBase()));
        if (input.vigenteHasta() != null) t.setVigenteHasta(input.vigenteHasta());
        return repo.save(t);
    }

    private TipoDia determinarTipoDia(String fecha) {
        var feriados = Set.of("01-01","01-22","02-17","02-18","03-23","04-10",
                "05-01","06-19","08-06","11-02","12-25");
        var d = LocalDate.parse(fecha);
        if (feriados.contains(fecha.substring(5))) return TipoDia.FERIADO;
        if (d.getDayOfWeek().getValue() >= 5) return TipoDia.VIERNES_DOMINGO;
        return TipoDia.LUNES_JUEVES;
    }

    public record CrearTarifaInput(String rutaId, TipoDia tipoDia, Double precioBase,
                                   String vigenteDesde, String vigenteHasta) {}

    public record ActualizarTarifaInput(String rutaId, TipoDia tipoDia, Double precioBase,
                                        String vigenteDesde, String vigenteHasta) {}
}
