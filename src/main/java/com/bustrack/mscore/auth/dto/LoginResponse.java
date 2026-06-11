package com.bustrack.mscore.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String refreshToken;
    private UsuarioData usuario;

    @Data
    @Builder
    public static class UsuarioData {
        private String id;
        private String nombre;
        private String email;
        private String rol;
        private String terminalId;
    }
}
