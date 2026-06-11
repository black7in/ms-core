package com.bustrack.mscore.blockchain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);
    private final String msBlockchainUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BlockchainService(@Value("${app.ms-blockchain.url}") String msBlockchainUrl) {
        this.msBlockchainUrl = msBlockchainUrl;
    }

    public String registrarFactura(String hashSha256, String numeroFactura, BigDecimal monto) throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "hashSha256", hashSha256,
                "numeroFactura", numeroFactura,
                "monto", monto
        ));
        var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        var authHeader = attrs != null ? attrs.getRequest().getHeader("Authorization") : null;

        var reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(msBlockchainUrl + "/blockchain/facturas"))
                .header("Content-Type", "application/json");
        if (authHeader != null) reqBuilder.header("Authorization", authHeader);
        var req = reqBuilder.POST(HttpRequest.BodyPublishers.ofString(body)).build();
        var resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400)
            throw new RuntimeException("Blockchain respondió " + resp.statusCode() + ": " + resp.body());
        log.info("Blockchain => {} {}", resp.statusCode(), resp.body());
        return objectMapper.readTree(resp.body()).path("txHash").asText();
    }
}
