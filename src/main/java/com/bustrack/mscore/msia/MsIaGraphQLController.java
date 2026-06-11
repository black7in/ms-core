package com.bustrack.mscore.msia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Controller
public class MsIaGraphQLController {

    private static final Logger log = LoggerFactory.getLogger(MsIaGraphQLController.class);
    private final String msIaUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MsIaGraphQLController(@Value("${app.ms-ia.url}") String msIaUrl) {
        this.msIaUrl = msIaUrl;
    }

    @QueryMapping
    public ReporteResponse reporteInteligente(@Argument String pregunta) {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            var authHeader = attrs != null ? attrs.getRequest().getHeader("Authorization") : null;

            var body = objectMapper.writeValueAsString(Map.of("pregunta", pregunta));
            var reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(msIaUrl + "/agente/reporte"))
                    .header("Content-Type", "application/json");
            if (authHeader != null) reqBuilder.header("Authorization", authHeader);
            var resp = HttpClient.newHttpClient().send(reqBuilder.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.ofString());

            var result = objectMapper.readValue(resp.body(), ReporteResponse.class);
            if (result.pregunta() == null)
                return new ReporteResponse(pregunta, "", List.of(), List.of(), 0,
                        objectMapper.readTree(resp.body()).path("error").asText("Error desconocido de MS-IA"));
            return result;
        } catch (Exception e) {
            log.error("MS-IA proxy error: {}", e.getMessage());
            return new ReporteResponse(pregunta, "", List.of(), List.of(), 0, e.getMessage());
        }
    }
}
