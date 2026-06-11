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
        var result = new java.util.LinkedHashMap<String, Object>();
        result.put("id", c.getId());
        result.put("ci", c.getCi());
        result.put("nombre", c.getNombre());
        result.put("telefono", c.getTelefono() != null ? c.getTelefono() : "");
        result.put("licenciaNumero", c.getLicenciaNumero());
        result.put("licenciaCategoria", c.getLicenciaCategoria());
        result.put("licenciaVence", c.getLicenciaVence());
        result.put("estado", c.getEstado().name());
        result.put("fotoPerfilS3Key", c.getFotoPerfilS3Key() != null ? c.getFotoPerfilS3Key() : "");
        result.put("fotoFacialS3Key", c.getFotoFacialS3Key() != null ? c.getFotoFacialS3Key() : "");
        result.put("fotoPerfilUrl", c.getFotoPerfilS3Key() != null ? storage.getDownloadUrl(c.getFotoPerfilS3Key()) : "");
        result.put("fotoFacialUrl", c.getFotoFacialS3Key() != null ? storage.getDownloadUrl(c.getFotoFacialS3Key()) : "");
        return ResponseEntity.ok(result);
    }
}
