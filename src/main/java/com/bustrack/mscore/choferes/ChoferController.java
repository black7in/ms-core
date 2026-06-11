package com.bustrack.mscore.choferes;

import com.bustrack.mscore.common.enums.EstadoChofer;
import com.bustrack.mscore.common.enums.TipoArchivo;
import com.bustrack.mscore.storage.StorageService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import java.util.List;

@Controller
public class ChoferController {

    private final ChoferService service;
    private final StorageService storage;

    public ChoferController(ChoferService service, StorageService storage) {
        this.service = service;
        this.storage = storage;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<Chofer> choferes(@Argument EstadoChofer estado) { return service.findAll(estado); }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','CHOFER')")
    public Chofer chofer(@Argument String id) { return service.findById(id); }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Chofer crearChofer(@Argument CrearChoferInput input) {
        var c = Chofer.builder()
                .ci(input.ci()).nombre(input.nombre()).telefono(input.telefono())
                .licenciaNumero(input.licenciaNumero()).licenciaCategoria(input.licenciaCategoria())
                .licenciaVence(input.licenciaVence()).build();
        return service.create(c, input.crearUsuario() == Boolean.TRUE ? input.emailUsuario() : null,
                input.crearUsuario() == Boolean.TRUE ? input.passwordUsuario() : null);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Chofer actualizarChofer(@Argument String id, @Argument ActualizarChoferInput input) {
        return service.update(id, Chofer.builder()
                .nombre(input.nombre()).telefono(input.telefono())
                .licenciaNumero(input.licenciaNumero()).licenciaCategoria(input.licenciaCategoria())
                .licenciaVence(input.licenciaVence()).build());
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Chofer cambiarEstadoChofer(@Argument String id, @Argument EstadoChofer estado) {
        return service.cambiarEstado(id, estado);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Chofer guardarFotoChofer(@Argument String id, @Argument String s3Key, @Argument TipoArchivo tipo) {
        return service.guardarFoto(id, s3Key, tipo);
    }

    @SchemaMapping(typeName = "Chofer", field = "fotoPerfilUrl")
    public String fotoPerfilUrl(Chofer chofer) {
        return chofer.getFotoPerfilS3Key() != null ? storage.getDownloadUrl(chofer.getFotoPerfilS3Key()) : null;
    }

    @SchemaMapping(typeName = "Chofer", field = "fotoFacialUrl")
    public String fotoFacialUrl(Chofer chofer) {
        return chofer.getFotoFacialS3Key() != null ? storage.getDownloadUrl(chofer.getFotoFacialS3Key()) : null;
    }

    public record CrearChoferInput(String ci, String nombre, String telefono, String licenciaNumero,
                                   String licenciaCategoria, String licenciaVence,
                                   Boolean crearUsuario, String emailUsuario, String passwordUsuario) {}

    public record ActualizarChoferInput(String ci, String nombre, String telefono, String licenciaNumero,
                                        String licenciaCategoria, String licenciaVence) {}
}
