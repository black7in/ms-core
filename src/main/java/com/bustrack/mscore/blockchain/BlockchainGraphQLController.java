package com.bustrack.mscore.blockchain;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
public class BlockchainGraphQLController {

    private final BlockchainService blockchain;

    public BlockchainGraphQLController(BlockchainService blockchain) {
        this.blockchain = blockchain;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','VENDEDOR')")
    public Map<String, Object> verificarFactura(@Argument String hash) {
        try {
            return blockchain.verificarFactura(hash);
        } catch (Exception e) {
            return Map.of("existe", false, "autentica", false,
                    "error", e.getMessage());
        }
    }
}
