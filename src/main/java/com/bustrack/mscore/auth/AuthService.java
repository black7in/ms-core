package com.bustrack.mscore.auth;

import com.bustrack.mscore.auth.dto.LoginRequest;
import com.bustrack.mscore.auth.dto.LoginResponse;
import com.bustrack.mscore.auth.dto.RefreshRequest;
import com.bustrack.mscore.security.JwtService;
import com.bustrack.mscore.usuarios.Usuario;
import com.bustrack.mscore.usuarios.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        var usuario = usuarioRepository.findByEmailAndActivoTrue(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        if (request.getPushToken() != null) {
            usuario.setPushToken(request.getPushToken());
            usuarioRepository.save(usuario);
        }

        return buildResponse(usuario);
    }

    public LoginResponse refresh(RefreshRequest request) {
        var claims = jwtService.validateRefreshToken(request.getRefreshToken());
        var usuario = usuarioRepository.findByIdAndActivoTrue(claims.getSubject())
                .orElseThrow(() -> new RuntimeException("Token inválido"));
        return buildResponse(usuario);
    }

    private LoginResponse buildResponse(Usuario usuario) {
        String terminalId = usuario.getTerminal() != null ? usuario.getTerminal().getId() : null;
        String token = jwtService.generateAccessToken(usuario.getId(), usuario.getEmail(),
                usuario.getRol().name(), terminalId);
        String refreshToken = jwtService.generateRefreshToken(usuario.getId(), usuario.getEmail(),
                usuario.getRol().name(), terminalId);

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .usuario(LoginResponse.UsuarioData.builder()
                        .id(usuario.getId())
                        .nombre(usuario.getNombre())
                        .email(usuario.getEmail())
                        .rol(usuario.getRol().name())
                        .terminalId(terminalId)
                        .build())
                .build();
    }
}
