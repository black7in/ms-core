package com.bustrack.mscore.usuarios;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {
    Optional<Usuario> findByEmailAndActivoTrue(String email);
    Optional<Usuario> findByIdAndActivoTrue(String id);
    List<Usuario> findAllByActivoTrue();
}
