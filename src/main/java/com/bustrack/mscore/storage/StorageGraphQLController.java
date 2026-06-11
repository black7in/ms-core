package com.bustrack.mscore.storage;

import com.bustrack.mscore.common.enums.TipoArchivo;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class StorageGraphQLController {

    private final StorageService storage;

    public StorageGraphQLController(StorageService storage) { this.storage = storage; }

    @MutationMapping @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> generarUrlSubida(@Argument TipoArchivo tipo, @Argument String entidadId,
                                                 @Argument String extension) {
        var ext = (extension != null && !extension.isEmpty())
                ? "." + extension.replaceAll("^\\.", "") : "";
        var s3Key = tipo.name().toLowerCase() + "/" + entidadId + "/" + System.currentTimeMillis() + ext;
        return Map.of("uploadUrl", storage.generateUploadUrl(s3Key), "s3Key", s3Key);
    }
}
