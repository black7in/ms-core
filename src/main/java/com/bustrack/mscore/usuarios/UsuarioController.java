package com.bustrack.mscore.usuarios;

import com.bustrack.mscore.common.enums.Rol;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import java.util.List;

@Controller
public class UsuarioController {

    private final UsuarioRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Usuario> usuarios() { return repo.findAllByActivoTrue(); }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario usuario(@Argument String id) {
        return repo.findByIdAndActivoTrue(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario crearUsuario(@Argument CrearUsuarioInput input) {
        var u = Usuario.builder()
                .nombre(input.nombre()).email(input.email())
                .passwordHash(passwordEncoder.encode(input.password()))
                .rol(input.rol()).build();
        return repo.save(u);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario actualizarUsuario(@Argument String id, @Argument ActualizarUsuarioInput input) {
        var u = repo.findByIdAndActivoTrue(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (input.nombre() != null) u.setNombre(input.nombre());
        if (input.email() != null) u.setEmail(input.email());
        if (input.rol() != null) u.setRol(input.rol());
        return repo.save(u);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Usuario desactivarUsuario(@Argument String id) {
        var u = repo.findByIdAndActivoTrue(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        u.setActivo(false);
        return repo.save(u);
    }

    public record CrearUsuarioInput(String nombre, String email, String password, Rol rol) {}
    public record ActualizarUsuarioInput(String nombre, String email, String password, Rol rol) {}
}
