package com.bustrack.mscore.choferes;

import com.bustrack.mscore.storage.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/choferes")
public class ChoferRestController {

    private final ChoferService service;
    private final StorageService storage;

    public ChoferRestController(ChoferService service, StorageService storage) {
        this.service = service;
        this.storage = storage;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','CHOFER')")
    public ResponseEntity<Map<String, Object>> getChofer(@PathVariable String id) {
        var c = service.findById(id);
        return ResponseEntity.ok(Map.of(
                "id", c.getId(),
                "ci", c.getCi(),
                "nombre", c.getNombre(),
                "telefono", c.getTelefono() != null ? c.getTelefono() : "",
                "licenciaNumero", c.getLicenciaNumero(),
                "licenciaCategoria", c.getLicenciaCategoria(),
                "licenciaVence", c.getLicenciaVence(),
                "estado", c.getEstado().name(),
                "fotoPerfilS3Key", c.getFotoPerfilS3Key() != null ? c.getFotoPerfilS3Key() : "",
                "fotoFacialS3Key", c.getFotoFacialS3Key() != null ? c.getFotoFacialS3Key() : "",
                "fotoPerfilUrl", c.getFotoPerfilS3Key() != null ? storage.getDownloadUrl(c.getFotoPerfilS3Key()) : "",
                "fotoFacialUrl", c.getFotoFacialS3Key() != null ? storage.getDownloadUrl(c.getFotoFacialS3Key()) : ""
        ));
    }
}
