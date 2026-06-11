package com.bustrack.mscore.webhooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class WebhooksService {

    private static final Logger log = LoggerFactory.getLogger(WebhooksService.class);
    private final String ventaUrl;
    private final String cancelacionUrl;

    public WebhooksService(
            @Value("${app.n8n.webhook-venta-url}") String ventaUrl,
            @Value("${app.n8n.webhook-cancelacion-url}") String cancelacionUrl) {
        this.ventaUrl = ventaUrl;
        this.cancelacionUrl = cancelacionUrl;
    }

    public void notifyVenta(Object data) {
        send(ventaUrl, data);
    }

    public void notifyCancelacion(Object data) {
        send(cancelacionUrl, data);
    }

    private void send(String url, Object data) {
        try {
            if (url == null || url.isEmpty()) return;
            var json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
            var req = HttpRequest.newBuilder().uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json)).build();
            var resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
            log.info("Webhook => {} — {} {}", url, resp.statusCode(), resp.body());
        } catch (Exception e) {
            log.error("Webhook failed: {}", e.getMessage());
        }
    }
}
