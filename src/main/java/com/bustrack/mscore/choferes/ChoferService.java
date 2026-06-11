package com.bustrack.mscore.choferes;

import com.bustrack.mscore.common.enums.EstadoChofer;
import com.bustrack.mscore.common.enums.Rol;
import com.bustrack.mscore.common.enums.TipoArchivo;
import com.bustrack.mscore.usuarios.Usuario;
import com.bustrack.mscore.usuarios.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class ChoferService {

    private final ChoferRepository repo;
    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    public ChoferService(ChoferRepository repo, UsuarioRepository usuarioRepo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Chofer> findAll(EstadoChofer estado) {
        return estado != null ? repo.findByEstado(estado) : repo.findAll();
    }

    public Chofer findById(String id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Chofer no encontrado"));
    }

    public Chofer create(Chofer data, String emailUsuario, String passwordUsuario) {
        if (emailUsuario != null && passwordUsuario != null) {
            var u = Usuario.builder()
                    .nombre(data.getNombre())
                    .email(emailUsuario)
                    .passwordHash(passwordEncoder.encode(passwordUsuario))
                    .rol(Rol.CHOFER)
                    .build();
            data.setUsuario(usuarioRepo.save(u));
        }
        return repo.save(data);
    }

    public Chofer update(String id, Chofer data) {
        var c = findById(id);
        if (data.getNombre() != null) c.setNombre(data.getNombre());
        if (data.getTelefono() != null) c.setTelefono(data.getTelefono());
        if (data.getLicenciaNumero() != null) c.setLicenciaNumero(data.getLicenciaNumero());
        if (data.getLicenciaCategoria() != null) c.setLicenciaCategoria(data.getLicenciaCategoria());
        if (data.getLicenciaVence() != null) c.setLicenciaVence(data.getLicenciaVence());
        return repo.save(c);
    }

    public Chofer cambiarEstado(String id, EstadoChofer estado) {
        var c = findById(id);
        c.setEstado(estado);
        return repo.save(c);
    }

    public Chofer guardarFoto(String id, String s3Key, TipoArchivo tipo) {
        var c = findById(id);
        if (tipo == TipoArchivo.FOTO_FACIAL) c.setFotoFacialS3Key(s3Key);
        else c.setFotoPerfilS3Key(s3Key);
        return repo.save(c);
    }
}
