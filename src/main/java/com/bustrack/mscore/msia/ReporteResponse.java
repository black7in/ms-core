package com.bustrack.mscore.msia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReporteResponse(
        String pregunta,
        String explicacion,
        List<String> columnas,
        List<List<String>> filas,
        int totalFilas,
        String error
) {}
