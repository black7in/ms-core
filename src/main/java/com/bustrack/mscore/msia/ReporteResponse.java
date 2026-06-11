package com.bustrack.mscore.msia;

import java.util.List;

public record ReporteResponse(
        String pregunta,
        String explicacion,
        List<String> columnas,
        List<List<String>> filas,
        int totalFilas,
        String error
) {}
