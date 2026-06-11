package com.bustrack.mscore.auth;

import com.bustrack.mscore.auth.dto.LoginRequest;
import com.bustrack.mscore.auth.dto.LoginResponse;
import com.bustrack.mscore.usuarios.Usuario;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
public class AuthGraphQLController {

    private final AuthService authService;

    public AuthGraphQLController(AuthService authService) { this.authService = authService; }

    @MutationMapping
    public LoginResponse login(@Argument String email, @Argument String password) {
        var req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return authService.login(req);
    }

    @QueryMapping
    public Usuario me(@AuthenticationPrincipal Usuario user) {
        return user;
    }
}
