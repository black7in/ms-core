package com.bustrack.mscore.usuarios;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/usuarios")
public class UsuarioRestController {

    private final UsuarioRepository repo;

    public UsuarioRestController(UsuarioRepository repo) { this.repo = repo; }

    @PatchMapping("/push-token")
    public ResponseEntity<?> updatePushToken(@RequestBody Map<String, String> body,
                                              @AuthenticationPrincipal Usuario user) {
        user.setPushToken(body.get("push_token"));
        repo.save(user);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
