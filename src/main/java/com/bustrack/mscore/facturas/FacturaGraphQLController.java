package com.bustrack.mscore.facturas;

import com.bustrack.mscore.storage.StorageService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import java.util.List;

@Controller
public class FacturaGraphQLController {

    private final FacturaRepository repo;
    private final StorageService storage;

    public FacturaGraphQLController(FacturaRepository repo, StorageService storage) {
        this.repo = repo;
        this.storage = storage;
    }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<Factura> facturas(@Argument Integer page, @Argument Integer limit) {
        int p = page != null ? page : 1, l = limit != null ? limit : 20;
        return repo.findAll().stream().skip((p - 1) * l).limit(l).toList();
    }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public Factura factura(@Argument String id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Factura no encontrada"));
    }

    @QueryMapping @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public Factura facturaPorBoleto(@Argument String boletoId) {
        return repo.findByBoletoId(boletoId).orElse(null);
    }

    @SchemaMapping(typeName = "Factura", field = "pdfUrl")
    public String pdfUrl(Factura factura) {
        return factura.getPdfS3Key() != null ? storage.getDownloadUrl(factura.getPdfS3Key()) : null;
    }
}
