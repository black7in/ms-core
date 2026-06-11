package com.bustrack.mscore.terminales;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import java.util.List;

@Controller
public class TerminalController {

    private final TerminalService service;

    public TerminalController(TerminalService service) { this.service = service; }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','VENDEDOR')")
    public List<Terminal> terminales() { return service.findAll(); }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','VENDEDOR')")
    public Terminal terminal(@Argument String id) { return service.findById(id); }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Terminal crearTerminal(@Argument CrearTerminalInput input) {
        var t = Terminal.builder()
                .nombre(input.nombre())
                .ciudad(input.ciudad())
                .direccion(input.direccion())
                .build();
        return service.create(t);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Terminal actualizarTerminal(@Argument String id, @Argument ActualizarTerminalInput input) {
        var t = Terminal.builder()
                .nombre(input.nombre())
                .ciudad(input.ciudad())
                .direccion(input.direccion())
                .build();
        return service.update(id, t);
    }

    public record CrearTerminalInput(String nombre, String ciudad, String direccion) {}
    public record ActualizarTerminalInput(String nombre, String ciudad, String direccion) {}
}
