package com.bustrack.mscore.storage;

import com.bustrack.mscore.common.enums.TipoArchivo;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/storage")
public class StorageRestController {

    private final StorageService storage;

    public StorageRestController(StorageService storage) { this.storage = storage; }

    @GetMapping("/upload-url")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> getUploadUrl(@RequestParam TipoArchivo tipo, @RequestParam String entidadId,
                                             @RequestParam(required = false) String extension) {
        var ext = extension != null && !extension.isEmpty() ? "." + extension.replaceAll("^\\.", "") : "";
        var s3Key = tipo.name().toLowerCase() + "/" + entidadId + "/" + System.currentTimeMillis() + ext;
        return Map.of("uploadUrl", storage.generateUploadUrl(s3Key), "s3Key", s3Key);
    }
}
