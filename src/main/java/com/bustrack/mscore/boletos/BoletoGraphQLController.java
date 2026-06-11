package com.bustrack.mscore.boletos;

import com.bustrack.mscore.common.enums.EstadoBoleto;
import com.bustrack.mscore.storage.StorageService;
import com.bustrack.mscore.usuarios.Usuario;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import java.util.*;

@Controller
public class BoletoGraphQLController {

    private final BoletoService service;
    private final BoletoRepository repo;
    private final StorageService storage;

    public BoletoGraphQLController(BoletoService service, BoletoRepository repo, StorageService storage) {
        this.service = service;
        this.repo = repo;
        this.storage = storage;
    }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','SUPERVISOR')")
    public Map<String, Object> boletos(@Argument String viajeId, @Argument String clienteId,
            @Argument EstadoBoleto estado, @Argument Integer page, @Argument Integer limit) {
        var all = repo.findAll();
        if (viajeId != null) all = all.stream().filter(b -> b.getViaje().getId().equals(viajeId)).toList();
        if (clienteId != null) all = all.stream().filter(b -> b.getCliente().getId().equals(clienteId)).toList();
        if (estado != null) all = all.stream().filter(b -> b.getEstado() == estado).toList();
        int p = page != null ? page : 1, l = limit != null ? limit : 20;
        var items = all.stream().skip((p - 1) * l).limit(l).toList();
        return Map.of("items", items, "total", all.size());
    }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','SUPERVISOR')")
    public Boleto boleto(@Argument String id) { return service.findById(id); }

    @MutationMapping @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','SUPERVISOR')")
    public Boleto venderBoleto(@Argument VenderBoletoInput input, @AuthenticationPrincipal Usuario user) {
        return service.venderBoleto(input.viajeId(), input.asientoId(), input.clienteId(),
                user.getId(), input.precioVenta(), input.nit(), input.razonSocial());
    }

    @MutationMapping @PreAuthorize("hasAnyRole('ADMIN','VENDEDOR','SUPERVISOR')")
    public Boleto cancelarBoleto(@Argument String id) { return service.cancelarBoleto(id); }

    @SchemaMapping(typeName = "Boleto", field = "pdfUrl")
    public String pdfUrl(Boleto boleto) {
        return boleto.getPdfS3Key() != null ? storage.getDownloadUrl(boleto.getPdfS3Key()) : null;
    }

    public record VenderBoletoInput(String viajeId, String asientoId, String clienteId,
                                    Double precioVenta, String nit, String razonSocial) {}
}
