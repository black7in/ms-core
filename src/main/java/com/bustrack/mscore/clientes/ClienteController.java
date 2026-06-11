package com.bustrack.mscore.clientes;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import java.util.List;

@Controller
public class ClienteController {

    private final ClienteRepository repo;

    public ClienteController(ClienteRepository repo) { this.repo = repo; }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public List<Cliente> clientes(@Argument String busqueda) {
        return repo.findAll().stream()
                .filter(c -> busqueda == null || c.getNombre().toLowerCase().contains(busqueda.toLowerCase())
                        || c.getCi().contains(busqueda)).toList();
    }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public Cliente cliente(@Argument String id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public Cliente clientePorCi(@Argument String ci) {
        return repo.findByCi(ci).orElse(null);
    }

    @MutationMapping @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public Cliente crearCliente(@Argument CrearClienteInput input) {
        return repo.save(Cliente.builder().ci(input.ci()).nombre(input.nombre())
                .telefono(input.telefono()).email(input.email()).build());
    }

    @MutationMapping @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR')")
    public Cliente actualizarCliente(@Argument String id, @Argument ActualizarClienteInput input) {
        var c = repo.findById(id).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        if (input.nombre() != null) c.setNombre(input.nombre());
        if (input.telefono() != null) c.setTelefono(input.telefono());
        if (input.email() != null) c.setEmail(input.email());
        return repo.save(c);
    }

    public record CrearClienteInput(String ci, String nombre, String telefono, String email) {}
    public record ActualizarClienteInput(String ci, String nombre, String telefono, String email) {}
}
