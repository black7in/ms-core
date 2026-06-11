package com.bustrack.mscore.facturas;

import com.bustrack.mscore.storage.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/facturas")
public class FacturaRestController {

    private final FacturaRepository facturaRepo;
    private final StorageService storage;

    public FacturaRestController(FacturaRepository facturaRepo, StorageService storage) {
        this.facturaRepo = facturaRepo;
        this.storage = storage;
    }

    @GetMapping("/{boletoId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','VENDEDOR')")
    public ResponseEntity<?> descargarPdf(@PathVariable String boletoId) {
        var f = facturaRepo.findByBoletoId(boletoId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));
        if (f.getPdfS3Key() == null) return ResponseEntity.notFound().build();
        var url = storage.getDownloadUrl(f.getPdfS3Key());
        return ResponseEntity.status(302).header("Location", url).build();
    }
}
