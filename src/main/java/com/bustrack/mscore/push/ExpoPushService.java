package com.bustrack.mscore.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class ExpoPushService {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushService.class);
    private static final String EXPO_URL = "https://exp.host/--/api/v2/push/send";

    private final String accessToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExpoPushService(@Value("${app.expo.access-token}") String accessToken) {
        this.accessToken = accessToken;
    }

    public void notifyViajeAsignado(String pushToken, String viajeId,
                                     String origen, String destino,
                                     String fecha, String horaSalida, String carril) {
        String carrilStr = carril != null ? ", Carril " + carril : "";
        String body = String.format("Tenés un viaje asignado: %s → %s el %s a las %s%s",
                origen, destino, fecha, horaSalida, carrilStr);

        Map<String, Object> payload = Map.of(
                "to", pushToken,
                "title", "🚌 Nuevo viaje asignado",
                "body", body,
                "data", Map.of("tipo", "VIAJE_ASIGNADO", "viajeId", viajeId)
        );

        Thread.ofVirtual().start(() -> {
            try {
                var json = objectMapper.writeValueAsString(payload);
                var req = HttpRequest.newBuilder()
                        .uri(URI.create(EXPO_URL))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + accessToken)
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                var resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
                log.info("Expo push => {} {}", resp.statusCode(), resp.body());
            } catch (Exception e) {
                log.error("Expo push failed: {}", e.getMessage());
            }
        });
    }
}
