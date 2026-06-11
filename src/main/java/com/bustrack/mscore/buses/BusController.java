package com.bustrack.mscore.buses;

import com.bustrack.mscore.common.enums.EstadoBus;
import com.bustrack.mscore.storage.StorageService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import java.util.List;

@Controller
public class BusController {

    private final BusService service;
    private final StorageService storage;

    public BusController(BusService service, StorageService storage) {
        this.service = service;
        this.storage = storage;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<Bus> buses(@Argument EstadoBus estado) { return service.findAll(estado); }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public Bus bus(@Argument String id) { return service.findById(id); }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Bus crearBus(@Argument CrearBusInput input) {
        return service.create(Bus.builder()
                .placa(input.placa()).marca(input.marca()).modelo(input.modelo())
                .anio(input.anio()).capacidad(input.capacidad())
                .numeroCarriles(input.numeroCarriles() != null ? input.numeroCarriles() : 2)
                .build());
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Bus actualizarBus(@Argument String id, @Argument ActualizarBusInput input) {
        return service.update(id, Bus.builder()
                .placa(input.placa()).marca(input.marca()).modelo(input.modelo())
                .anio(input.anio()).capacidad(input.capacidad())
                .estadoMecanico(input.estadoMecanico())
                .build());
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Bus cambiarEstadoBus(@Argument String id, @Argument EstadoBus estado) {
        return service.cambiarEstado(id, estado);
    }

    @SchemaMapping(typeName = "Bus", field = "fotoUrl")
    public String fotoUrl(Bus bus) {
        return bus.getFotoS3Key() != null ? storage.getDownloadUrl(bus.getFotoS3Key()) : null;
    }

    public record CrearBusInput(String placa, String marca, String modelo, Integer anio,
                                Integer capacidad, Integer numeroCarriles) {}

    public record ActualizarBusInput(String placa, String marca, String modelo, Integer anio,
                                     Integer capacidad, Integer numeroCarriles, EstadoBus estadoMecanico) {}
}
